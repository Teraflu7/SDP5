package com.delivery.model;

public record Route(
        String id,
        Location origin,
        Location destination,
        double distanceKm,
        int estimatedMinutes,
        double cost,
        double carbonEmission,
        boolean isAvailable,
        double trafficPenaltyMinutes
) {
    public Route {
        if (distanceKm <= 0 || cost < 0 || estimatedMinutes <= 0 || carbonEmission < 0) {
            throw new IllegalArgumentException("Invalid route metric: values cannot be negative or zero where forbidden");
        }
    }

    public double getEffectiveTime() {
        return estimatedMinutes + trafficPenaltyMinutes;
    }
}