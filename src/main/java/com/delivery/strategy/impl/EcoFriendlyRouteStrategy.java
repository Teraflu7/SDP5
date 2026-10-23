package com.delivery.strategy.impl;

import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.Comparator;
import java.util.List;

public class EcoFriendlyRouteStrategy implements RouteStrategy {

    private final double emissionWeight;
    private final double distanceWeight;

    public EcoFriendlyRouteStrategy() {
        this(0.70, 0.30);
    }

    public EcoFriendlyRouteStrategy(double emissionWeight, double distanceWeight) {
        this.emissionWeight = emissionWeight;
        this.distanceWeight = distanceWeight;
    }

    public double calculateEcoScore(Route route) {
        // EcoScore = carbonEmission * emissionWeight + distance * distanceWeight (lowest is cleanest)
        return (route.carbonEmission() * emissionWeight) + (route.distanceKm() * distanceWeight);
    }

    @Override
    public String getStrategyName() {
        return "EcoFriendlyRouteStrategy";
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        ValidatedCandidateRoutes validated = applyHardConstraints(routes, request);

        Route best = validated.validRoutes().stream()
                .min(Comparator.comparingDouble(this::calculateEcoScore))
                .orElseThrow();

        double score = calculateEcoScore(best);
        String explanation = String.format("Selected route %s with best eco-score %.3f (CO2: %.2f kg, dist: %.1f km, weights: %.2f/%.2f).",
                best.id(), score, best.carbonEmission(), best.distanceKm(), emissionWeight, distanceWeight);

        return new RouteDecision(getStrategyName(), best, explanation, validated.rejectionReasons());
    }
}