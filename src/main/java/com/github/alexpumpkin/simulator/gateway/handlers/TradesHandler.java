package com.github.alexpumpkin.simulator.gateway.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexpumpkin.simulator.trades.TradeBus;
import com.github.alexpumpkin.simulator.trades.model.Trade;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;

/**
 * WebSocket handler, emitting new trades.
 */
public class TradesHandler implements WebSocketHandler {
    private final TradeBus tradeBus;
    private final ObjectMapper objectMapper;

    public TradesHandler(TradeBus tradeBus, ObjectMapper objectMapper) {
        this.tradeBus = tradeBus;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle websocket session to emit new trades
     * @param session the session to handle
     * @return empty Mono
     */
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(tradeBus.trades()
                .map(this::writeValueAsString)
                .map(session::textMessage));
    }

    private String writeValueAsString(Trade trade) {
        try {
            return objectMapper.writeValueAsString(trade);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
