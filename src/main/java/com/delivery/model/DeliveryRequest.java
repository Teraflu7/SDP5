package com.delivery.model;

public record DeliveryRequest(
        String orderId,
        DeliveryPriority priority,
        double maxBudget,
        int maxDeliveryMinutes,
        boolean ecoPreferred,
        double maxCarbonEmissionLimit
) {
    public DeliveryRequest {
        if (maxBudget <= 0 || maxDeliveryMinutes <= 0) {
            throw new IllegalArgumentException("Budget and max delivery time must be positive.");
        }
    }
}