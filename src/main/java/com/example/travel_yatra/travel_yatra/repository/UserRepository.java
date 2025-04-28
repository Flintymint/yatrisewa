package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> fetchAllByRole(@Param("role") String role);
}
