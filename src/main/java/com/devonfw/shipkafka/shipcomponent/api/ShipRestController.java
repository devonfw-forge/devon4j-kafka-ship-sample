package com.devonfw.shipkafka.shipcomponent.api;

import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.common.events.ShipDamagedEvent;
import com.devonfw.shipkafka.common.exceptions.BookingAlreadyConfirmedException;
import com.devonfw.shipkafka.shipcomponent.exceptions.ShipNotFoundException;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import com.devonfw.shipkafka.shipcomponent.domain.repositories.ShipRepository;
import com.devonfw.shipkafka.shipcomponent.logic.ShipComponentLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/ships")
public class ShipRestController {

    private final ShipComponentLogic shipComponentLogic;

    private final ShipRepository shipRepository;

    private static final Logger LOG = LoggerFactory.getLogger(ShipRestController.class);

    @Autowired
    public ShipRestController(ShipComponentLogic shipComponentLogic, ShipRepository shipRepository) {
        this.shipComponentLogic = shipComponentLogic;
        this.shipRepository = shipRepository;
    }

    @GetMapping
    public List<Ship> getShips() {
        return shipRepository.findAll();
    }

    @GetMapping(value = "/{id:\\d+}")
    public Ship getShip(@PathVariable("id") Long shipId) throws ShipNotFoundException {
        return shipRepository
                .findById(shipId)
                .orElseThrow(() -> new ShipNotFoundException(shipId));
    }

    @PostMapping(value = "/{id:\\d+}/damage")
    @ResponseStatus(HttpStatus.OK)
    public ShipDamagedEvent postDamagedShip(@PathVariable("id") Long shipId) {
        ShipDamagedEvent shipDamagedEvent = new ShipDamagedEvent(shipId);
        shipComponentLogic.sendMessage("ship-damaged", shipDamagedEvent);
        return shipDamagedEvent;
    }

    //TODO: move it somewhere else
    @KafkaListener(id = "bookings", topics = "bookings", groupId = "ship")
    public void onBookingEvent(Booking booking) throws ShipNotFoundException, BookingAlreadyConfirmedException {
        LOG.info("Received: {}", booking);
        shipComponentLogic.confirmBooking(booking);
    }
}
