package com.soc.productservice.controllers;

import com.soc.productservice.dto.ProductRequestDto;
import com.soc.productservice.dto.ProductResponseDto;
import com.soc.productservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponseDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDto getProductById(@PathVariable("id") String id){
        try{
            return productService.getProductById(id);
        }
        catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID %s not found.".formatted(id));
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody ProductRequestDto productRequestDto, HttpServletRequest httpRequest) {
        productService.createProduct(productRequestDto);
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProduct(@PathVariable("id") String id, @RequestBody ProductRequestDto productRequestDto) {
        try{
            productService.updateProduct(productRequestDto, id);
        }
        catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID %s not found.".formatted(id));
        }
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProduct(@PathVariable("id") String id){
        try{
            productService.deleteProduct(id);
        }
        catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID %s not found.".formatted(id));
        }
    }
}
