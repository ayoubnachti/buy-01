package com.ecommerce.productservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.productservice.dtos.request.ProductRequest;
import com.ecommerce.productservice.exceptions.custom.ForbiddenException;
import com.ecommerce.productservice.exceptions.custom.ResourceNotFoundException;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.repositories.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  private ProductService productService;

  private static final String SELLER_ID = "seller-123";
  private static final String OTHER_SELLER_ID = "seller-456";
  private static final String PRODUCT_ID = "product-abc";

  @BeforeEach
  void setUp() {
    productService = new ProductService(productRepository);
  }

  // --- create ---

  @Test
  void create_setsSellerIdFromParam_notFromRequestBody() {
    ProductRequest request = new ProductRequest(
        "Keyboard", "Mechanical", BigDecimal.TEN, 5, List.of("http://img/1.png"));

    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    productService.create(request, SELLER_ID);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(SELLER_ID);
  }

  @Test
  void create_withNullImageUrls_defaultsToEmptyList() {
    ProductRequest request = new ProductRequest(
        "Mouse", "Wireless", BigDecimal.ONE, 10, null);

    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    productService.create(request, SELLER_ID);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertThat(captor.getValue().getImageUrls()).isEmpty();
  }

  @Test
  void create_withImageUrls_persistsThemAsProvided() {
    List<String> urls = List.of("http://img/1.png", "http://img/2.png");
    ProductRequest request = new ProductRequest("Monitor", "27in", BigDecimal.TEN, 2, urls);

    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    productService.create(request, SELLER_ID);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertThat(captor.getValue().getImageUrls()).containsExactlyElementsOf(urls);
  }

  // --- updateProduct: ownership (the bug we just fixed — this is the important
  // one) ---

  @Test
  void updateProduct_ownerMatches_savesAndReturnsUpdatedFields() {
    Product existing = Product.builder()
        .id(PRODUCT_ID)
        .name("Old name")
        .description("Old desc")
        .price(BigDecimal.ONE)
        .quantity(1)
        .userId(SELLER_ID)
        .imageUrls(List.of())
        .build();

    ProductRequest request = new ProductRequest(
        "New name", "New desc", BigDecimal.TEN, 5, List.of("http://img/new.png"));

    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = productService.updateProduct(request, PRODUCT_ID, SELLER_ID);

    verify(productRepository).save(existing); // fails if the save() call is missing again
    assertThat(result.name()).isEqualTo("New name");
    assertThat(result.imageUrls()).containsExactly("http://img/new.png");
  }

  @Test
  void updateProduct_callerIsNotOwner_throwsForbiddenAndDoesNotSave() {
    Product existing = Product.builder()
        .id(PRODUCT_ID)
        .name("Name")
        .description("Desc")
        .price(BigDecimal.ONE)
        .quantity(1)
        .userId(SELLER_ID)
        .imageUrls(List.of())
        .build();

    ProductRequest request = new ProductRequest("Hacked", "Desc", BigDecimal.ONE, 1, null);

    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> productService.updateProduct(request, PRODUCT_ID, OTHER_SELLER_ID))
        .isInstanceOf(ForbiddenException.class);

    verify(productRepository, never()).save(any());
  }

  @Test
  void updateProduct_productDoesNotExist_throwsNotFound() {
    ProductRequest request = new ProductRequest("Name", "Desc", BigDecimal.ONE, 1, null);

    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.updateProduct(request, PRODUCT_ID, SELLER_ID))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(productRepository, never()).save(any());
  }

  @Test
  void deleteProduct_ownerMatches_deletesProduct() {
    Product existing = Product.builder()
        .id(PRODUCT_ID)
        .userId(SELLER_ID)
        .imageUrls(List.of())
        .build();

    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

    productService.deleteProduct(PRODUCT_ID, SELLER_ID);

    verify(productRepository).delete(existing);
  }

  @Test
  void deleteProduct_callerIsNotOwner_throwsForbiddenAndDoesNotDelete() {
    Product existing = Product.builder()
        .id(PRODUCT_ID)
        .userId(SELLER_ID)
        .imageUrls(List.of())
        .build();

    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID, OTHER_SELLER_ID))
        .isInstanceOf(ForbiddenException.class);

    verify(productRepository, never()).delete(any());
  }

  @Test
  void deleteProduct_productDoesNotExist_throwsNotFound() {
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID, SELLER_ID))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(productRepository, never()).delete(any());
  }
}