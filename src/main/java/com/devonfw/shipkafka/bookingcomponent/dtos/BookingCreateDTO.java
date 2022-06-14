package com.devonfw.shipkafka.bookingcomponent.dtos;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class BookingCreateDTO {

    @NotNull
    public Long shipId;

    @NotNull
    public int containerCount;

}
