package com.devonfw.shipkafka.bookingcomponent.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@ResponseStatus(HttpStatus.NOT_FOUND)
@EqualsAndHashCode(callSuper = false)
public class CustomerNotFoundException extends Exception {

    private final Long customerId;

    public CustomerNotFoundException(Long customerId) {
        super(String.format("Could not find customer with number %d.", customerId));

        this.customerId = customerId;
    }
}