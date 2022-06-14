package com.devonfw.shipkafka.bookingcomponent.logic;

import com.devonfw.shipkafka.Application;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles(profiles = "testing")
class BookingComponentLogicTest {

    @Autowired
    private BookingComponentBusinessLogic bookingComponentBusinessLogic;

    @MockBean
    private CustomerRepository customerRepository;

    @Test
    void getCustomersSuccess() {
        given(this.customerRepository.findAll()).willReturn(
                Collections.singletonList(new Customer("Jane", "Doe")));

        List<Customer> actual = bookingComponentBusinessLogic.getCustomers();
        assertThat(actual).size().isEqualTo(1);
        assertThat(actual.get(0).getFirstName()).isEqualTo("Jane");
    }
}