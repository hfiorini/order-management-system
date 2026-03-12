package com.invenco.is360.service;

import com.invenco.is360.dto.ProductRequest;
import com.invenco.is360.dto.ProductResponse;
import com.invenco.is360.entity.Product;
import com.invenco.is360.exception.ResourceNotFoundException;
import com.invenco.is360.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Creates a new product and invalidates the product list cache.
     */
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Product entity = new Product(request.getName(), request.getPrice(), request.getStockQuantity());
        Product saved = productRepository.save(entity);
        return new ProductResponse(saved);
    }

    /**
     * Returns all products. Result is cached for 10 minutes
     * (configured via Caffeine spec in application.properties).
     */
    @Cacheable(value = "products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::new)
                .toList();
    }

    /**
     * Internal lookup — returns entity for use by other services (e.g. OrderService).
     */
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /**
     * Updates a product and invalidates cache.
     */
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        Product saved = productRepository.save(product);
        return new ProductResponse(saved);
    }
}
