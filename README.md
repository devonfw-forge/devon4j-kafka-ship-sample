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
http://localhost:8080/customers/
```

After clicking "Send" button, you will see the following response:
```
  
```

#### Getting a list of available Ships

#### Create a new Customer

#### Delete a Customer

#### Create a new Ship

#### Create a new booking (Customer books a ship with required containers)

#### Getting a list of Bookings

#### Getting Booking(s) of a specific Customer

#### Change the status of the ship

#### Implementing retry
