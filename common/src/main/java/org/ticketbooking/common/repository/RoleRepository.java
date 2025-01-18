package org.ticketbooking.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketbooking.common.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
