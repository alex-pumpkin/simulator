package com.github.alexpumpkin.simulator.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Simplified model to add new orders
 *
 * @param symbol   order book's symbol (for example GOOG for Google)
 * @param quantity quantity to buy/sell
 * @param price    price to buy/sell
 * @param uuid     universal unique identifier to prevent duplicated requests
 * @see java.util.UUID
 */
public record AddOrderDTO(@NotNull(message = "Symbol must not be null")
                          String symbol,
                          @NotNull(message = "Quantity must not be null")
                          @Positive(message = "Quantity must be positive")
                          Integer quantity,
                          @NotNull(message = "Price must not be null")
                          @Positive(message = "Price must be positive")
                          Integer price,
                          @NotNull(message = "uuid must not be null")
                          String uuid) {
}
