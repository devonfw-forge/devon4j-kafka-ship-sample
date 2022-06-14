package com.devonfw.shipkafka.bookingcomponent.domain.repositories;

import com.devonfw.shipkafka.common.domain.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b, Customer c WHERE c.id = :customerId AND b member of c.bookings " +
            "AND b.bookingStatus = com.devonfw.shipkafka.common.domain.datatypes.BookingStatus.CONFIRMED")
    List<Booking> findConfirmedBookings(Long customerId);

    List<Booking> findBookingsByShipId(Long shipId);
}
