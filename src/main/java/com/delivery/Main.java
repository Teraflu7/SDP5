package com.delivery;

import com.delivery.context.RoutePlanner;
import com.delivery.model.*;
import com.delivery.selector.StrategySelector;
import com.delivery.strategy.RouteStrategy;
import com.delivery.strategy.impl.*;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  SMART DELIVERY & ROUTE OPTIMIZATION ENGINE (Strategy Pattern)");
        System.out.println("===============================================================\n");

        Location centralHub = new Location("Central Hub", 51.1605, 71.4704);
        Location airportDist = new Location("Airport Cargo Area", 51.0222, 71.4669);

        // 8 Candidate routes (Section 3 of assignment)
        List<Route> candidateRoutes = List.of(
                new Route("R1-HIGHWAY", centralHub, airportDist, 24.5, 25, 4500.0, 4.2, true, 2.0),
                new Route("R2-EXPRESSWAY", centralHub, airportDist, 28.0, 20, 6200.0, 5.0, true, 1.0),
                new Route("R3-SUBURBAN", centralHub, airportDist, 22.0, 40, 2800.0, 3.1, true, 5.0),
                new Route("R4-ECO-TRAIL", centralHub, airportDist, 21.0, 35, 3100.0, 1.8, true, 0.0),
                new Route("R5-NIGHT-RING", centralHub, airportDist, 35.0, 30, 3900.0, 2.9, true, 0.0),
                new Route("R6-TUNNEL-DIR", centralHub, airportDist, 18.5, 22, 5400.0, 3.5, true, 8.0),
                new Route("R7-CLOSED-HWY", centralHub, airportDist, 19.0, 18, 2500.0, 2.0, false, 0.0), // Unavailable
                new Route("R8-EXPENSIVE-AIR", centralHub, airportDist, 30.0, 15, 12000.0, 7.5, true, 0.0) // High cost
        );

        DeliveryRequest standardReq = new DeliveryRequest("REQ-101", DeliveryPriority.STANDARD, 8000.0, 45, false, 6.0);
        DeliveryRequest urgentReq   = new DeliveryRequest("REQ-102", DeliveryPriority.URGENT, 10000.0, 30, false, 6.0);
        DeliveryRequest ecoReq      = new DeliveryRequest("REQ-103", DeliveryPriority.ECO, 7000.0, 50, true, 4.0);

        // 1. Runtime Strategy Switching Demonstration (Section 7)
        System.out.println(">>> 1. DEMONSTRATION OF RUNTIME STRATEGY SWITCHING (3+ switches):");
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        System.out.println(planner.plan(candidateRoutes, standardReq));

        planner.setStrategy(new CheapestRouteStrategy());
        System.out.println(planner.plan(candidateRoutes, standardReq));

        planner.setStrategy(new EcoFriendlyRouteStrategy());
        System.out.println(planner.plan(candidateRoutes, standardReq));

        planner.setStrategy(new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
        System.out.println(planner.plan(candidateRoutes, standardReq));

        // 2. Automatic Strategy Selection (Section 8)
        System.out.println("\n>>> 2. AUTOMATIC STRATEGY SELECTION BY SELECTOR:");
        StrategySelector selector = new StrategySelector();
        selector.registerStrategy(DeliveryPriority.EXPRESS_DISTANCE, new ShortestDistanceRouteStrategy());

        RouteStrategy autoStrategy1 = selector.resolveStrategy(urgentReq);
        System.out.printf("For Request %s (Priority: %s) -> Resolved: %s\n", urgentReq.orderId(), urgentReq.priority(), autoStrategy1.getStrategyName());
        planner.setStrategy(autoStrategy1);
        System.out.println(planner.plan(candidateRoutes, urgentReq));

        RouteStrategy autoStrategy2 = selector.resolveStrategy(ecoReq);
        System.out.printf("For Request %s (Priority: %s) -> Resolved: %s\n", ecoReq.orderId(), ecoReq.priority(), autoStrategy2.getStrategyName());
        planner.setStrategy(autoStrategy2);
        System.out.println(planner.plan(candidateRoutes, ecoReq));

        // 3. Strategy Comparison Mode (Section 12)
        System.out.println("\n>>> 3. STRATEGY COMPARISON MODE (Same Request Evaluated by All Strategies):");
        List<RouteStrategy> allStrategies = List.of(
                new FastestRouteStrategy(),
                new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(),
                new BalancedRouteStrategy(),
                new ShortestDistanceRouteStrategy()
        );

        Map<String, RouteDecision> comparison = planner.compareAll(candidateRoutes, standardReq, allStrategies);
        comparison.forEach((stratName, decision) -> {
            Route r = decision.selectedRoute();
            System.out.printf("Strategy: %-28s | Route: %-14s | Time: %4.1f min | Cost: %7.1f | CO2: %4.2f kg | Dist: %4.1f km\n",
                    stratName, r.id(), r.getEffectiveTime(), r.cost(), r.carbonEmission(), r.distanceKm());
        });
    }
}