package com.github.alexpumpkin.simulator.orders.model;

import com.github.alexpumpkin.simulator.model.Order;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Order wrapper for the in-memory storage. In real life we would use external database, and we wouldn't need this
 * wrapper and AtomicReference. Most likely we would use the optimistic locking on the database.
 *
 * @param order original order
 * @param state current order state, wrapped in the {@link AtomicReference}. We should always use this state instead of
 *              the order.state()
 */
public record OrderStorageEntity(Order order, AtomicReference<Order.State> state) {

    /**
     * Create new entity for given order.
     *
     * @param order order to wrap
     * @return new entity
     */
    public static OrderStorageEntity of(Order order) {
        return new OrderStorageEntity(order, new AtomicReference<>(order.state()));
    }

    /**
     * Create new order with actual state
     *
     * @return new order with actual state
     */
    public Order orderWithCurrentState() {
        return order.withState(state.get());
    }
}
