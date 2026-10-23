package com.delivery.strategy.impl;

import com.delivery.model.DeliveryPriority;
import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.Comparator;
import java.util.List;

public class CheapestRouteStrategy implements RouteStrategy {

    private final double distanceCostPerKm;
    private final double handlingFee;

    public CheapestRouteStrategy() {
        this(15.0, 50.0);
    }

    public CheapestRouteStrategy(double distanceCostPerKm, double handlingFee) {
        this.distanceCostPerKm = distanceCostPerKm;
        this.handlingFee = handlingFee;
    }

    public double calculateTotalCost(Route route, DeliveryRequest request) {
        double prioritySurcharge = (request.priority() == DeliveryPriority.URGENT) ? 300.0 : 0.0;
        return route.cost() + (route.distanceKm() * distanceCostPerKm) + handlingFee + prioritySurcharge;
    }

    @Override
    public String getStrategyName() {
        return "CheapestRouteStrategy";
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        ValidatedCandidateRoutes validated = applyHardConstraints(routes, request);

        Route best = validated.validRoutes().stream()
                .min(Comparator.comparingDouble(r -> calculateTotalCost(r, request)))
                .orElseThrow();

        double totalCost = calculateTotalCost(best, request);
        String explanation = String.format("Selected route %s with lowest calculated cost of %.2f (base: %.2f + distance fee: %.2f + handling: %.2f).",
                best.id(), totalCost, best.cost(), best.distanceKm() * distanceCostPerKm, handlingFee);

        return new RouteDecision(getStrategyName(), best, explanation, validated.rejectionReasons());
    }
}