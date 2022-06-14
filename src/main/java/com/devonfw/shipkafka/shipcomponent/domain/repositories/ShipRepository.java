package com.devonfw.shipkafka.shipcomponent.domain.repositories;

import com.devonfw.shipkafka.shipcomponent.domain.entities.Ship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipRepository extends JpaRepository<Ship, Long> {
}
