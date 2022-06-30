## shipkafka - spring-kafka sample application

This sample application uses kafka for exchanging messages between two components.

Further information on the sample application like architecture and implementation details can be found in the wiki.

### Execution
#### Prerequisite
In order to run the sample application, it is required to have ZooKeeper & Kafka running on your system. This can be done by using the provided Dockerfile with docker-compose:
```
docker-compose up
```

A frontend is not provided. For using the endpoints, we suggest you to use **Postman** for simple API usage (Further reading: https://learning.postman.com/docs/getting-started/introduction/). This app's functionalities are thoroughly elaborated below (after **Starting the Application**).

#### Starting the Application
We use Gradle as a build tool and the Spring Boot Gradle plugin to run our application. If you are using IntelliJ, you can run the application from the context menu:
- Open the Gradle tool window which can be found on the right side of IntelliJ window
- Click the application task (Tasks > application)
- Double click bootRun to start the application

if you want to start the application from the terminal, run the following command in a terminal window (open terminal in root directory):
```
./gradlew bootRun
```

After starting the application, you can then start to use or observe some of the app's functionalities, which are going to be elaborated thorougly below.

#### Getting a list of Customers
Using Postman, you can copy and paste the following HTTP request URL and select the **GET** method (the action type) on the left of the URL field to get a list of Customers:
```
http://localhost:8080/customers
```

#### Getting a list of available Ships
Use the following HTTP request URL and select the **GET** method to get a list of available Ships:
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
Afterwards, use the following URL and select the **POST** method to successfully create a Customer:
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

Afterwards, use the following URL and select the POST method to successfully create a Ship:
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
    "containerCount": 2
}
```
The specified data above means that the Customer with the ID 5 wants to book the Ship with the ID 4 and the required container count of this ship is 2. If the damaged status of the booked Ship at the time is **FALSE** (meaning the ship is in good condition), you can see that the booking status of the customer is changed from _**REQUESTED**_ to _**CONFIRMED**_, if the containerCount of the ship is higher than the requested containers. You can check the booking status of the ship by seeing the list of the Customers (see **Getting a list of Customers**).

#### Getting a list of Bookings
You can see the list of all bookings from all customers by selecting the **GET** method and using the following URL:
```
http://localhost:8080/bookings
```

#### Getting Booking(s) of a specific Customer
You can retrieve a list of bookings of a specific customer by firstly defining the ID of the Customer you want to see (in the example below, Customer with the ID 5), selecting the **GET** method, and finally using the following URL:
```
http://localhost:8080/customers/5/bookings
```

#### Change the status/condition of the ship
The status/condition of a Ship can be changed/updated whenever it's damaged or not. To achieve this, use the following URL and choose **PUT** method:
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

The status of a ship can be set from "notDamaged" to "damaged". If a ship is at that time damaged, then the ship and its containers cannot be reserved. However, if the ship is repaired, its status can be changed back to "notDamaged" at any time and hence the ship and its containers can be booked again. But since we could not know exactly when the status of the ship will be changed to "notDamaged", retry is implemented here.

For example, you want to reserve a few containers from a "damaged" ship and thus you send a request if it's still available. As soon as you sent the request, the ship is actually damaged at the moment. However, the status of the ship is set to "notDamaged" in the next second because it had been repaired. Therefore, you need to check if the status is changed within a few seconds.

Do the following steps to observe the implemented retry:
- Get a list of available Ships (see **Getting the list of available Ships**)
- Select a ship whose status you wish to modify
- Change the status of the chosen Ship to **TRUE** (damaged = **TRUE**, ship is damaged)
- Get a list of available Ships again to make sure the status of the chosen Ship has changed
- Get a list of Customers (see **Getting a list of Customers**)
- Select one of the Customers that wants to book a ship
- With the chosen Customer, book a currently damaged ship and its containers (see **Create a new booking**)
- Since the booked ship is currently damaged, the system will retry the booking a few times (precisely 3 times). In the console, you can see the LOG info stating that the booking confirmation is being retried
- **If the Ship is still damaged**, the system will throw the exception stating that the booked ship is still damaged. The booking is then cancelled
- **If the Ship is being repaired and its status changes** between the retry period, the ship is successfully booked and hence the booking can be confirmed
