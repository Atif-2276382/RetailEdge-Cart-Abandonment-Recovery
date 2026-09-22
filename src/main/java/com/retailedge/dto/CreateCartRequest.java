package com.retailedge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCartRequest(
        @NotBlank @Email @Size(max = 320) String customerEmail,
        @NotNull @DecimalMin(value = "0.00") BigDecimal cartTotal) {
}