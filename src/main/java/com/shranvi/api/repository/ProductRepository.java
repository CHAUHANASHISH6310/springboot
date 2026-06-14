package com.shranvi.api.repository;

import com.shranvi.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetch all active products
    List<Product> findByIsActiveTrue();

    // Find by category
    List<Product> findByCategoryAndIsActiveTrue(String category);

    // Find by SKU
    Optional<Product> findBySku(String sku);

    // Search by name (case-insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    List<Product> searchByName(String keyword);

    // Count active products
    long countByIsActiveTrue();
}
