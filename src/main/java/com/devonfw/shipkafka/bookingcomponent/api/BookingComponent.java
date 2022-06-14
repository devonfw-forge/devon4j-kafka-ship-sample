package com.devonfw.shipkafka.bookingcomponent.api;

import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.BookingRepository;
import com.devonfw.shipkafka.common.exceptions.BookingAlreadyConfirmedException;
import com.devonfw.shipkafka.bookingcomponent.exceptions.BookingNotFoundException;
import com.devonfw.shipkafka.bookingcomponent.logic.BookingComponentBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingComponent {

    private final BookingComponentBusinessLogic bookingComponentBusinessLogic;

    private final BookingRepository bookingRepository;

    @Autowired
    public BookingComponent(BookingComponentBusinessLogic bookingComponentBusinessLogic, BookingRepository bookingRepository) {
        this.bookingComponentBusinessLogic = bookingComponentBusinessLogic;
        this.bookingRepository = bookingRepository;
    }

    public Optional<Booking> getBooking(Long bookingId) {

        return bookingRepository.findById(bookingId);
    }

//    public void confirm(Long bookingId) throws BookingNotFoundException, BookingAlreadyConfirmedException {
//
//        bookingComponentBusinessLogic.confirmBooking(bookingId);
//    }
}
