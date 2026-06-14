package com.shranvi.api.service;

import com.shranvi.api.model.Product;
import com.shranvi.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Banarasi Silk Saree");
        sampleProduct.setDescription("Premium Banarasi silk saree with gold zari work");
        sampleProduct.setPrice(new BigDecimal("4999.00"));
        sampleProduct.setCategory("Saree");
        sampleProduct.setSku("SAR-001");
        sampleProduct.setIsActive(true);
        sampleProduct.setStockQuantity(50);
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Arrange
        when(productRepository.findAll()).thenReturn(Arrays.asList(sampleProduct));

        // Act
        List<Product> products = productService.getAllProducts();

        // Assert
        assertThat(products).isNotNull();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Banarasi Silk Saree");
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getActiveProducts_ShouldReturnOnlyActiveProducts() {
        // Arrange
        when(productRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(sampleProduct));

        // Act
        List<Product> products = productService.getActiveProducts();

        // Assert
        assertThat(products).isNotNull();
        assertThat(products).allMatch(Product::getIsActive);
        verify(productRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getProductById_WhenExists_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        // Act
        Optional<Product> result = productService.getProductById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getSku()).isEqualTo("SAR-001");
    }

    @Test
    void getProductById_WhenNotExists_ShouldReturnEmpty() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Product> result = productService.getProductById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void saveProduct_ShouldSaveAndReturnProduct() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // Act
        Product saved = productService.saveProduct(sampleProduct);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Banarasi Silk Saree");
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    void deleteProduct_WhenExists_ShouldSoftDelete() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // Act
        boolean result = productService.deleteProduct(1L);

        // Assert
        assertThat(result).isTrue();
        assertThat(sampleProduct.getIsActive()).isFalse(); // Soft deleted
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    void deleteProduct_WhenNotExists_ShouldReturnFalse() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = productService.deleteProduct(999L);

        // Assert
        assertThat(result).isFalse();
        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductCount_ShouldReturnCount() {
        // Arrange
        when(productRepository.countByIsActiveTrue()).thenReturn(25L);

        // Act
        long count = productService.getProductCount();

        // Assert
        assertThat(count).isEqualTo(25L);
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() {
        // Arrange
        when(productRepository.searchByName("silk")).thenReturn(Arrays.asList(sampleProduct));

        // Act
        List<Product> results = productService.searchProducts("silk");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).containsIgnoringCase("Silk");
    }
}
