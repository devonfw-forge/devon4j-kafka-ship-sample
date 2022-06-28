package com.devonfw.shipkafka.shipcomponent.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Value
@ResponseStatus(HttpStatus.BAD_REQUEST)
@EqualsAndHashCode(callSuper = false)
public class ShipDamagedException  extends Exception{

    private final Long shipId;

    // Testing retry
    public ShipDamagedException(Long shipId) {
        super(String.format("Ship %s is currently damaged.", shipId));

        this.shipId = shipId;
    }
}
