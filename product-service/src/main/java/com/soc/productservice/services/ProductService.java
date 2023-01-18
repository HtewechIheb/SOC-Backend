package com.soc.productservice.services;

import com.soc.productservice.dto.ProductRequestDto;
import com.soc.productservice.dto.ProductResponseDto;
import com.soc.productservice.models.Product;
import com.soc.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToProductResponseDto).toList();
    }

    public ProductResponseDto getProductById(String id) {
        return productRepository.findById(id).map(this::mapToProductResponseDto).orElseThrow();
    }

    public void createProduct(ProductRequestDto productRequestDto) {
        Product product = Product.builder()
                .code(productRequestDto.getCode())
                .name(productRequestDto.getName())
                .description(productRequestDto.getDescription())
                .price(productRequestDto.getPrice())
                .build();

        productRepository.save(product);
    }

    public void updateProduct(ProductRequestDto productRequestDto, String id){
        Product productToUpdate = productRepository.findById(id).orElseThrow();
        productToUpdate.setCode(productRequestDto.getCode());
        productToUpdate.setName(productRequestDto.getName());
        productToUpdate.setDescription(productRequestDto.getDescription());
        productToUpdate.setPrice(productRequestDto.getPrice());

        productRepository.save(productToUpdate);
    }

    public void deleteProduct(String id){
        Product productToDelete = productRepository.findById(id).orElseThrow();
        productRepository.delete(productToDelete);
    }

    private ProductResponseDto mapToProductResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
}
