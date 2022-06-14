package com.devonfw.shipkafka.bookingcomponent.api;

import com.devonfw.shipkafka.Application;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.bookingcomponent.dtos.BookingCreateDTO;
import com.devonfw.shipkafka.bookingcomponent.dtos.IdDTO;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import com.devonfw.shipkafka.shipcomponent.domain.repositories.ShipRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureJsonTesters
@ActiveProfiles(profiles = "testing")
class CustomerRestControllerTest {

    private final Log log = LogFactory.getLog(getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShipRepository shipRepository;

    private Customer customer;

    private Ship ship;

    @BeforeEach
    void setUp() {
        this.customerRepository.deleteAll();
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
    void getAllCustomersSuccess() {
        //@formatter:off
        given().
                // add this here to log request --> log().all().
        when().
                get("/customers").
        then().
                // add this here to log response --> log().all().
                statusCode(HttpStatus.OK.value()).
                body("lastName", hasItems("Muster"));
        //@formatter:on
    }

    @Test
    void getCustomerSuccess() {
        //@formatter:off
        given().
        when().
                get("/customers/{id}", customer.getId()).
        then().
                statusCode(HttpStatus.OK.value()).
                body("lastName", equalTo("Muster"));
        //@formatter:on
    }

    @Test
    void getCustomerFailBecauseOfNotFound() {
        //@formatter:off
        given().
        when().
                get("/customers/{id}", Integer.MAX_VALUE).
        then().
                statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }

    @ParameterizedTest
    @CsvSource({
            "Hui,Boo",
            "H,B",
            "12345678901234567890,12345678901234567890"
    })
    void createCustomerSuccess(ArgumentsAccessor arguments) {
        JSONObject customerCreateDTO = new JSONObject();
        String[] attributes = new String[]{"firstName", "lastName"};
        Stream<String> attributeStream = Arrays.stream(attributes);
        attributeStream.forEach(attribute -> {
            if (arguments.getString(Arrays.asList(attributes).indexOf(attribute)) != null) {
                try {
                    customerCreateDTO.put(attribute, arguments.getString(Arrays.asList(attributes).indexOf(attribute)));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //@formatter:off
        given().
                contentType(ContentType.JSON).
                body(customerCreateDTO.toString()).
        when().
                post("/customers").
        then().
                statusCode(HttpStatus.CREATED.value()).
                body("id", is(greaterThan(0)));
        //@formatter:on
    }

    @ParameterizedTest
    @CsvSource({
            ",Boo",
            "Hui,",
            "123456789012345678901,12345678901234567890",
            "12345678901234567890,123456789012345678901"
    })
    void createCustomerFailBecauseOfValidationFailure(ArgumentsAccessor arguments) {
        JSONObject customerCreateDTO = new JSONObject();
        String[] attributes = new String[]{"firstName", "lastName"};
        Stream<String> attributeStream = Arrays.stream(attributes);
        attributeStream.forEach(attribute -> {
            if (arguments.getString(Arrays.asList(attributes).indexOf(attribute)) != null) {
                try {
                    customerCreateDTO.put(attribute, arguments.getString(Arrays.asList(attributes).indexOf(attribute)));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //@formatter:off
        given().
                contentType(ContentType.JSON).
                body(customerCreateDTO.toString()).
        when().
                post("/customers").
        then().
                statusCode(HttpStatus.BAD_REQUEST.value());
        //@formatter:on
    }

    @Test
    void updateCustomerSuccess() {
        JSONObject customerUpdateDTO = new JSONObject();
        try {
            customerUpdateDTO.put("id", customer.getId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            customerUpdateDTO.put("lastName", "boooohui");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        //@formatter:off
        given().
                contentType(ContentType.JSON).
                body(customerUpdateDTO.toString()).
        when().
                put("/customers").
        then().
                statusCode(HttpStatus.OK.value());
        //@formatter:on
    }

    @Test
    void updateCustomerFailBecauseOfValidationFailure() {
        JSONObject customerUpdateDTO = new JSONObject();
        try {
            customerUpdateDTO.put("id", customer.getId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            customerUpdateDTO.put("lastname", "");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        //@formatter:off
        given().
                contentType(ContentType.JSON).
                body(customerUpdateDTO.toString()).
                when().
                put("/customers").
                then().
                statusCode(HttpStatus.BAD_REQUEST.value());
        //@formatter:on
    }

    @Test
    void deleteCustomerSuccess() {
        //@formatter:off
        given().
                delete("/customers/{id}", customer.getId()).
        then().
                statusCode(HttpStatus.OK.value());
        //@formatter:on
    }

    @Test
    void deleteCustomerFailBecauseOfCustomerNotFound() {
        //@formatter:off
        given().
                delete("/customers/{id}", Integer.MAX_VALUE).
        then().
                statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }

    @Test
    void createBookingSuccess() throws IOException {
        //@formatter:off
        Long bookingId = given().
                contentType(ContentType.JSON).
                body(new BookingCreateDTO(ship.getId(), 5)).
        when().
                post("/customers/{id}/bookings", customer.getId()).
        then().
                statusCode(HttpStatus.CREATED.value()).
                body("id", is(greaterThan(0))).
        extract().
                body().as(IdDTO.class).getId();
        //@formatter:on
    }

    @Test
    void addBookingToCustomerFailBecauseOfCustomerNotFound() {
        //@formatter:off
        given().
                contentType(ContentType.JSON).
                body(new BookingCreateDTO(ship.getId(), 3)).
        when().
                post("/customers/{id}/bookings", Integer.MAX_VALUE).
        then().
                statusCode(HttpStatus.NOT_FOUND.value());
        //@formatter:on
    }
}