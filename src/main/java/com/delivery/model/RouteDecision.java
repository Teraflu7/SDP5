package com.delivery.model;

import java.util.List;

public record RouteDecision(
        String strategyName,
        Route selectedRoute,
        String explanation,
        List<String> rejectedReasons
) {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Strategy: %s ===\n", strategyName));
        if (selectedRoute != null) {
            sb.append(String.format("Selected Route: %s (ID: %s)\n", selectedRoute.origin().name() + " -> " + selectedRoute.destination().name(), selectedRoute.id()));
            sb.append(String.format("Metrics: Cost=%.2f, Time=%.1f min, Dist=%.1f km, CO2=%.2f kg\n",
                    selectedRoute.cost(), selectedRoute.getEffectiveTime(), selectedRoute.distanceKm(), selectedRoute.carbonEmission()));
        } else {
            sb.append("No Route Selected.\n");
        }
        sb.append("Reasoning: ").append(explanation).append("\n");
        if (rejectedReasons != null && !rejectedReasons.isEmpty()) {
            sb.append("Filtered Out Routes:\n");
            for (String r : rejectedReasons) {
                sb.append("  - ").append(r).append("\n");
            }
        }
        return sb.toString();
    }
}