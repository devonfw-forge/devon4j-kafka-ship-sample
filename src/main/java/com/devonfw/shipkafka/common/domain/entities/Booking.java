package com.devonfw.shipkafka.common.domain.entities;

import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.bookingcomponent.dtos.BookingCreateDTO;
import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Booking {

    @Setter(AccessLevel.NONE)
    private final Date createdOn = new Date();
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Date lastUpdatedOn;

    private  int containerCount;

    private Long shipId;

    @Setter(AccessLevel.NONE)
    private BookingStatus bookingStatus = BookingStatus.REQUESTED;

    @Version
    private Long version;

    public Booking(Long shipId, int containerCount) {

        this.shipId = shipId;
        this.containerCount = containerCount;
        this.lastUpdatedOn = new Date();
    }

//    public static Booking of(BookingCreateDTO bookingCreateDTO) {
//        return new Booking(bookingCreateDTO.shipId, bookingCreateDTO.getContainerCount());
//    }

    public void updateBookingStatus(BookingStatus newStatus) {
        bookingStatus = bookingStatus.transition(newStatus);
        lastUpdatedOn = new Date();
    }
}
