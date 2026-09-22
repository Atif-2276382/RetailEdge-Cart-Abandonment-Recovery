package com.retailedge.dto;

import jakarta.validation.constraints.Min;

public record UpdateStockRequest(@Min(0) int stock) {
}