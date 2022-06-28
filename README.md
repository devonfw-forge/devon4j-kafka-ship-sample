## shipkafka - spring-kafka sample application

This sample application uses kafka for exchanging messages between two components.

### Architecture

The example application consists of two services that communicate through Kafka topics.

The `booking-service` sends newly created/requested bookings to the Kafka `bookings` topic. The `ship-service` listens for incoming events (new bookings). After the `ship-service` receives a booking, it verifies if it is possible to execute the booking. The condition would be that if there are enough containers on the requested ship. If it is, then the booking gets confirmed. Otherwise, it gets rejected. The response, which is visualized as `bookingStatus`, is then sent back to the `bookings` topic and the `booking-service` listen to this topic. Only when the response is consumed, the `bookingStatus` gets updated.

![](https://i.imgur.com/Bb8c5l1.png)


There are 3 possible outcomes, which are visualized as `bookingStatus`: `REQUESTED`, `CONFIRMED`, `CANCELED`. The `bookingStatus` of newly requested bookings, which are sent from `booking-service` to `bookings`, is `REQUESTED`. The booking of a ship with enough available containers will result in `CONFIRMED`.

However, there might be some cases when a booking will not be confirmed. If a ship with not enough container is booked, the booking is not possible and hence gives `CANCELED` in the `bookingStatus`. Furthermore, when an already booked ship is damaged, the `ship-service` will then send this new response to Kafka `ship-damaged` topic. Afterwards, the `bookingStatus` is updated to `CANCELED`.

### Implementation
#### Prerequisites
The following are the necessary tech stacks that are needed to be installed before running a spring kafka application:
- Apache Kafka
- Zookeeper

  **OR**
- Docker images (container that includes both Kafka Message Broker and Zookeeper)




#### Topics Creation

The `booking-service` automatically creates the necessary topics on application startup by defining beans as followed:

```
@Bean
public NewTopic bookings() {
   return TopicBuilder.name("bookings")
         .partitions(2)
         .compact()
         .build();
}
```

This creates the `bookings` topic with two partitions. The method `compact()` enables log compaction on the topic. A topic with log compaction removes any old records when there is a more recent update with the same primary key. This is optional and needs to be decided on a case-by-case basis.

The creation of all necessary topics can be found in class `BookingComponentMessagingGateway`, in which the *Messaging Gateway* pattern is implemented. The class basically isolates messaging-specific functionalities (necessary codes to send or receive a message) from the rest of the application code. Hence, only the Messaging Gate code is aware of the messaging system.

Within the class, another topic called `shipDamagedTopic` is defined and used to fetch messages whenever the `ship-service` informs the `booking-service` that the booked ship is currently damaged.

```
@Bean
    public NewTopic shipDamagedTopic(){
        return TopicBuilder.name("shipDamagedTopic").build();
    }
```

The `booking-service` receives events from the `ship-service`. Every booking event contains an `id`, which was set by the `booking-service`. ***(Should this be explained in consuming messages part?)***


#### Add new booking (Sending messages):

After topics are created with kafka, the subsequent step is to send messages/data to it. `spring-kafka` simplifies sending messages to topics with the class `KafkaTemplate<K,V>`, which provides methods for sending messages to Kafka topics in a feasible way.

```
private final KafkaTemplate<Long, Object> longTemplate;
public <T> void sendMessage(String topic, Long key, T message) {
        LOG.info("Sending message : {}", message.toString());
        longTemplate.send(topic, key, message);
    }
```
The `send` API will send the data to the provided topic with the provided key and no partition. It returns a `ListenableFuture` object after a send. The `sendMessage` function is **for example implemented for adding a new booking**, which can be seen in class `BookingComponentBusinessLogic`. The new booking with its own associated id (which is retrieved using `getId()`) and the number of requested containers given by the customer is sent to `bookings` topic with message `booking`.

```
@Transactional(rollbackFor = {CustomerNotFoundException.class})
    public Booking addBooking(Long customerId, BookingCreateDTO bookingCreateDTO) throws CustomerNotFoundException{
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);

        if (optionalCustomer.isPresent()) {
            Customer customer = optionalCustomer.get();

            //Booking booking = bookingRepository.save(Booking.of(bookingCreateDTO));
            Booking booking = bookingRepository.save(new Booking(bookingCreateDTO.getShipId(), bookingCreateDTO.getContainerCount()));

            customer.addBooking(booking);
            customerRepository.save(customer);

            bookingComponentMessagingGateway.sendMessage("bookings", booking.getId(), booking);


            return booking;
        } else {
            throw new CustomerNotFoundException(customerId);
        }
    }
```
It is important to note that the status of the newly created booking is `REQUESTED`. The status of the bookings is managed by `BookingStatus`, which is an enum. The other constants within this enum are `CONFIRMED` and `CANCELED`.

#### onBookingEvent, listenBooking (Receiving messages):
The booking is then consumed via `bookings` topic by `ship-service` and will be checked, if that booking already existed in the system or the ship that would be booked also exists. The process can be found in class `ShipRestController`.
```
@KafkaListener(id = "bookings", topics = "bookings", groupId = "ship")
    public void onBookingEvent(Booking booking) throws ShipNotFoundException, BookingAlreadyConfirmedException {
        LOG.info("Received: {}", booking);
        shipComponentLogic.confirmBooking(booking);
    }
```
The booking can be confirmed through `confirmBooking` function, which can be found in `ShipComponentLogic` class. Firstly, it will check if the status of this newly created booking is already confirmed or not. If it is, then `BookingAlreadyConfirmedException` will be thrown. If it's newly created, the ship that'll be booked is checked whether there's enough available container or not. If there is not enough available containers, the booking is cancelled (`BookingStatus.CANCELED`). Otherwise, it will be confirmed (`BookingStatus.CONFIRMED`) and this confirmation is afterwards sent to the `ship-bookings` topic.
```
@Transactional(rollbackFor = {BookingAlreadyConfirmedException.class})
    public void confirmBooking(Booking booking) throws BookingAlreadyConfirmedException, ShipNotFoundException {
        Ship ship = shipRepository.findById(booking.getShipId()).orElseThrow(() -> new ShipNotFoundException(booking.getShipId()));
        LOG.info("Found: {}", ship);

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            throw new BookingAlreadyConfirmedException(booking.getId());
        } else if (booking.getBookingStatus().equals(BookingStatus.REQUESTED)) {
            if(booking.getContainerCount() < ship.getAvailableContainers()){
                ship.setAvailableContainers(ship.getAvailableContainers() - booking.getContainerCount());
                booking.updateBookingStatus(BookingStatus.CONFIRMED);
                shipRepository.save(ship);
            } else {
                booking.updateBookingStatus(BookingStatus.CANCELED);
            }
        }

        template.send("ship-bookings", booking.getId(), booking);
        LOG.info("Sent: {}", booking);
    }
```

The confirmation is finally consumed by `booking-service`, in which the booking is processed, hence updating the booking status to `CONFIRMED`.
```
@KafkaListener(id ="ship-bookings", topics = "ship-bookings", groupId = "booking")
    public void listenBooking(Booking booking) throws BookingNotFoundException {
        LOG.info("Received message: {}", booking.toString());
        bookingComponentBusinessLogic.processBooking(booking);
    }
```

#### Logging
As logging tool, SLF4J (Simple Logging Facade for Java) is used. It is an abstraction layer for different Java logging frameworks, such as Log4j2 or logback.

The general pattern (common solution) for accessing loggers by defining logger as static final instance is no longer recommended, as well as defining logger as instance variable (as slf4j.org used to recommend). For example, in `ShipComponentLogic`, the log is declared as static final instance as following:

```
private static final Logger LOG = LoggerFactory.getLogger(ShipComponentLogic.class);
```

In `BookingComponentMessagingGateway`, the log is defined as instance variable as following:
```
private final Logger LOG = LoggerFactory.getLogger(getClass());
```
Further comparison and explanation between these two different declarations can be found in https://www.slf4j.org/faq.html#declared_static.

SLF4J standardized the logging levels, which are different for the particular implementations. The usage of SLF4J is straightforward yet adaptable, allowing for better readability and performance.

The logging levels used in SLF4J are: *TRACE*, *INFO*, *DEBUG*, *ERROR*, and *WARN*. *FATAL* logging level (introduced in Log4j) is removed in SLF4J due to redundancy (https://www.slf4j.org/faq.html#fatal) as well as due to the fact that we should not determine when to terminate an application in a logging framework (https://www.baeldung.com/slf4j-with-log4j2-logback).


#### Tracing


In this sample application, tracing implementations are supported by Spring Cloud Sleuth. Sleuth seamlessly interfaces with logging frameworks such as SLF4J and logback to add unique IDs that aid in the tracking and diagnosis of issues leveraging logs. Before it is implemented, its dependency must be defined in `build.gradle` file as following:
```
dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:2021.0.2"
	}
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
}
```
Once added within the application, Spring Cloud Sleuth automatically formats the logs that contain traceId and spanId. Below you can see the logs when the application just started and when a customer just booked a ship.

```
2022-06-13 22:01:16.243  INFO [shipkafka,bd2025db4480982a,bd2025db4480982a] 14272 --- [nio-8080-exec-5] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1655150476243
```
```
2022-06-13 22:01:16.430  INFO [shipkafka,bd2025db4480982a,cfcad1e77644ff13] 14272 --- [ bookings-0-C-1] c.d.s.s.api.ShipRestController           : Received: Booking(createdOn=Mon Jun 13 22:01:16 CEST 2022, id=12, lastUpdatedOn=Mon Jun 13 22:01:16 CEST 2022, containerCount=2, shipId=4, bookingStatus=REQUESTED, version=0)
```
The part of normal log with additional core information from Spring Sleuth follows the format of:
**[application name, traceId, spanId]**

- **Application name** - name that is set in the properties file/settings.gradle file. It can be used to aggregate logs from multiple instances of the same application.
- **traceId** - unique identifier for a specific job, request, or action. It is the same across all microservices.
- **spanId** - unique identifier that tracks a unit of work. It is assigned to each operation and therefore can be vary depends on what request is performed.


Once the application is started, the traceID and spanID **will be the same** by default.

Further reading: https://www.baeldung.com/spring-cloud-sleuth-single-application#:~:text=TraceId%20%E2%80%93%20This%20is%20an%20id,that%20consists%20of%20multiple%20steps.

#### Retry
Failures in a distributed system may happen, i.e. failed message process, network errors, runtime exceptions. Therefore, the retry logic implementation is something essential to have.

It is important to note that Retries in Kafka can be quickly implemented at the consumer side. This is known as Simple Blocking Retries. To accomplish visible error handling without causing real-time disruption, Non-Blocking Retries and Dead Letter Topics are implemented.


Non-Blocking Retries can easily be added to a listener:

```
@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 2))
@KafkaListener(id = "bookings", topics = "bookings", groupId = "ship")
public void listenBookings(Booking booking){
	...
}

@DltHandler
public void listenBookingsDlt(Booking booking){
    LOG.info("Received DLT message: {}", booking);
}
    
```

In this example the `@RetryableTopic` annotation attempts to process a received message 3 times. The first retry is done after a delay of 2 seconds. Each further attempt multiplies the delay by 2 with a max delay of 10 seconds. If the message couldn't be processed, it gets send to the deadletter topic annotated with `@DltHandler`.

```
bookings-retry-5000
```
Each retry creates a new topic like in the example above.

DLT creates a topic for messages that could not get processed. The topic gets named like the example below:
```
bookings-dlt
```

### Execution
#### Prerequisite
In order to run the sample application, it is required to have ZooKeeper & Kafka running on your system.
```
docker-compose up
```

As for testing environment, we suggest you to use **Postman API** for simple API test (Further reading: https://learning.postman.com/docs/getting-started/introduction/). The tests of this app's functionalities are thoroughly elaborated below (after **Starting the Application**).

#### Starting the Application
We use Gradle as a build tool and the Spring Boot Gradle plugin to run our application. If you are using IntelliJ, you can run the application from the context menu:
- Open the Gradle tool window which can be found on the right side of IntelliJ window
- Click the application task (Tasks > application)
- Double click bootRun to start the application

if you want to start the application from the terminal, run the following command in a terminal window (open terminal in root directory):
```
./gradlew bootRun
```

After starting the application, you can then start to test or observe some of the app's functionalities, which are going to be elaborated thorougly below.

#### Getting a list of Customers
Using Postman API, you can copy and paste the following HTTP request URL and select the **GET** method (the action type) on the left of the URL field to get a list of Customers:
```
http://localhost:8080/customers
```

#### Getting a list of available Ships
Copy and paste the following HTTP request URL and select the **GET** method to get a list of available Ships:
```
 http://localhost:8080/ships
```

#### Create a new Customer
A new customer can be created by adding the following details as JSON in the body data (**Further reading: https://learning.postman.com/docs/sending-requests/requests/#sending-body-data**):
```
{
    "firstName": "First Name",
    "lastName": "Last Name"
}
```
Afterwards, copy and paste the following URL and select the **POST** method to successfully create a Customer:
```
http://localhost:8080/customers
```

#### Delete a Customer
A Customer can be deleted from the system by specifying the specific Customer ID in the HTTP URL and selecting the **DELETE** method. As an example, the Customer with the ID 5 want to be removed from the system:
```
http://localhost:8080/customers/5
```

#### Create a new Ship
A new Ship can be created by adding the following details as JSON in the body data:
```
{
    "shipName": "Ship One",
    "availableContainers": 20,
    "damaged": false
}
```
It is important to note that **integer** number should be provided to define how many available containers a ship has (just like the example above). As for the status of the ship, **boolean** value is required (TRUE = damaged, FALSE = good condition).

Afterwards, copy and paste the following URL and select the POST method to successfully create a Ship:
```
 http://localhost:8080/ships
```
#### Create a new booking (Customer books a ship with required containers)
Customers can book ships. In order to create a booking successfully, the ID of the specific customer needs to be defined in the URL. Using the **POST** method, below is the example of the URL:
```
 http://localhost:8080/customers/5/bookings
```
The request example above means that the Customer with the ID 5 wants to book a new ship. Furthermore, the details of the booked ship still need to be defined as JSON in the body data so that the booking can be created successfully:
```
{
    "shipId": 4,
    "containerCount": 2,
}
```
The specified data above means that the Customer with the ID 5 wants to book the Ship with the ID 4 and the required container of this ship is 2. If the status of the booked Ship at the time is **FALSE** (meaning the ship is in good condition), you can see that the booking status of the customer is changed from _**REQUESTED**_ to _**CONFIRMED**_. You can check the booking status of the ship by seeing the list of the Customers (see **Getting a list of Customers**).

#### Getting a list of Bookings
You can see the list of all bookings from all customers by selecting the **GET** method and copying and pasting the following URL:
```
http://localhost:8080/bookings
```

#### Getting Booking(s) of a specific Customer
You can retrieve a list of bookings of a specific customer by firstly defining the ID of the Customer you want to see (in the example below, Customer with the ID 5), selecting the **GET** method, and finally copying and pasting the following URL:
```
http://localhost:8080/customers/5/bookings
```

#### Change the status/condition of the ship
The status/condition of a Ship can be changed/updated whenever it's damaged or not. To achieve this, copy and paste the following URL and choose **PUT** method:
```
http://localhost:8080/ships
```
Afterwards, you still need to specify the ID and the status of the ship as JSON in the body data. In the example below, we want to tell that the Ship with the ID 4 is currently damaged:
```
{
    "id": 4,
    "damaged": true
}
```
Finally, you can check the status of the ship by seeing the list of the Ships (see **Getting the list of available Ships**).

#### Implementing retry
The retry functionality can be observed by implementing the following scenario:

The status of a ship can be set from "ready-to-go" to "damaged". If a ship is at that time damaged, then the ship and its containers cannot be reserved. However, if the ship is repaired, its status can be changed back to "ready-to-go" at any time and hence the ship and its containers can be booked again. But since we could not know exactly when the status of the ship will be changed to "ready-to-go", retry is implemented here.

For example, you want to reserve a few containers from a "damaged" ship and thus you send a request if it's still available. As soon as you sent the request, the ship is actually at the moment damaged. However, the status of the ship is set to "ready-to-go" in the next second because it had been repaired. Therefore, you need to check if, for example, within a few seconds the status is changed.

Follow the following steps to observe the retry being implemented:
- See a list of available Ships (see **Getting the list of available Ships**)
- Select a ship whose status you wish to modify
- Change the status of the chosen Ship to **TRUE** (damaged = **TRUE**, ship is damaged)
- See a list of available Ships again to make sure the status of the chosen Ship has changed
- See a list of Customers (see **Getting a list of Customers**)
- Select one of the Customers that wants to book a ship
- With the chosen Customer, book a currently damaged ship and its containers (see **Create a new booking**)
- Since the booked ship is currently damaged, the system will retry to book it a few times (precisely 3 times). At the console, you can see the LOG info stating that the booking confirmation is being retried
- **If the Ship is still damaged**, the system will throw the exception stating that the booked ship is still damaged. The booking is then cancelled
- **If the Ship is being repaired and its status changes** between the retry period, the ship is successfully booked and hence the booking can be confirmed