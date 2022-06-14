package com.devonfw.shipkafka.bookingcomponent.api;

import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.bookingcomponent.dtos.BookingCreateDTO;
import com.devonfw.shipkafka.bookingcomponent.dtos.CustomerCreateDTO;
import com.devonfw.shipkafka.bookingcomponent.dtos.CustomerUpdateDTO;
import com.devonfw.shipkafka.bookingcomponent.dtos.IdDTO;
import com.devonfw.shipkafka.bookingcomponent.exceptions.CustomerNotFoundException;
import com.devonfw.shipkafka.bookingcomponent.logic.BookingComponentBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/customers")
public class CustomerRestController {

    private final BookingComponentBusinessLogic bookingComponentBusinessLogic;

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerRestController(BookingComponentBusinessLogic bookingComponentBusinessLogic, CustomerRepository customerRepository) {
        this.bookingComponentBusinessLogic = bookingComponentBusinessLogic;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<Customer> getCustomers() {

        return customerRepository.findAll();
    }

    @GetMapping(value = "/{id:\\d+}")
    public Customer getCustomer(@PathVariable("id") Long customerId) throws CustomerNotFoundException {

        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @GetMapping(value = "/{id:\\d+}/bookings")
    public List<Booking> getBookingsOfCustomer(@PathVariable("id") Long customerId, @RequestParam(value = "onlyConfirmed", required = false, defaultValue = "false") Boolean onlyConfirmed) throws CustomerNotFoundException {
        return bookingComponentBusinessLogic.getBookings(customerId, onlyConfirmed);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCustomer(@PathVariable("id") Long customerId) throws CustomerNotFoundException {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customerRepository.delete(customer);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdDTO addCustomer(@Valid @RequestBody CustomerCreateDTO customerCreateDTO) {

        return new IdDTO(customerRepository.save(Customer.of(customerCreateDTO)).getId());
    }

    @PutMapping
    public void updateCustomer(@Valid @RequestBody CustomerUpdateDTO customerUpdateDTO) throws CustomerNotFoundException {

        Customer customerToUpdate = customerRepository
                .findById(customerUpdateDTO.getId())
                .orElseThrow(() -> new CustomerNotFoundException(customerUpdateDTO.getId()));

        if (customerUpdateDTO.getLastName() != null) {
            customerToUpdate.setLastName(customerUpdateDTO.getLastName());
        }

        customerRepository.save(customerToUpdate);
    }

    @PostMapping(value = "/{id:\\d+}/bookings")
    public ResponseEntity<?> addBooking(@PathVariable("id") Long customerId, @Valid @RequestBody BookingCreateDTO bookingCreateDTO) {
        try {
            Booking booking = bookingComponentBusinessLogic.addBooking(customerId, bookingCreateDTO);
            return new ResponseEntity<>(new IdDTO(booking.getId()), HttpStatus.CREATED);
        } catch (CustomerNotFoundException ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
