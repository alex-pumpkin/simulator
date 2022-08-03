package com.github.alexpumpkin.simulator.matching;

import com.github.alexpumpkin.simulator.model.Order;
import com.github.alexpumpkin.simulator.orders.api.OrdersService;
import com.github.alexpumpkin.simulator.orders.model.OrderBook;
import com.github.alexpumpkin.simulator.orders.services.OrderBooksHolder;
import com.github.alexpumpkin.simulator.trades.TradeBus;
import com.github.alexpumpkin.simulator.trades.model.Trade;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Matching engine to process order books
 */
@Slf4j
public class MatchingEngine {

    private static final Set<Order.State> PROCESS_STATES = EnumSet.of(
            Order.State.PROCESS_PENDING, Order.State.PROCESS_PARTIALLY_EXECUTED);

    private final OrdersService ordersService;
    private final TradeBus tradeBus;

    public MatchingEngine(OrdersService ordersService, OrderBooksHolder orderBooksHolder, TradeBus tradeBus) {
        this.ordersService = ordersService;
        this.tradeBus = tradeBus;
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(ignored -> Flux.fromIterable(orderBooksHolder.allSymbols()))
                .parallel().runOn(Schedulers.parallel())
                .doOnNext(symbol -> match(symbol, orderBooksHolder.getOrderBook(symbol)))
                .subscribe();
    }

    private void match(String symbol, OrderBook orderBook) {
        Iterator<Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>>> sellIterator =
                orderBook.sellOrdersByPrice();

        while (sellIterator.hasNext()) {
            Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>> sellPriceEntry =
                    sellIterator.next();
            Integer sellPrice = sellPriceEntry.getKey();
            ConcurrentNavigableMap<Instant, Tuple2<String, Integer>> sellOrders = sellPriceEntry.getValue();
            Iterator<Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>>> buyIterator =
                    orderBook.buyOrdersByPrice();
            while (buyIterator.hasNext() && !sellOrders.isEmpty()) {
                Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>> buyPriceEntry =
                        buyIterator.next();
                Integer buyPrice = buyPriceEntry.getKey();
                ConcurrentNavigableMap<Instant, Tuple2<String, Integer>> buyOrders = buyPriceEntry.getValue();
                if (buyPrice < sellPrice) {
                    return;
                }
                match(symbol, sellPrice, sellOrders, buyOrders);
            }
        }
    }

    private void match(String symbol, Integer sellPrice,
                       ConcurrentNavigableMap<Instant, Tuple2<String, Integer>> sellOrders,
                       ConcurrentNavigableMap<Instant, Tuple2<String, Integer>> buyOrders) {
        Iterator<Map.Entry<Instant, Tuple2<String, Integer>>> sellIterator = sellOrders.entrySet().iterator();
        while (sellIterator.hasNext()) {
            Map.Entry<Instant, Tuple2<String, Integer>> sell = sellIterator.next();
            Instant sellInstant = sell.getKey();
            String sellUUID = sell.getValue().getT1();
            int sellQuantity = sell.getValue().getT2();
            Iterator<Map.Entry<Instant, Tuple2<String, Integer>>> buyIterator = buyOrders.entrySet().iterator();
            while (buyIterator.hasNext()) {
                Map.Entry<Instant, Tuple2<String, Integer>> buy = buyIterator.next();
                Instant buyInstant = buy.getKey();
                String buyUUID = buy.getValue().getT1();
                int buyQuantity = buy.getValue().getT2();

                Tuple2<Integer, Integer> matchResult = match(symbol, sellPrice, sellUUID, sellQuantity, buyUUID, buyQuantity);
                // at least one of the elements must be 0
                sellQuantity = matchResult.getT1();
                buyQuantity = matchResult.getT2();
                if (sellQuantity == 0) {
                    if (buyQuantity != 0) {
                        buyOrders.put(buyInstant, Tuples.of(buyUUID, buyQuantity));
                    } else {
                        buyIterator.remove();
                    }
                    sellIterator.remove();
                    break;
                }
                buyIterator.remove();
            }

            if (sellQuantity != 0) {
                sellOrders.put(sellInstant, Tuples.of(sellUUID, sellQuantity));
            }
        }
    }

    private Tuple2<Integer, Integer> match(String symbol, Integer sellPrice,
                                           String sellUUID, int sellQuantity,
                                           String buyUUID, int buyQuantity) {

        log.debug("Match symbol={}, sellPrice={}, sellQuantity={}, buyQuantity={}",
                symbol, sellPrice, sellQuantity, buyQuantity);
        Tuple2<Order.State, Order.State> lockResult = ordersService.lockToProcess(sellUUID, buyUUID);
        if (PROCESS_STATES.contains(lockResult.getT1()) && PROCESS_STATES.contains(lockResult.getT2())) {
            if (sellQuantity > buyQuantity) {
                int newSellQuantity = sellQuantity - buyQuantity;
                ordersService.unlockProcessed(sellUUID, Order.State.PARTIALLY_EXECUTED);
                ordersService.unlockProcessed(buyUUID, Order.State.EXECUTED);
                tradeBus.accept(new Trade(symbol, sellPrice, buyQuantity, sellUUID, buyUUID));
                return Tuples.of(newSellQuantity, 0);
            } else {
                int newBuyQuantity = buyQuantity - sellQuantity;
                ordersService.unlockProcessed(sellUUID, Order.State.EXECUTED);
                ordersService.unlockProcessed(buyUUID, newBuyQuantity > 0 ?
                        Order.State.PARTIALLY_EXECUTED : Order.State.EXECUTED);
                tradeBus.accept(new Trade(symbol, sellPrice, sellQuantity, sellUUID, buyUUID));
                return Tuples.of(0, newBuyQuantity);
            }
        } else if (PROCESS_STATES.contains(lockResult.getT1())) {
            log.warn("Buy order already processed. uuid={}", buyUUID);
            return Tuples.of(sellQuantity, 0);
        } else if (PROCESS_STATES.contains(lockResult.getT2())) {
            log.warn("Sell order already processed. uuid={}", sellUUID);
            return Tuples.of(0, buyQuantity);
        } else {
            log.warn("Orders already processed. sellUUID={}, buyUUID={}", sellUUID, buyUUID);
            return Tuples.of(0, 0);
        }
    }
}
