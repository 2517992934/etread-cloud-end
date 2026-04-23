package com.etread.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GatewayRouteRefreshListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRouteRefreshListener.class);

    private final ApplicationEventPublisher eventPublisher;

    public GatewayRouteRefreshListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener({RefreshScopeRefreshedEvent.class, EnvironmentChangeEvent.class})
    public void refreshRoutes() {
        LOGGER.info("Gateway route configuration changed, refreshing route cache");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
