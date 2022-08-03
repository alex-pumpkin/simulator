package com.github.alexpumpkin.simulator.orders.services;

import com.github.alexpumpkin.simulator.model.Order;
import com.github.alexpumpkin.simulator.orders.api.OrdersService;
import com.github.alexpumpkin.simulator.orders.model.OrderStorageEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link OrdersService} in memory implementation.
 */
@Slf4j
public class OrdersServiceImpl implements OrdersService {

    private static final String CANCEL_MESSAGE = "Impossible to cancel order. Current state is {}";
    private final Map<String, OrderStorageEntity> orderStorage = new ConcurrentHashMap<>();
    private final OrderBooksHolder orderBooksHolder;


    public OrdersServiceImpl(OrderBooksHolder orderBooksHolder) {
        this.orderBooksHolder = orderBooksHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Order> add(Order order) {
        OrderStorageEntity current = orderStorage.putIfAbsent(order.uuid(), OrderStorageEntity.of(order));
        if (current == null) {
            log.debug("Order added: {}", order);
            orderBooksHolder.add(order);
            return Mono.just(order);
        } else {
            log.debug("Duplicate request for uuid: {}. Return current: {}", order.uuid(), current.order());
            return Mono.just(current.orderWithCurrentState());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> cancel(String uuid) {
        Objects.requireNonNull(uuid);
        OrderStorageEntity current = orderStorage.get(uuid);
        if (current == null) {
            log.debug("Order is not found by uuid = {}. Pretend we successfully deleted.", uuid);
            return Mono.empty();
        }

        Order.State currentState = current.state().get();
        return switch (currentState) {
            case CANCELLED, PARTIALLY_CANCELED -> {
                log.debug("Order already cancelled. UUID = {}", uuid);
                yield Mono.empty();
            }
            case PENDING, PARTIALLY_EXECUTED -> {
                log.debug("Cancel order: {}", current.orderWithCurrentState());
                if (current.state().compareAndSet(Order.State.PENDING, Order.State.CANCELLED)) {
                    yield Mono.empty();
                } else {
                    if (current.state().compareAndSet(Order.State.PARTIALLY_EXECUTED, Order.State.PARTIALLY_CANCELED)) {
                        yield Mono.empty();
                    } else {
                        log.debug(CANCEL_MESSAGE, current.state().get());
                        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Cannot cancel");
                    }
                }
            }
            case PROCESS_PENDING, PROCESS_PARTIALLY_EXECUTED -> {
                log.debug(CANCEL_MESSAGE, currentState);
                throw new ResponseStatusException(HttpStatus.LOCKED, "Cannot cancel order in %s state"
                        .formatted(currentState));
            }
            default -> {
                log.debug(CANCEL_MESSAGE, currentState);
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Cannot cancel order in %s state"
                        .formatted(currentState));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Order> getOrderByUUID(String uuid) {
        return Mono.justOrEmpty(orderStorage.get(uuid))
                .map(OrderStorageEntity::orderWithCurrentState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple2<Order.State, Order.State> lockToProcess(String sellUUID, String buyUUID) {
        OrderStorageEntity sellOrder = orderStorage.get(sellUUID);
        AtomicReference<Order.State> sellStateRef = sellOrder.state();
        OrderStorageEntity buyOrder = orderStorage.get(buyUUID);
        AtomicReference<Order.State> buyStateRef = buyOrder.state();
        Order.State sellState = sellStateRef.get();
        Order.State buyState = buyStateRef.get();

        boolean sellPrepared = false;
        boolean buyPrepared = false;

        if (sellState == Order.State.PENDING) {
            sellPrepared = sellStateRef.compareAndSet(Order.State.PENDING,
                    Order.State.PROCESS_PENDING);
        } else if (sellState == Order.State.PARTIALLY_EXECUTED) {
            sellPrepared = sellStateRef.compareAndSet(Order.State.PARTIALLY_EXECUTED,
                    Order.State.PROCESS_PARTIALLY_EXECUTED);
        }
        if (buyState == Order.State.PENDING) {
            buyPrepared = buyStateRef.compareAndSet(Order.State.PENDING,
                    Order.State.PROCESS_PENDING);
        } else if (buyState == Order.State.PARTIALLY_EXECUTED) {
            buyPrepared = buyStateRef.compareAndSet(Order.State.PARTIALLY_EXECUTED,
                    Order.State.PROCESS_PARTIALLY_EXECUTED);
        }

        // rollback if we couldn't prepare both
        if (sellPrepared ^ buyPrepared) {
            sellStateRef.compareAndSet(Order.State.PROCESS_PENDING,
                    Order.State.PENDING);
            sellStateRef.compareAndSet(Order.State.PROCESS_PARTIALLY_EXECUTED,
                    Order.State.PARTIALLY_EXECUTED);
            buyStateRef.compareAndSet(Order.State.PROCESS_PENDING,
                    Order.State.PENDING);
            buyStateRef.compareAndSet(Order.State.PROCESS_PARTIALLY_EXECUTED,
                    Order.State.PARTIALLY_EXECUTED);
            log.debug("Orders locking failed: sellOrder=[{}], buyOrder=[{}]",
                    sellOrder.orderWithCurrentState(), buyOrder.orderWithCurrentState());
        } else {
            log.debug("Orders are locked successfully: sellOrder=[{}], buyOrder=[{}]",
                    sellOrder.orderWithCurrentState(), buyOrder.orderWithCurrentState());
        }

        return Tuples.of(sellStateRef.get(), buyStateRef.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockProcessed(String uuid, Order.State state) {
        OrderStorageEntity order = orderStorage.get(uuid);
        AtomicReference<Order.State> currentStateRef = order.state();
        currentStateRef.set(state);
        log.debug("Order unlocked: {}", order.orderWithCurrentState());
    }
}
