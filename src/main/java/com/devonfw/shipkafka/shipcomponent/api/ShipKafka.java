package com.devonfw.shipkafka.shipcomponent.api;

import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.common.exceptions.BookingAlreadyConfirmedException;
import com.devonfw.shipkafka.shipcomponent.exceptions.ShipNotFoundException;
import com.devonfw.shipkafka.shipcomponent.logic.ShipComponentLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

public class ShipKafka {

//    private static final Logger LOG = LoggerFactory.getLogger(ShipKafka.class);
//    private final ShipComponentLogic shipComponentLogic;

//    @Autowired
//    public ShipKafka(ShipComponentLogic shipComponentLogic){
//        this.shipComponentLogic = shipComponentLogic;
//    }
//
//    @KafkaListener(id = "bookings", topics = "bookings", groupId = "ship")
//    public void onBookingEvent(Booking booking) throws ShipNotFoundException, BookingAlreadyConfirmedException {
//        LOG.info("Received: {}", booking);
//        shipComponentLogic.confirmBooking(booking);
//    }


}
