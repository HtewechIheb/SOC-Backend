package com.soc.orderservice.services;

import com.soc.orderservice.dto.InventoryResponseDto;
import com.soc.orderservice.dto.InventoryUpdateRequestDto;
import com.soc.orderservice.dto.OrderLineItemsDto;
import com.soc.orderservice.dto.OrderRequestDto;
import com.soc.orderservice.events.OrderPlacedEvent;
import com.soc.orderservice.models.Order;
import com.soc.orderservice.models.OrderLineItems;
import com.soc.orderservice.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequestDto orderRequestDto, Jwt jwt){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItemsList = orderRequestDto
                .getOrderLineItemsList()
                .stream()
                .map(this::mapToOrderLineItems)
                .toList();

        order.setOrderLineItemsList(orderLineItemsList);

        List<String> codes = order.getOrderLineItemsList().stream().map(OrderLineItems::getCode).toList();

        InventoryResponseDto[] inventoryResponseArray =  webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("code", codes).build())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwt.getTokenValue()))
                .retrieve()
                .bodyToMono(InventoryResponseDto[].class)
                .block();

        boolean productsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponseDto::isInStock);

        List<InventoryUpdateRequestDto> inventoryUpdateRequestDtoList = orderLineItemsList.stream().map(orderLineItems -> InventoryUpdateRequestDto
                    .builder()
                    .code(orderLineItems.getCode())
                    .quantity(orderLineItems.getQuantity())
                    .build())
                .toList();

        if(productsInStock){
            webClientBuilder.build().post()
                            .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("operation", "reduce").build())
                                    .bodyValue(inventoryUpdateRequestDtoList)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwt.getTokenValue()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
        }
        else {
            throw new IllegalArgumentException("Some products are out of stock.");
        }
    }

    private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setCode(orderLineItemsDto.getCode());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());

        return orderLineItems;
    }

    public void cancelOrder(String orderNumber, Jwt jwt) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NoSuchElementException(orderNumber));

        List<InventoryUpdateRequestDto> inventoryUpdateRequestDtoList = order.getOrderLineItemsList().stream().map(orderLineItems -> InventoryUpdateRequestDto
                        .builder()
                        .code(orderLineItems.getCode())
                        .quantity(orderLineItems.getQuantity())
                        .build())
                .toList();

        webClientBuilder.build().post()
                .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("operation", "add").build())
                .bodyValue(inventoryUpdateRequestDtoList)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwt.getTokenValue()))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        orderRepository.deleteById(order.getId());
        kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
    }
}
