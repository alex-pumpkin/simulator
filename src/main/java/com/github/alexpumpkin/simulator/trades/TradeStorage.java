package com.github.alexpumpkin.simulator.trades;

import com.github.alexpumpkin.simulator.trades.model.Trade;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The trade storage.
 */
@Slf4j
public class TradeStorage {

    private final Map<String, Trade> trades;

    public TradeStorage(TradeBus tradeBus) {
        trades = new ConcurrentHashMap<>();
        tradeBus.trades()
                .doOnNext(this::add)
                .subscribeOn(Schedulers.parallel())
                .subscribe();
    }

    private void add(Trade trade) {
        log.debug("Trade added: {}", trade);
        trades.putIfAbsent(trade.uuid(), trade);
    }
}
