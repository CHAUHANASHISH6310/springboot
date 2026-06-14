package com.shranvi.api.service;

import com.shranvi.api.model.Product;
import com.shranvi.api.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    // ✅ Fetch ALL products from database
    public List<Product> getAllProducts() {
        logger.info("Fetching all products from database");
        return productRepository.findAll();
    }

    // ✅ Fetch only active products
    public List<Product> getActiveProducts() {
        logger.info("Fetching active products");
        return productRepository.findByIsActiveTrue();
    }

    // ✅ Fetch single product by ID
    public Optional<Product> getProductById(Long id) {
        logger.info("Fetching product with ID: {}", id);
        return productRepository.findById(id);
    }

    // ✅ Fetch products by category
    public List<Product> getProductsByCategory(String category) {
        logger.info("Fetching products for category: {}", category);
        return productRepository.findByCategoryAndIsActiveTrue(category);
    }

    // ✅ Search products by keyword
    public List<Product> searchProducts(String keyword) {
        logger.info("Searching products with keyword: {}", keyword);
        return productRepository.searchByName(keyword);
    }

    // ✅ Get total product count
    public long getProductCount() {
        return productRepository.countByIsActiveTrue();
    }

    // ✅ Save new product
    public Product saveProduct(Product product) {
        logger.info("Saving new product: {}", product.getName());
        return productRepository.save(product);
    }

    // ✅ Update product
    public Optional<Product> updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id).map(existing -> {
            existing.setName(updatedProduct.getName());
            existing.setDescription(updatedProduct.getDescription());
            existing.setPrice(updatedProduct.getPrice());
            existing.setStockQuantity(updatedProduct.getStockQuantity());
            existing.setCategory(updatedProduct.getCategory());
            existing.setImageUrl(updatedProduct.getImageUrl());
            existing.setSku(updatedProduct.getSku());
            existing.setIsActive(updatedProduct.getIsActive());
            return productRepository.save(existing);
        });
    }

    // ✅ Delete product (soft delete)
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id).map(product -> {
            product.setIsActive(false);
            productRepository.save(product);
            logger.info("Soft deleted product ID: {}", id);
            return true;
        }).orElse(false);
    }
}
