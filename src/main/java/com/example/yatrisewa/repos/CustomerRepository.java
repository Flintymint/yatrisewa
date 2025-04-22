package com.example.yatrisewa.repos;

import com.example.yatrisewa.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByMobileOrEmail(String mobile, String email);
    Optional<Customer> findByMobile(String mobile);
    Optional<Customer> findByEmail(String email);

    Boolean existsByEmail(String email);
    Boolean existsByMobile(String mobile);
    Boolean existsByMobileOrEmail(String mobile, String email);
}
