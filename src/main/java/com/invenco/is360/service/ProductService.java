package com.invenco.is360.service;

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
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * Returns all products. Result is cached for 10 minutes
     * (configured via Caffeine spec in application.properties).
     */
    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /**
     * Updates a product and invalidates cache.
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product updateProduct(Long id, Product updated) {
        Product product = findById(id);
        product.setName(updated.getName());
        product.setPrice(updated.getPrice());
        product.setStockQuantity(updated.getStockQuantity());
        return productRepository.save(product);
    }
}
