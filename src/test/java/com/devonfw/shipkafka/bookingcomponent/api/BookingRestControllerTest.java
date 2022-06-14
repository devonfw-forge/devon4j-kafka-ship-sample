package com.devonfw.shipkafka.bookingcomponent.api;

import com.devonfw.shipkafka.Application;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.BookingRepository;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.bookingcomponent.dtos.BookingCreateDTO;
import com.devonfw.shipkafka.bookingcomponent.dtos.IdDTO;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import com.devonfw.shipkafka.shipcomponent.domain.repositories.ShipRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureJsonTesters
@ActiveProfiles(profiles = "testing")
class BookingRestControllerTest {

    private final Log log = LogFactory.getLog(getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShipRepository shipRepository;

    private Customer customer;

    private Ship ship;

    @BeforeEach
    void setUp() {
        this.customerRepository.deleteAll();
        this.bookingRepository.deleteAll();
        this.shipRepository.deleteAll();

        customer = this.customerRepository.save(new Customer("Max", "Muster"));

        ship = this.shipRepository.save(new Ship("Mein Schiff 42", 10));

        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    // -------------------------------------------------------------------------------------------------------------------
    // für JSONPath siehe http://goessner.net/articles/JsonPath/ und den Tester unter https://jsonpath.curiousconcept.com/
    // -------------------------------------------------------------------------------------------------------------------
    @Test
    void getNoBookingsFoundSuccess() {
        //@formatter:off
        given().
        when().
                get("/bookings").
        then().
                statusCode(HttpStatus.OK.value()).
                body("", hasSize(0));
        //@formatter:on
    }

    @Test
    void getBookingFailBecauseOfNotFound() {
        //@formatter:off
        given().
        when().
                get("/bookings/{id}", Integer.MAX_VALUE).
        then().
                statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }

    @Test
    void getBookingSuccess() {
        //@formatter:off
        Long bookingId = given().
                contentType(ContentType.JSON).
                body(new BookingCreateDTO(ship.getId(), 5)).
        when().
                post("/customers/{id}/bookings", customer.getId()).
        then().
                statusCode(HttpStatus.CREATED.value()).
        extract().
                body().as(IdDTO.class).getId();

        given().
        when().
                get("/bookings").
        then().
                statusCode(HttpStatus.OK.value()).
                body("id", hasItem(bookingId.intValue()));

        given().
        when().
                get("/bookings/{id}", bookingId).
        then().
                statusCode(HttpStatus.OK.value());
        //@formatter:on
    }
}