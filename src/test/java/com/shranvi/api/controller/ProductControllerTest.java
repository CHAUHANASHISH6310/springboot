package com.shranvi.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shranvi.api.model.Product;
import com.shranvi.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Kanjivaram Saree");
        sampleProduct.setPrice(new BigDecimal("7500.00"));
        sampleProduct.setCategory("Saree");
        sampleProduct.setIsActive(true);
        sampleProduct.setStockQuantity(30);
    }

    @Test
    void getAllProducts_ShouldReturn200WithProductList() throws Exception {
        // Arrange
        when(productService.getAllProducts()).thenReturn(Arrays.asList(sampleProduct));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Kanjivaram Saree"))
                .andExpect(jsonPath("$.data[0].category").value("Saree"));
    }

    @Test
    void getProductById_WhenFound_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.getProductById(1L)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getProductById_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createProduct_ShouldReturn201() throws Exception {
        // Arrange
        when(productService.saveProduct(any(Product.class))).thenReturn(sampleProduct);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Kanjivaram Saree"));
    }

    @Test
    void healthCheck_ShouldReturn200() throws Exception {
        // Arrange
        when(productService.getProductCount()).thenReturn(10L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchProducts_ShouldReturnResults() throws Exception {
        // Arrange
        when(productService.searchProducts("saree")).thenReturn(Arrays.asList(sampleProduct));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/search?keyword=saree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].category").value("Saree"));
    }
}
