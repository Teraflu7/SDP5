package com.delivery.strategy.impl;

import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.Comparator;
import java.util.List;

public class FastestRouteStrategy implements RouteStrategy {

    @Override
    public String getStrategyName() {
        return "FastestRouteStrategy";
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        ValidatedCandidateRoutes validated = applyHardConstraints(routes, request);

        // Sorting by effective time = estimatedMinutes + trafficPenaltyMinutes
        Route best = validated.validRoutes().stream()
                .min(Comparator.comparingDouble(Route::getEffectiveTime))
                .orElseThrow();

        String explanation = String.format("Selected route %s with minimum effective time of %.1f mins (base: %d min, traffic delay: +%.1f min).",
                best.id(), best.getEffectiveTime(), best.estimatedMinutes(), best.trafficPenaltyMinutes());

        return new RouteDecision(getStrategyName(), best, explanation, validated.rejectionReasons());
    }
}