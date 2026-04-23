package com.etread.gateway.route;

import com.etread.gateway.config.GatewayDynamicRouteProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DynamicRouteDefinitionLocator implements RouteDefinitionLocator {

    private final GatewayDynamicRouteProperties routeProperties;

    public DynamicRouteDefinitionLocator(GatewayDynamicRouteProperties routeProperties) {
        this.routeProperties = routeProperties;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeProperties.getRoutes());
    }
}
