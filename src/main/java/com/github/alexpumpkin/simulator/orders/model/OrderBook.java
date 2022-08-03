package com.github.alexpumpkin.simulator.orders.model;

import com.github.alexpumpkin.simulator.model.Order;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Order book abstraction to store intermediate information about orders sorted by price and registered timestamp.
 * <br/>
 * price -> registered -> [orderUUID, current_quantity]. current_quantity could be reduced by matching engine.
 */
public class OrderBook {
    private final ConcurrentNavigableMap<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>> sellOrders;
    private final ConcurrentNavigableMap<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>> buyOrders;

    public OrderBook() {
        sellOrders = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
        buyOrders = new ConcurrentSkipListMap<>(Comparator.<Integer>naturalOrder().reversed());
    }

    /**
     * Add new order.
     *
     * @param order new order.
     */
    public void add(Order order) {
        if (order.type() == Order.Type.BUY) {
            buy(order);
        } else {
            sell(order);
        }
    }

    /**
     * Get iterator fo orders to sell
     *
     * @return iterator
     */
    public Iterator<Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>>> sellOrdersByPrice() {
        return sellOrders.entrySet().iterator();
    }

    /**
     * Get iterator fo orders to buy
     *
     * @return iterator
     */
    public Iterator<Map.Entry<Integer, ConcurrentNavigableMap<Instant, Tuple2<String, Integer>>>> buyOrdersByPrice() {
        return buyOrders.entrySet().iterator();
    }

    private void buy(Order order) {
        buyOrders.compute(order.price(), (integer, instantOrderMap) -> {
            if (instantOrderMap == null) {
                instantOrderMap = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
            }
            instantOrderMap.put(order.registered(), Tuples.of(order.uuid(), order.quantity()));
            return instantOrderMap;
        });
    }

    private void sell(Order order) {
        sellOrders.compute(order.price(), (integer, instantOrderMap) -> {
            if (instantOrderMap == null) {
                instantOrderMap = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
            }

            instantOrderMap.put(order.registered(), Tuples.of(order.uuid(), order.quantity()));
            return instantOrderMap;
        });
    }
}
