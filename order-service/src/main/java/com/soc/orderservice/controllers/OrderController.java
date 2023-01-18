package com.soc.orderservice.controllers;

import com.soc.orderservice.dto.OrderRequestDto;
import com.soc.orderservice.services.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AsyncTaskExecutor executor;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory")
    public CompletableFuture<ResponseEntity<?>> placeOrder(@RequestBody OrderRequestDto orderRequestDto){
        return CompletableFuture.supplyAsync(() -> {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            try {
                orderService.placeOrder(orderRequestDto, jwt);
            }
            catch (WebClientResponseException ex) {
                return switch (ex.getStatusCode()) {
                    case BAD_REQUEST -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid operation.");
                    case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Some of the items do not exist.");
                    default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Some went wrong! Please try again.");
                };
            }

            return ResponseEntity.ok("Order Placed Successfully.");
        }, executor);
    }

    @DeleteMapping("{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory")
    public CompletableFuture<ResponseEntity<?>> cancelOrder(@PathVariable("orderNumber") String orderNumber){
        return CompletableFuture.supplyAsync(() -> {
            Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            try {
                orderService.cancelOrder(orderNumber, jwt);
            }
            catch (NoSuchElementException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with number %s does not exist.".formatted(ex.getMessage()));
            }
            catch (WebClientResponseException ex) {
                return switch (ex.getStatusCode()) {
                    case BAD_REQUEST -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid operation.");
                    case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Some of the items do not exist.");
                    default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Some went wrong! Please try again.");
                };
            }

            return ResponseEntity.ok("Order Canceled Successfully.");
        }, executor);
    }

    public CompletableFuture<ResponseEntity<?>> fallbackMethod(RuntimeException runtimeException){
        return CompletableFuture.supplyAsync(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong! Please try again."));
    }
}
