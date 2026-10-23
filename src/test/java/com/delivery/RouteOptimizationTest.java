package com.delivery;

import com.delivery.context.RoutePlanner;
import com.delivery.exception.InvalidStrategyConfigException;
import com.delivery.exception.NoSuitableRouteException;
import com.delivery.model.DeliveryPriority;
import com.delivery.model.DeliveryRequest;
import com.delivery.model.Location;
import com.delivery.model.Route;
import com.delivery.model.RouteDecision;
import com.delivery.selector.StrategySelector;
import com.delivery.strategy.RouteStrategy;
import com.delivery.strategy.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RouteOptimizationTest {

    private List<Route> sampleRoutes;
    private Location locA;
    private Location locB;

    @BeforeEach
    void setUp() {
        locA = new Location("Point A", 51.1, 71.4);
        locB = new Location("Point B", 51.2, 71.5);

        // Точно выверенные параметры: у каждой стратегии свой уникальный и безоговорочный победитель
        sampleRoutes = List.of(
                new Route("R-FAST",   locA, locB, 30.0, 15, 6000.0, 5.0, true, 2.0), // Time: 17 min (быстрее всех)
                new Route("R-CHEAP",  locA, locB, 25.0, 40, 2000.0, 4.0, true, 0.0), // Стоимость: 2000 (дешевле всех)
                new Route("R-SHORT",  locA, locB, 10.0, 30, 4000.0, 5.0, true, 0.0), // Дистанция: 10 км (минимальная, но CO2=5.0)
                new Route("R-ECO",    locA, locB, 14.0, 35, 3200.0, 0.5, true, 1.0), // Эко-счет: (0.5*0.7)+(14*0.3)=4.55 против R-SHORT (5.0*0.7)+(10*0.3)=6.50
                new Route("R-UNAVAIL",locA, locB,  8.0, 10, 1000.0, 1.0, false, 0.0) // Недоступен
        );
    }

    @Test
    @DisplayName("1. Fastest Strategy selects route with lowest effective time considering traffic")
    void testFastestRouteStrategy() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-1", DeliveryPriority.URGENT, 10000, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertEquals("R-FAST", decision.selectedRoute().id());
        assertEquals(17.0, decision.selectedRoute().getEffectiveTime());
    }

    @Test
    @DisplayName("2. Cheapest Strategy picks route with lowest overall cost formula")
    void testCheapestRouteStrategy() {
        RoutePlanner planner = new RoutePlanner(new CheapestRouteStrategy(10.0, 20.0));
        DeliveryRequest req = new DeliveryRequest("REQ-2", DeliveryPriority.ECONOMY, 10000, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertEquals("R-CHEAP", decision.selectedRoute().id());
    }

    @Test
    @DisplayName("3. Eco Strategy minimizes carbon footprint score")
    void testEcoFriendlyRouteStrategy() {
        RoutePlanner planner = new RoutePlanner(new EcoFriendlyRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-3", DeliveryPriority.ECO, 10000, 60, true, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertEquals("R-ECO", decision.selectedRoute().id());
    }

    @Test
    @DisplayName("4. Balanced Strategy successfully normalizes metrics and computes score")
    void testBalancedRouteStrategy() {
        RoutePlanner planner = new RoutePlanner(new BalancedRouteStrategy(0.5, 0.3, 0.1, 0.1));
        DeliveryRequest req = new DeliveryRequest("REQ-4", DeliveryPriority.STANDARD, 10000, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertNotNull(decision.selectedRoute());
        assertTrue(decision.explanation().contains("balanced score"));
    }

    @Test
    @DisplayName("5. Fifth Strategy (Shortest Distance) picks the minimal distance route")
    void testShortestDistanceStrategy() {
        RoutePlanner planner = new RoutePlanner(new ShortestDistanceRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-5", DeliveryPriority.STANDARD, 10000, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertEquals("R-SHORT", decision.selectedRoute().id());
        assertEquals(10.0, decision.selectedRoute().distanceKm());
    }

    @Test
    @DisplayName("6. Runtime strategy switching alters selection behavior dynamically")
    void testRuntimeStrategySwitching() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-6", DeliveryPriority.STANDARD, 10000, 60, false, 10);

        RouteDecision d1 = planner.plan(sampleRoutes, req);
        assertEquals("R-FAST", d1.selectedRoute().id());

        planner.setStrategy(new EcoFriendlyRouteStrategy());
        RouteDecision d2 = planner.plan(sampleRoutes, req);
        assertEquals("R-ECO", d2.selectedRoute().id());

        planner.setStrategy(new ShortestDistanceRouteStrategy());
        RouteDecision d3 = planner.plan(sampleRoutes, req);
        assertEquals("R-SHORT", d3.selectedRoute().id());
    }

    @Test
    @DisplayName("7. Automatic StrategySelector maps request priority to corresponding strategy")
    void testStrategySelectorResolution() {
        StrategySelector selector = new StrategySelector();

        DeliveryRequest urgent = new DeliveryRequest("U1", DeliveryPriority.URGENT, 5000, 30, false, 10);
        DeliveryRequest eco = new DeliveryRequest("E1", DeliveryPriority.ECO, 5000, 30, false, 10);
        DeliveryRequest econ = new DeliveryRequest("C1", DeliveryPriority.ECONOMY, 5000, 30, false, 10);

        assertEquals("FastestRouteStrategy", selector.resolveStrategy(urgent).getStrategyName());
        assertEquals("EcoFriendlyRouteStrategy", selector.resolveStrategy(eco).getStrategyName());
        assertEquals("CheapestRouteStrategy", selector.resolveStrategy(econ).getStrategyName());
    }

    @Test
    @DisplayName("8. Hard Constraint: Exclude unavailable routes even if they have best metrics")
    void testUnavailableRouteIsFilteredOut() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-8", DeliveryPriority.URGENT, 10000, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertNotEquals("R-UNAVAIL", decision.selectedRoute().id());
        assertTrue(decision.rejectedReasons().stream().anyMatch(r -> r.contains("R-UNAVAIL rejected")));
    }

    @Test
    @DisplayName("9. Hard Constraint: Budget limit rejection")
    void testBudgetHardConstraint() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-9", DeliveryPriority.URGENT, 3500, 60, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertNotEquals("R-FAST", decision.selectedRoute().id());
        assertTrue(decision.rejectedReasons().stream().anyMatch(r -> r.contains("R-FAST rejected: Cost")));
    }

    @Test
    @DisplayName("10. Hard Constraint: Deadline time limit rejection")
    void testDeadlineHardConstraint() {
        RoutePlanner planner = new RoutePlanner(new CheapestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-10", DeliveryPriority.ECONOMY, 10000, 20, false, 10);

        RouteDecision decision = planner.plan(sampleRoutes, req);
        assertNotEquals("R-CHEAP", decision.selectedRoute().id());
        assertTrue(decision.rejectedReasons().stream().anyMatch(r -> r.contains("R-CHEAP rejected: Time")));
    }

    @Test
    @DisplayName("11. Failure Scenario: No suitable route throws NoSuitableRouteException")
    void testNoSuitableRouteException() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest impossibleReq = new DeliveryRequest("REQ-11", DeliveryPriority.URGENT, 500, 5, false, 0.5);

        assertThrows(NoSuitableRouteException.class, () -> planner.plan(sampleRoutes, impossibleReq));
    }

    @Test
    @DisplayName("12. Failure Scenario: Empty route candidate list throws NoSuitableRouteException")
    void testEmptyRoutesException() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-12", DeliveryPriority.STANDARD, 5000, 30, false, 10);

        assertThrows(NoSuitableRouteException.class, () -> planner.plan(Collections.emptyList(), req));
    }

    @Test
    @DisplayName("13. Failure Scenario: Invalid weights configuration throws InvalidStrategyConfigException")
    void testInvalidBalancedWeights() {
        assertThrows(InvalidStrategyConfigException.class, () ->
                new BalancedRouteStrategy(0.4, 0.4, 0.4, 0.4)
        );

        assertThrows(InvalidStrategyConfigException.class, () ->
                new BalancedRouteStrategy(-0.1, 0.5, 0.3, 0.3)
        );
    }

    @Test
    @DisplayName("14. Comparison mode executes multiple strategies on identical request")
    void testComparisonMode() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        DeliveryRequest req = new DeliveryRequest("REQ-14", DeliveryPriority.STANDARD, 10000, 60, false, 10);

        Map<String, RouteDecision> comparison = planner.compareAll(
                sampleRoutes,
                req,
                List.of(new FastestRouteStrategy(), new EcoFriendlyRouteStrategy())
        );

        assertEquals(2, comparison.size());
        assertEquals("R-FAST", comparison.get("FastestRouteStrategy").selectedRoute().id());
        assertEquals("R-ECO", comparison.get("EcoFriendlyRouteStrategy").selectedRoute().id());
    }

    @Test
    @DisplayName("15. Boundary Case: Route exactly matching budget and deadline is accepted")
    void testExactBoundaryValues() {
        Route boundaryRoute = new Route("R-EXACT", locA, locB, 10.0, 20, 5000.0, 2.0, true, 0.0);
        DeliveryRequest boundaryReq = new DeliveryRequest("REQ-15", DeliveryPriority.STANDARD, 5000.0, 20, false, 2.0);

        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        RouteDecision decision = planner.plan(List.of(boundaryRoute), boundaryReq);

        assertEquals("R-EXACT", decision.selectedRoute().id());
    }

    @Test
    @DisplayName("16. Negative route values throw IllegalArgumentException on construction")
    void testInvalidRouteDomainValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Route("R-BAD", locA, locB, -10.0, 20, 100.0, 1.0, true, 0.0)
        );
    }
}