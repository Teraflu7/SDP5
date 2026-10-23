package com.delivery.strategy;

import com.delivery.exception.NoSuitableRouteException;
import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;

import java.util.ArrayList;
import java.util.List;

public interface RouteStrategy {

    RouteDecision selectRoute(List<Route> routes, DeliveryRequest request);

    String getStrategyName();

    /**
     * Common hard constraints validation.
     * Evaluates route availability, budget ceiling, time ceiling, and carbon limit.
     */
    default ValidatedCandidateRoutes applyHardConstraints(List<Route> routes, DeliveryRequest request) {
        if (routes == null || routes.isEmpty()) {
            throw new NoSuitableRouteException("Route candidate list is empty.");
        }
        if (request == null) {
            throw new IllegalArgumentException("DeliveryRequest cannot be null.");
        }

        List<Route> validRoutes = new ArrayList<>();
        List<String> rejectionLogs = new ArrayList<>();

        for (Route r : routes) {
            if (!r.isAvailable()) {
                rejectionLogs.add(String.format("Route %s rejected: Route is temporarily unavailable", r.id()));
                continue;
            }
            if (r.cost() > request.maxBudget()) {
                rejectionLogs.add(String.format("Route %s rejected: Cost (%.2f) exceeds max budget (%.2f)", r.id(), r.cost(), request.maxBudget()));
                continue;
            }
            if (r.getEffectiveTime() > request.maxDeliveryMinutes()) {
                rejectionLogs.add(String.format("Route %s rejected: Time (%.1f min) exceeds deadline (%d min)", r.id(), r.getEffectiveTime(), request.maxDeliveryMinutes()));
                continue;
            }
            if (request.maxCarbonEmissionLimit() > 0 && r.carbonEmission() > request.maxCarbonEmissionLimit()) {
                rejectionLogs.add(String.format("Route %s rejected: Carbon (%.2f kg) exceeds limit (%.2f kg)", r.id(), r.carbonEmission(), request.maxCarbonEmissionLimit()));
                continue;
            }
            validRoutes.add(r);
        }

        if (validRoutes.isEmpty()) {
            throw new NoSuitableRouteException("No routes satisfied all business constraints. Rejections: " + rejectionLogs);
        }

        return new ValidatedCandidateRoutes(validRoutes, rejectionLogs);
    }

    record ValidatedCandidateRoutes(List<Route> validRoutes, List<String> rejectionReasons) {}
}