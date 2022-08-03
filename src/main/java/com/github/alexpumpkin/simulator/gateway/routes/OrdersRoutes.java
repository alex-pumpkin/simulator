package com.github.alexpumpkin.simulator.gateway.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexpumpkin.simulator.gateway.handlers.OrdersHandler;
import com.github.alexpumpkin.simulator.gateway.handlers.TradesHandler;
import com.github.alexpumpkin.simulator.trades.TradeBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Simulator configuration for RouterFunctions.
 */
@Configuration
@Slf4j
public class OrdersRoutes {

    @Bean
    RouterFunction<ServerResponse> mainRouterFunction(OrdersHandler ordersHandler) {
        return route()
                .POST("/orders/buy", contentType(MediaType.APPLICATION_JSON), ordersHandler::buy)
                .POST("/orders/sell", contentType(MediaType.APPLICATION_JSON), ordersHandler::sell)
                .DELETE("/orders/{uuid}", ordersHandler::cancel)
                .build();
    }

    @Bean
    HandlerMapping handlerMapping(TradeBus tradeBus, ObjectMapper objectMapper) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/trades", new TradesHandler(tradeBus, objectMapper));
        return new SimpleUrlHandlerMapping(map, -1);
    }

    @Bean
    @Profile("test")
    RouterFunction<ServerResponse> additionalRouterFunction(OrdersHandler ordersHandler) {
        return route()
                .GET("/orders/{uuid}", ordersHandler::getByUuid)
                .build();
    }
}
