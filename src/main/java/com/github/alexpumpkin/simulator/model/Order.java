package com.github.alexpumpkin.simulator.model;

import java.time.Instant;

/**
 * Order model.
 *
 * @param uuid       universal unique identifier of the order
 * @param symbol     order book's symbol (for example GOOG for Google)
 * @param quantity   quantity to buy/sell
 * @param price      price to buy/sell
 * @param type       type of the order (buy or sell)
 * @param registered timestamp of the registration
 * @param state      state of the order
 */
public record Order(String uuid,
                    String symbol,
                    int quantity,
                    int price,
                    Type type,
                    Instant registered,
                    State state) {

    /**
     * Create new order with BUY type.
     *
     * @param dto order parameters.
     * @return new order.
     */
    public static Order buy(AddOrderDTO dto) {
        return new Order(dto.uuid(),
                dto.symbol(),
                dto.quantity(),
                dto.price(),
                Type.BUY,
                Instant.now(),
                State.PENDING);
    }

    /**
     * Create new order with SELL type.
     *
     * @param dto order parameters.
     * @return new order.
     */
    public static Order sell(AddOrderDTO dto) {
        return new Order(dto.uuid(),
                dto.symbol(),
                dto.quantity(),
                dto.price(),
                Type.SELL,
                Instant.now(),
                State.PENDING);
    }

    /**
     * Create new order with given state.
     *
     * @param state order state.
     * @return new order.
     */
    public Order withState(State state) {
        return new Order(this.uuid(),
                this.symbol(),
                this.quantity(),
                this.price(),
                this.type,
                this.registered,
                state);
    }

    /**
     * Order type
     */
    public enum Type {
        BUY,
        SELL
    }

    /**
     * Order state
     *
     * @startuml #
     * [*] --> PENDING
     * PENDING --> CANCELED
     * PENDING --> PROCESS_PENDING
     * PROCESS_PENDING --> PENDING
     * PROCESS_PENDING --> PARTIALLY_EXECUTED
     * PROCESS_PENDING --> EXECUTED
     * PARTIALLY_EXECUTED --> PROCESS_PARTIALLY_EXECUTED
     * PROCESS_PARTIALLY_EXECUTED --> PARTIALLY_EXECUTED
     * PROCESS_PARTIALLY_EXECUTED --> EXECUTED
     * PARTIALLY_EXECUTED --> PARTIALLY_CANCELED
     * CANCELED --> [*]
     * PARTIALLY_CANCELED ---> [*]
     * EXECUTED --> [*]
     * @enduml
     */
    public enum State {
        PENDING,
        PROCESS_PENDING,
        PARTIALLY_EXECUTED,
        PROCESS_PARTIALLY_EXECUTED,
        CANCELLED,
        PARTIALLY_CANCELED, //todo: is it legal?
        EXECUTED,
    }
}
