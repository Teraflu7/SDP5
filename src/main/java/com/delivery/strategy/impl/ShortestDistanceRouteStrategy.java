package com.delivery.strategy.impl;

import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.Comparator;
import java.util.List;

public class ShortestDistanceRouteStrategy implements RouteStrategy {

    @Override
    public String getStrategyName() {
        return "ShortestDistanceRouteStrategy";
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        ValidatedCandidateRoutes validated = applyHardConstraints(routes, request);

        Route best = validated.validRoutes().stream()
                .min(Comparator.comparingDouble(Route::distanceKm))
                .orElseThrow();

        String explanation = String.format("Selected route %s with minimum absolute distance of %.2f km.",
                best.id(), best.distanceKm());

        return new RouteDecision(getStrategyName(), best, explanation, validated.rejectionReasons());
    }
}