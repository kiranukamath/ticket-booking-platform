package org.ticketbooking.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.ticketbooking.common.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
