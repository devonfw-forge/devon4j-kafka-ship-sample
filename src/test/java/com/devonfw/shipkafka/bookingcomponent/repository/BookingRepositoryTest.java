package com.devonfw.shipkafka.bookingcomponent.repository;

import com.devonfw.shipkafka.Application;
import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.BookingRepository;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles(profiles = "testing")
class BookingRepositoryTest {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        this.customerRepository.deleteAll();
        this.bookingRepository.deleteAll();

        customer = this.customerRepository.save(new Customer("Max", "Muster"));
        Booking unconfirmedBooking = new Booking(new Ship("Mein Schiff 1", 5).getId(), 3);
        customer.addBooking(unconfirmedBooking);
        Booking confirmedBooking = new Booking(new Ship("Mein Schiff 2", 5).getId(), 3);
        confirmedBooking.updateBookingStatus(BookingStatus.CONFIRMED);
        customer.addBooking(confirmedBooking);

        customerRepository.save(customer);
    }

    @Test
    void findConfirmedBookingsSuccess() {
        List<Booking> actual = bookingRepository.findConfirmedBookings(customer.getId());
        assertThat(actual).size().isEqualTo(1);
    }
}
