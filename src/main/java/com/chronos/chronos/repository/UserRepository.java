package com.chronos.chronos.repository;

import com.chronos.chronos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// JpaRepository<User, UUID> means:
// - This repository works with the User entity
// - The primary key type is UUID
// Spring automatically generates all standard DB operations:
// save(), findById(), findAll(), delete() etc.
// We don't write any implementation — Spring does it for us!
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring reads this method name and automatically generates the SQL:
    // SELECT * FROM users WHERE email = ?
    // This is called "derived queries" — Spring derives SQL from method names
    Optional<User> findByEmail(String email);

    // Generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
    // Used to check for duplicate emails during registration
    boolean existsByEmail(String email);
}
