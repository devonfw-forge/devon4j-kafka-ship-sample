package com.devonfw.shipkafka.bookingcomponent.logic;
import com.devonfw.shipkafka.bookingcomponent.gateway.BookingComponentMessagingGateway;
import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.BookingRepository;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.bookingcomponent.dtos.BookingCreateDTO;
import com.devonfw.shipkafka.common.exceptions.BookingAlreadyConfirmedException;
import com.devonfw.shipkafka.bookingcomponent.exceptions.BookingNotFoundException;
import com.devonfw.shipkafka.bookingcomponent.exceptions.CustomerNotFoundException;
import com.devonfw.shipkafka.shipcomponent.exceptions.ShipNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookingComponentBusinessLogic {

    private final CustomerRepository customerRepository;

    private final BookingRepository bookingRepository;

    private final BookingComponentMessagingGateway bookingComponentMessagingGateway;

    @Autowired
    public BookingComponentBusinessLogic(CustomerRepository customerRepository,
                                         BookingRepository bookingRepository,
                                         @Lazy BookingComponentMessagingGateway bookingComponentMessagingGateway){
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.bookingComponentMessagingGateway = bookingComponentMessagingGateway;
    }

    @Transactional(readOnly = true)
    public List<Customer> getCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(rollbackFor = {CustomerNotFoundException.class})
    public Booking addBooking(Long customerId, BookingCreateDTO bookingCreateDTO) throws CustomerNotFoundException{
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);

        if (optionalCustomer.isPresent()) {
            Customer customer = optionalCustomer.get();

            //Booking booking = bookingRepository.save(Booking.of(bookingCreateDTO));
            Booking booking = bookingRepository.save(new Booking(bookingCreateDTO.getShipId(), bookingCreateDTO.getContainerCount()));

            customer.addBooking(booking);
            customerRepository.save(customer);

            bookingComponentMessagingGateway.sendMessage("bookings", booking.getId(), booking);


            return booking;
        } else {
            throw new CustomerNotFoundException(customerId);
        }
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookings(Long customerId, Boolean onlyConfirmed) throws CustomerNotFoundException {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (onlyConfirmed) {
            return bookingRepository.findConfirmedBookings(customerId);
        } else {
            return customer.getBookings();
        }
    }

    //@Transactional(rollbackFor = {ShipNotFoundException.class})
    public void cancelBookings(Long shipId){
        List<Booking> bookings = bookingRepository.findBookingsByShipId(shipId);

        if (bookings.isEmpty()){
            //throw new ShipNotFoundException(shipId);
            return;
        }

        for (Booking booking : bookings) {
            booking.updateBookingStatus(BookingStatus.CANCELED);
            bookingRepository.save(booking);
        }
    }

    public void processBooking(Booking booking) throws BookingNotFoundException {
        Booking b = bookingRepository
                .findById(booking.getId())
                .orElseThrow(() -> new BookingNotFoundException(booking.getId()));

        //if(b.getBookingStatus() != BookingStatus.CANCELED){
            b.updateBookingStatus(booking.getBookingStatus());
            bookingRepository.save(b);
        //}
    }
}
