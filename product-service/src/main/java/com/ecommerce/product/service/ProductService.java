package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    
		public ProductResponse createProduct(ProductRequest productRequest) {
			
			Product product = Product.builder()
					.name(productRequest.getName())
					.description(productRequest.getDescription())
					.price(productRequest.getPrice())
					.build();
			 Product saved = productRepository.save(product);
			 return mapToProductResponse(saved);
			
		}
		
		public List<ProductResponse> getAllProducts(){
			List<Product> products =productRepository.findAll();
			
			return products.stream().map(this::mapToProductResponse).toList();
		}
		
		private ProductResponse mapToProductResponse(Product product) {
			
			return ProductResponse.builder()
					.id(product.getId())
					.name(product.getName())
					.description(product.getDescription())
					.price(product.getPrice())
					.build();
		}

}

