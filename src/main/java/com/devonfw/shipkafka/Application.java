package com.devonfw.shipkafka;

import com.devonfw.shipkafka.common.domain.datatypes.BookingStatus;
import com.devonfw.shipkafka.common.domain.entities.Booking;
import com.devonfw.shipkafka.bookingcomponent.domain.entities.Customer;
import com.devonfw.shipkafka.bookingcomponent.domain.repositories.CustomerRepository;
import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import com.devonfw.shipkafka.shipcomponent.domain.repositories.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@SpringBootApplication
public class Application implements HealthIndicator {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public Health health() {
		return Health.up().build();
	}
}


@Component
@Profile("default")
class PopulateTestDataRunner implements CommandLineRunner {

	private final CustomerRepository customerRepository;

	private final ShipRepository shipRepository;

	@Autowired
	public PopulateTestDataRunner(CustomerRepository customerRepository, ShipRepository shipRepository) {
		super();
		this.customerRepository = customerRepository;
		this.shipRepository = shipRepository;
	}

	@Override
	public void run(String... args) {

		shipRepository.save(new Ship("Ship Aachen", 2));
		Ship shipBerlin = new Ship("Ship Berlin", 5);
		shipRepository.save(shipBerlin);
		Ship shipHamburg = new Ship("Ship Hamburg", 8);
		shipRepository.save(shipHamburg);

		Arrays.asList(
						"Miller,Doe,Smith".split(","))
				.forEach(
						name -> customerRepository.save(new Customer("Jane", name))
				);

		Customer customer = new Customer("Max", "Muster");
		Booking booking = new Booking(shipBerlin.getId(), 3);
		booking.updateBookingStatus(BookingStatus.CANCELED);
		customer.getBookings().add(booking);
		booking = new Booking(shipHamburg.getId(), 5);
		booking.updateBookingStatus(BookingStatus.CONFIRMED);
		customer.getBookings().add(booking);

		customerRepository.save(customer);
	}
}
