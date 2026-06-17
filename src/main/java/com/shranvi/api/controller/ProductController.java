package com.shranvi.api.controller;

import com.shranvi.api.model.ApiResponse;
import com.shranvi.api.model.Product;
import com.shranvi.api.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")  // Allow all origins (restrict in production)
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * GET /api/v1/products
     * Fetch ALL products from the products table → JSON
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            ApiResponse<List<Product>> response = ApiResponse.success(
                "Products fetched  ", products
            );
            response.setCount(products.size());
            logger.info("Returned {} products", products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch products: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/products/active
     * Fetch only active products
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Product>>> getActiveProducts() {
        try {
            List<Product> products = productService.getActiveProducts();
            ApiResponse<List<Product>> response = ApiResponse.success(
                "Active products fetched", products
            );
            response.setCount(products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/products/{id}
     * Fetch single product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
            .map(product -> ResponseEntity.ok(ApiResponse.success("Product found", product)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Product not found with ID: " + id)));
    }

    /**
     * GET /api/v1/products/category/{category}
     * Fetch products by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Product>>> getByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        ApiResponse<List<Product>> response = ApiResponse.success("Products by category", products);
        response.setCount(products.size());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/products/search?keyword=saree
     * Search products by name keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Product>>> searchProducts(
            @RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        ApiResponse<List<Product>> response = ApiResponse.success("Search results", products);
        response.setCount(products.size());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/products
     * Create new product
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product) {
        try {
            Product saved = productService.saveProduct(product);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", saved));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create product: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/products/{id}
     * Update existing product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id, @RequestBody Product product) {
        return productService.updateProduct(id, product)
            .map(updated -> ResponseEntity.ok(ApiResponse.success("Product updated", updated)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Product not found with ID: " + id)));
    }

    /**
     * DELETE /api/v1/products/{id}
     * Soft-delete a product
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Product not found with ID: " + id));
    }

    /**
     * GET /api/v1/products/health
     * Health check endpoint for CI/CD pipeline
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        long count = productService.getProductCount();
        return ResponseEntity.ok(
            ApiResponse.success("API is healthy. Active products: " + count, "OK")
        );
    }
}
