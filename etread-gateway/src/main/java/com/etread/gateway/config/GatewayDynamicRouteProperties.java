package com.etread.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "etread.gateway.dynamic")
public class GatewayDynamicRouteProperties {

    private List<RouteDefinition> routes = new ArrayList<>();

    public List<RouteDefinition> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDefinition> routes) {
        this.routes = routes;
    }
}
