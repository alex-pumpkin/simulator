package com.github.alexpumpkin.simulator.trades.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Trade abstraction
 *
 * @param uuid          universal unique identifier of the trade
 * @param symbol        order book's symbol (for example GOOG for Google)
 * @param price         price of the trade
 * @param quantity      quantity of the trade
 * @param sellOrderUuid selling order uuid
 * @param buyOrderUuid  buying order uuid
 * @param timestamp     timestamp when the trade was executed
 */
public record Trade(String uuid,
                    String symbol,
                    Integer price,
                    Integer quantity,
                    String sellOrderUuid,
                    String buyOrderUuid,
                    Instant timestamp) {

    public Trade(String symbol, Integer price, Integer quantity, String sellUuid, String buyUuid) {
        this(UUID.randomUUID().toString(), symbol, price, quantity, sellUuid, buyUuid, Instant.now());
    }
}
