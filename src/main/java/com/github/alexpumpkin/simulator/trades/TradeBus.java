package com.github.alexpumpkin.simulator.trades;

import com.github.alexpumpkin.simulator.trades.model.Trade;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Utility bus to propagate new trades.
 */
public class TradeBus implements Consumer<Trade> {

    private final Sinks.Many<Trade> trades;

    public TradeBus() {
        trades = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public void accept(Trade trade) {
        trades.emitNext(trade, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
    }

    public Flux<Trade> trades() {
        return trades.asFlux();
    }
}
