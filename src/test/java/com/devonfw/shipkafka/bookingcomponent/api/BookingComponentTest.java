package com.devonfw.shipkafka.bookingcomponent.api;

import com.devonfw.shipkafka.Application;
import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.BookingRepository;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureJsonTesters
@ActiveProfiles(profiles = "testing")
class BookingComponentTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingComponent bookingComponent;

    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        this.bookingRepository.deleteAll();
        Ship ship = new Ship("Mein Hybrid-Schiff", 5);
        confirmedBooking = this.bookingRepository.save(new Booking(ship.getId(), 3));
        confirmedBooking.updateBookingStatus(BookingStatus.CONFIRMED);
    }

    @Test
    void getBookingSuccess() {
        assertThat(bookingComponent.getBooking(confirmedBooking.getId())).isNotEmpty();
    }

    @Test
    void getBookingFailBecauseOfNotFound() {
        assertThat(bookingComponent.getBooking(0L)).isEmpty();
    }
}