package com.github.alexpumpkin.simulator.gateway.configuration;

import com.github.alexpumpkin.simulator.gateway.handlers.OrdersHandler;
import com.github.alexpumpkin.simulator.matching.MatchingEngine;
import com.github.alexpumpkin.simulator.orders.api.OrdersService;
import com.github.alexpumpkin.simulator.orders.services.OrderBooksHolder;
import com.github.alexpumpkin.simulator.orders.services.OrdersServiceImpl;
import com.github.alexpumpkin.simulator.trades.TradeBus;
import com.github.alexpumpkin.simulator.trades.TradeStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

/**
 * Simulator configuration for logic beans
 */
@Configuration
public class SimulatorConfiguration {

    @Bean
    OrderBooksHolder orderBooksHolder() {
        return new OrderBooksHolder();
    }

    @Bean
    OrdersService ordersService(OrderBooksHolder orderBooksHolder) {
        return new OrdersServiceImpl(orderBooksHolder);
    }


    @Bean
    OrdersHandler ordersHandler(Validator validator, OrdersService ordersService) {
        return new OrdersHandler(validator, ordersService);
    }

    @Bean
    TradeBus tradeBus() {
        return new TradeBus();
    }

    @Bean
    MatchingEngine matchingEngine(OrdersService ordersService, OrderBooksHolder orderBooksHolder, TradeBus tradeBus) {
        return new MatchingEngine(ordersService, orderBooksHolder, tradeBus);
    }

    @Bean
    TradeStorage tradeStorage(TradeBus tradeBus) {
        return new TradeStorage(tradeBus);
    }
}
