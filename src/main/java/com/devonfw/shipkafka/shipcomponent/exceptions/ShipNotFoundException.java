package com.devonfw.shipkafka.shipcomponent.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@ResponseStatus(HttpStatus.BAD_REQUEST)
@EqualsAndHashCode(callSuper = false)
public class ShipNotFoundException extends Exception {

    private final Long shipId;

    public ShipNotFoundException(Long shipId) {
        super(String.format("Ship %s was not found.", shipId));

        this.shipId = shipId;
    }
}