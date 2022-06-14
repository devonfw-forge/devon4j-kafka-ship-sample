package com.devonfw.shipkafka.shipcomponent.logic;

import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.common.exceptions.BookingAlreadyConfirmedException;
import com.devonfw.shipkafka.shipcomponent.exceptions.ShipNotFoundException;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import com.devonfw.shipkafka.shipcomponent.domain.repositories.ShipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipComponentLogic {

    private final ShipRepository shipRepository;

    private final KafkaTemplate<Long, Object> template;

    private static final Logger LOG = LoggerFactory.getLogger(ShipComponentLogic.class);

    @Autowired
    public ShipComponentLogic(ShipRepository shipRepository, KafkaTemplate<Long, Object> template){
        this.shipRepository = shipRepository;
        this.template = template;
    }

    @Transactional(rollbackFor = {BookingAlreadyConfirmedException.class})
    public void confirmBooking(Booking booking) throws BookingAlreadyConfirmedException, ShipNotFoundException {
        Ship ship = shipRepository.findById(booking.getShipId()).orElseThrow(() -> new ShipNotFoundException(booking.getShipId()));
        LOG.info("Found: {}", ship);

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            throw new BookingAlreadyConfirmedException(booking.getId());
        } else if (booking.getBookingStatus().equals(BookingStatus.REQUESTED)) {
            if(booking.getContainerCount() < ship.getAvailableContainers()){
                ship.setAvailableContainers(ship.getAvailableContainers() - booking.getContainerCount());
                booking.updateBookingStatus(BookingStatus.CONFIRMED);
                shipRepository.save(ship);
            } else {
                booking.updateBookingStatus(BookingStatus.CANCELED);
            }
        }

        template.send("ship-bookings", booking.getId(), booking);
        LOG.info("Sent: {}", booking);
    }

    public <T> void sendMessage(String topic, T message) {
        template.send(topic, message);
        LOG.info("Sent: {}", message);
    }
}
