package com.devonfw.shipkafka.bookingcomponent.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@ResponseStatus(HttpStatus.NOT_FOUND)
@EqualsAndHashCode(callSuper = false)
public class BookingNotFoundException extends Exception {

    private final String bookingNumber;

    public BookingNotFoundException(String bookingCode) {
        super(String.format("Could not find booking with code %s.", bookingCode));

        this.bookingNumber = bookingCode;
    }

    public BookingNotFoundException(Long bookingId) {
        super(String.format("Could not find booking with number %d.", bookingId));

        this.bookingNumber = bookingId.toString();
    }
}