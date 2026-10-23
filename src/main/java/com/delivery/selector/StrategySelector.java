package com.delivery.selector;

import com.delivery.model.DeliveryPriority;
import com.delivery.model.DeliveryRequest;
import com.delivery.strategy.RouteStrategy;
import com.delivery.strategy.impl.BalancedRouteStrategy;
import com.delivery.strategy.impl.CheapestRouteStrategy;
import com.delivery.strategy.impl.EcoFriendlyRouteStrategy;
import com.delivery.strategy.impl.FastestRouteStrategy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class StrategySelector {

    private final Map<DeliveryPriority, RouteStrategy> strategyMap;
    private final RouteStrategy fallbackStrategy;

    public StrategySelector() {
        this.strategyMap = new EnumMap<>(DeliveryPriority.class);
        this.fallbackStrategy = new BalancedRouteStrategy();

        // Default extensible mappings
        registerStrategy(DeliveryPriority.URGENT, new FastestRouteStrategy());
        registerStrategy(DeliveryPriority.ECONOMY, new CheapestRouteStrategy());
        registerStrategy(DeliveryPriority.ECO, new EcoFriendlyRouteStrategy());
        registerStrategy(DeliveryPriority.STANDARD, this.fallbackStrategy);
    }

    public void registerStrategy(DeliveryPriority priority, RouteStrategy strategy) {
        strategyMap.put(priority, Objects.requireNonNull(strategy));
    }

    public RouteStrategy resolveStrategy(DeliveryRequest request) {
        if (request == null) {
            return fallbackStrategy;
        }

        // Context-aware dynamic rule: if ecoPreferred is marked, prioritize Eco unless URGENT
        if (request.ecoPreferred() && request.priority() != DeliveryPriority.URGENT) {
            return strategyMap.getOrDefault(DeliveryPriority.ECO, fallbackStrategy);
        }

        return strategyMap.getOrDefault(request.priority(), fallbackStrategy);
    }
}