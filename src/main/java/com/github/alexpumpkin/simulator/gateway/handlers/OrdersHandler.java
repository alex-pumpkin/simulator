package com.github.alexpumpkin.simulator.gateway.handlers;

import com.github.alexpumpkin.simulator.model.AddOrderDTO;
import com.github.alexpumpkin.simulator.model.Order;
import com.github.alexpumpkin.simulator.orders.api.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

/**
 * Web handlers.
 */
@Slf4j
public class OrdersHandler {

    private final Validator validator;
    private final OrdersService ordersService;

    public OrdersHandler(Validator validator, OrdersService ordersService) {
        this.validator = validator;
        this.ordersService = ordersService;
    }

    /**
     * Handle "buy" request.
     *
     * @param request "buy" request to handle.
     * @return result of the "buy" request processing
     */
    public Mono<ServerResponse> buy(ServerRequest request) {
        return addOrder(request, Order::buy);
    }

    /**
     * Handle "sell" request.
     *
     * @param request "sell" request to handle.
     * @return result of the "sell" request processing
     */
    public Mono<ServerResponse> sell(ServerRequest request) {
        return addOrder(request, Order::sell);
    }

    /**
     * Handle "cancel" request.
     *
     * @param request "cancel" request to handle.
     * @return result of the "cancel" request processing
     */
    public Mono<ServerResponse> cancel(ServerRequest request) {
        return ordersService.cancel(request.pathVariable("uuid"))
                .then(ServerResponse.ok().build())
                .transform(this::errorHandling);
    }

    /**
     * Handle getByUuid request
     *
     * @param request getByUuid request to handle
     * @return result of the "getByUuid" request processing
     */
    public Mono<ServerResponse> getByUuid(ServerRequest request) {
        return ordersService.getOrderByUUID(request.pathVariable("uuid"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(order -> ServerResponse.ok().bodyValue(order))
                .transform(this::errorHandling);
    }

    private Mono<ServerResponse> addOrder(ServerRequest request, Function<AddOrderDTO, Order> mapper) {
        return request.bodyToMono(AddOrderDTO.class)
                .map(this::validate)
                .map(mapper)
                .flatMap(ordersService::add)
                .flatMap(order -> createdResponse(request, order))
                .transform(this::errorHandling);
    }

    private AddOrderDTO validate(AddOrderDTO dto) {
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "AddOrderDTO");
        validator.validate(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            //noinspection ConstantConditions
            throw new WebExchangeBindException(null, bindingResult);
        }
        return dto;
    }


    private static Mono<ServerResponse> createdResponse(ServerRequest request, Order order) {
        return ServerResponse.created(request.uriBuilder()
                        .replacePath("/orders/{id}")
                        .build(order.uuid()))
                .bodyValue(order);
    }

    private Mono<ServerResponse> errorHandling(Mono<ServerResponse> serverResponseMono) {
        return serverResponseMono
                .onErrorResume(WebExchangeBindException.class, e -> ServerResponse.badRequest()
                        .bodyValue(e.getBindingResult()
                                .getAllErrors()
                                .stream()
                                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                .filter(Objects::nonNull)
                                .toList()))
                .doOnError(throwable -> log.error("Generic error: ", throwable));
    }
}
