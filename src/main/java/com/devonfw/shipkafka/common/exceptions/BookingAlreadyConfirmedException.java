package com.devonfw.shipkafka.common.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@ResponseStatus(HttpStatus.BAD_REQUEST)
@EqualsAndHashCode(callSuper = false)
public class BookingAlreadyConfirmedException extends Exception {

    private final Long bookingId;

    public BookingAlreadyConfirmedException(Long bookingId) {
        super(String.format("Booking with number %d was already confirmed.", bookingId));

        this.bookingId = bookingId;
    }
}