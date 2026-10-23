package com.delivery.context;

import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RoutePlanner {

    private RouteStrategy strategy;

    public RoutePlanner(RouteStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Initial strategy must not be null");
    }

    public void setStrategy(RouteStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
    }

    public RouteStrategy getStrategy() {
        return strategy;
    }

    public RouteDecision plan(List<Route> routes, DeliveryRequest request) {
        // Delegates directly to strategy (DIP & SRP)
        return strategy.selectRoute(routes, request);
    }

    /**
     * Executes comparison mode across given strategies for the same request
     */
    public Map<String, RouteDecision> compareAll(List<Route> routes, DeliveryRequest request, List<RouteStrategy> strategies) {
        Map<String, RouteDecision> results = new LinkedHashMap<>();
        for (RouteStrategy strat : strategies) {
            results.put(strat.getStrategyName(), strat.selectRoute(routes, request));
        }
        return results;
    }
}