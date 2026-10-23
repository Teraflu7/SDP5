package com.delivery.strategy.impl;

import com.delivery.exception.InvalidStrategyConfigException;
import com.delivery.model.DeliveryRequest;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.strategy.RouteStrategy;

import java.util.Comparator;
import java.util.List;

public class BalancedRouteStrategy implements RouteStrategy {

    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double timeWeight, double costWeight, double distanceWeight, double emissionWeight) {
        double sum = timeWeight + costWeight + distanceWeight + emissionWeight;
        if (Math.abs(sum - 1.0) > 0.0001) {
            throw new InvalidStrategyConfigException(
                    String.format("Weights must sum to 1.0. Given sum: %.4f (t=%.2f, c=%.2f, d=%.2f, e=%.2f)",
                            sum, timeWeight, costWeight, distanceWeight, emissionWeight));
        }
        if (timeWeight < 0 || costWeight < 0 || distanceWeight < 0 || emissionWeight < 0) {
            throw new InvalidStrategyConfigException("Weights cannot be negative.");
        }
        this.timeWeight = timeWeight;
        this.costWeight = costWeight;
        this.distanceWeight = distanceWeight;
        this.emissionWeight = emissionWeight;
    }

    public BalancedRouteStrategy() {
        this(0.40, 0.30, 0.10, 0.20);
    }

    @Override
    public String getStrategyName() {
        return "BalancedRouteStrategy";
    }

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        ValidatedCandidateRoutes validated = applyHardConstraints(routes, request);
        List<Route> list = validated.validRoutes();

        // Calculate maximums for min-max normalization
        double maxTime = list.stream().mapToDouble(Route::getEffectiveTime).max().orElse(1.0);
        double maxCost = list.stream().mapToDouble(Route::cost).max().orElse(1.0);
        double maxDist = list.stream().mapToDouble(Route::distanceKm).max().orElse(1.0);
        double maxCo2 = list.stream().mapToDouble(Route::carbonEmission).max().orElse(1.0);

        Route best = list.stream()
                .min(Comparator.comparingDouble(r -> computeScore(r, maxTime, maxCost, maxDist, maxCo2)))
                .orElseThrow();

        double finalScore = computeScore(best, maxTime, maxCost, maxDist, maxCo2);
        String explanation = String.format("Selected route %s with balanced score %.4f [weights: time=%.2f, cost=%.2f, dist=%.2f, co2=%.2f].",
                best.id(), finalScore, timeWeight, costWeight, distanceWeight, emissionWeight);

        return new RouteDecision(getStrategyName(), best, explanation, validated.rejectionReasons());
    }

    private double computeScore(Route r, double maxTime, double maxCost, double maxDist, double maxCo2) {
        double normTime = (maxTime > 0) ? r.getEffectiveTime() / maxTime : 0;
        double normCost = (maxCost > 0) ? r.cost() / maxCost : 0;
        double normDist = (maxDist > 0) ? r.distanceKm() / maxDist : 0;
        double normCo2 = (maxCo2 > 0) ? r.carbonEmission() / maxCo2 : 0;

        return (normTime * timeWeight) + (normCost * costWeight) + (normDist * distanceWeight) + (normCo2 * emissionWeight);
    }
}