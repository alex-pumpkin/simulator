package com.github.alexpumpkin.simulator.orders.services;

import com.github.alexpumpkin.simulator.model.Order;
import com.github.alexpumpkin.simulator.orders.model.OrderBook;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support class to store OrderBooks by symbol.
 */
public class OrderBooksHolder {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    /**
     * Add order to the OrderBook for given symbol
     *
     * @param order order to add
     */
    public void add(Order order) {
        orderBooks.compute(order.symbol(), (s, orderBook) -> {
            if (orderBook == null) {
                orderBook = new OrderBook();
            }
            orderBook.add(order);
            return orderBook;
        });
    }

    /**
     * Get all registered symbols.
     *
     * @return set of symbols
     */
    public Set<String> allSymbols() {
        return orderBooks.keySet();
    }

    /**
     * Get OrderBook for given symbol.
     *
     * @param symbol symbol to get the OrderBook
     * @return OrderBook
     */
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }
}
