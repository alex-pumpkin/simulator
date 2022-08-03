package com.github.alexpumpkin.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexpumpkin.simulator.model.AddOrderDTO;
import com.github.alexpumpkin.simulator.trades.model.Trade;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class SimulatorApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testBuyOrder() {
        String uuid = "id_buy";
        webTestClient.post()
                .uri("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("A", 100, 25, uuid))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("location", Matchers.containsString(uuid))
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("BUY");
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/{uuid}").build(uuid))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("BUY")
                .jsonPath("state").isEqualTo("PENDING");
    }

    @Test
    void testSellOrder() {
        String uuid = "id_sell";
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("B", 100, 25, uuid))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("location", Matchers.containsString(uuid))
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("SELL");
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/{uuid}").build(uuid))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("SELL")
                .jsonPath("state").isEqualTo("PENDING");
    }

    @Test
    void testDeleteOrder() {
        String uuid = "id_delete";
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("C", 100, 25, uuid))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("location", Matchers.containsString(uuid))
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("SELL");
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/{uuid}").build(uuid))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("SELL")
                .jsonPath("state").isEqualTo("PENDING");
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/orders/{uuid}").build(uuid))
                .exchange()
                .expectStatus().isOk();
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/{uuid}").build(uuid))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("uuid").isEqualTo(uuid)
                .jsonPath("type").isEqualTo("SELL")
                .jsonPath("state").isEqualTo("CANCELLED");
    }

    @Test
    void testMatching() throws URISyntaxException {
        List<Trade> trades = new ArrayList<>();
        WebSocketClient client = new ReactorNettyWebSocketClient();

        URI url = new URI("ws://localhost:%s/trades".formatted(localPort));
        client.execute(url, session ->
                        session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .map(s -> {
                                    try {
                                        return objectMapper.readValue(s, Trade.class);
                                    } catch (JsonProcessingException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                                .doOnNext(trades::add)
                                .then())
                .subscribe();

        webTestClient.post()
                .uri("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("D", 10, 25, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("D", 20, 26, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("D", 50, 27, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("D", 15, 27, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("E", 100, 2134, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("E", 120, 2100, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();
        webTestClient.post()
                .uri("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddOrderDTO("E", 87, 2100, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isCreated();

        Mono.delay(Duration.ofSeconds(2)).block();
        assertEquals(2, trades.stream().filter(trade -> Objects.equals("D", trade.symbol())).count());
        assertEquals(1, trades.stream().filter(trade -> Objects.equals("E", trade.symbol())).count());
    }
}
