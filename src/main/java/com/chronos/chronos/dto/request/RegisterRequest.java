package com.chronos.chronos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// @Data is Lombok — generates getters, setters, equals, hashCode, toString
// We use @Data on DTOs because they just carry data — no business logic
@Data
public class RegisterRequest {

    // @NotBlank = cannot be null, empty string, or whitespace only
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    // @Email = validates that the string looks like a real email address
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    // @Size validates password length before we even touch the database
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
