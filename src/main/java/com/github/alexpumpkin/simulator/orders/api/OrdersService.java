package com.github.alexpumpkin.simulator.orders.api;

import com.github.alexpumpkin.simulator.model.Order;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Service which works with orders storage.
 */
public interface OrdersService {

    /**
     * Add new order to the storage.
     *
     * @param order new order.
     * @return added order or currently stored order if the order with given UUID already existed.
     */
    Mono<Order> add(Order order);

    /**
     * Cancel order by UUID
     *
     * @param uuid universal unique identifier of the order to cancel.
     * @return empty Mono or error Mono if the order is not in the one of "cancelable" states
     */
    Mono<Void> cancel(String uuid);

    /**
     * Get order by UUID
     *
     * @param uuid universal unique identifier of the order
     * @return found order or empty Mono
     */
    Mono<Order> getOrderByUUID(String uuid);

    /**
     * Atomically change the state of given orders to PROCESS_PENDING or PROCESS_PARTIALLY_EXECUTED to prevent
     * concurrent cancellation
     *
     * @param sellUUID UUID of the selling order
     * @param buyUUID  UUID of the buying order
     * @return pair with current state of orders
     */
    Tuple2<Order.State, Order.State> lockToProcess(String sellUUID, String buyUUID);

    /**
     * Change the state of given order to EXECUTED or PARTIALLY_EXECUTED (depends on given params).
     * Should be called only after lockToProcess
     *
     * @param uuid  UUID of the order
     * @param state new State (should be EXECUTED or PARTIALLY_EXECUTED)
     */
    void unlockProcessed(String uuid, Order.State state);
}
