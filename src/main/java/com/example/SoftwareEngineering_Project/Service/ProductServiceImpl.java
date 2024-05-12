package com.example.SoftwareEngineering_Project.Service;

import com.example.SoftwareEngineering_Project.DTO.DeliveryDTO;
import com.example.SoftwareEngineering_Project.Entity.BasketEntity;
import com.example.SoftwareEngineering_Project.Entity.DeliveryEntity;
import com.example.SoftwareEngineering_Project.Enum.Category;
import com.example.SoftwareEngineering_Project.Enum.DeliveryStatus;
import com.example.SoftwareEngineering_Project.Repository.BasketRepository;
import com.example.SoftwareEngineering_Project.DTO.BasketDTO;
import com.example.SoftwareEngineering_Project.DTO.ProductDTO;
import com.example.SoftwareEngineering_Project.Entity.ProductEntity;
import com.example.SoftwareEngineering_Project.Entity.UserEntity;
import com.example.SoftwareEngineering_Project.Repository.DeliveryRepository;
import com.example.SoftwareEngineering_Project.Repository.ProductRepository;
import com.example.SoftwareEngineering_Project.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BasketRepository basketRepository;
    private final DeliveryRepository deliveryRepository;

    //상품 등록
    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        UserEntity userEntity = userRepository.findById(productDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + productDTO.getUserId()));

        ProductEntity productEntity = productDTO.dtoToEntity(userEntity, productDTO.getCategory());
        ProductEntity savedProduct = productRepository.save(productEntity);
        logger.info("상품 등록 완료! " + savedProduct);
        return ProductDTO.entityToDto(savedProduct);
    }

    //상품 장바구니에 담기, 동일한 상품일 경우 개수만 증가
    @Override
    public BasketDTO addToBasket(Long userId, Long productId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + userId));

        ProductEntity productEntity = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. productId: " + productId));

        BasketEntity existingBasketItem = basketRepository.findByUser_IdAndProduct_Id(userId, productId);

        if (existingBasketItem != null) {
            existingBasketItem.setCount(existingBasketItem.getCount() + 1);
            BasketEntity savedBasket = basketRepository.save(existingBasketItem);
            logger.info("장바구니에 상품의 개수가 증가하였습니다 " + savedBasket);
            return BasketDTO.entityToDto(savedBasket);
        } else {
            BasketDTO basketDTO = new BasketDTO();
            basketDTO.setCount(1);
            BasketEntity basketEntity = basketDTO.dtoToEntity(userEntity, productEntity);
            BasketEntity savedBasket = basketRepository.save(basketEntity);
            logger.info("장바구니에 상품 추가 완료! " + savedBasket);
            return BasketDTO.entityToDto(savedBasket);
        }
    }

    //상품 장바구니에서 빼기, 동일한 상품일 경우 개수만 감소, 1에서 뺄 경우 장바구니에서 상품 삭제
    @Override
    public BasketDTO removeFromBasket(Long userId, Long productId) {
        BasketEntity existingBasketItem = basketRepository.findByUser_IdAndProduct_Id(userId, productId);

        if (existingBasketItem != null) {
            if (existingBasketItem.getCount() > 1) {
                existingBasketItem.setCount(existingBasketItem.getCount() - 1);
                BasketEntity savedBasket = basketRepository.save(existingBasketItem);
                logger.info("장바구니에 상품의 개수가 감소하였습니다 " + savedBasket);
                return BasketDTO.entityToDto(savedBasket);
            } else {
                basketRepository.delete(existingBasketItem);
                logger.info("장바구니에서 상품 삭제 완료! " + existingBasketItem);
                return null;
            }
        } else {
            throw new RuntimeException("장바구니에 해당 상품이 없습니다. userId: " + userId + ", productId: " + productId);
        }
    }

    //사용자 장바구니에 있는 상품 배송
    @Override
    public List<DeliveryDTO> createDeliveryForBasket(Long userId, DeliveryStatus status) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + userId));

        List<BasketEntity> baskets = basketRepository.findByUser_Id(userId);

        List<DeliveryEntity> deliveryEntities = baskets.stream()
                .map(basket -> {
                    DeliveryDTO deliveryDTO = new DeliveryDTO();
                    deliveryDTO.setUserId(userId);
                    deliveryDTO.setBasketId(basket.getId());
                    deliveryDTO.setStatus(status);
                    deliveryDTO.setStatusDateTime(LocalDateTime.now());

                    return deliveryDTO.dtoToEntity(user, basket, status);
                })
                .collect(Collectors.toList());

        List<DeliveryEntity> savedDeliveries = deliveryRepository.saveAll(deliveryEntities);

        logger.info("장바구니에 담긴 모든 상품의 배송이 시작되었습니다.");
        return savedDeliveries.stream()
                .map(DeliveryDTO::entityToDto)
                .collect(Collectors.toList());
    }

    //배송 상태 수정
    @Override
    public DeliveryDTO updateDeliveryStatus(Long deliveryId, DeliveryStatus status) {
        DeliveryEntity deliveryEntity = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("배송을 찾을 수 없습니다. deliveryId: " + deliveryId));

        deliveryEntity.setStatus(status);
        deliveryEntity.setStatusDateTime(LocalDateTime.now());

        DeliveryEntity updatedDelivery = deliveryRepository.save(deliveryEntity);

        logger.info("상품의 배송상태가 변경되었습니다!");
        return DeliveryDTO.entityToDto(updatedDelivery);
    }

    //모든 상품 조회
    @Override
    public List<ProductDTO> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(ProductDTO::entityToDto)
                .collect(Collectors.toList());
    }

    //해당 상품만 조회
    @Override
    public ProductDTO getProductById(Long id) {
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. id: " + id));
        return ProductDTO.entityToDto(productEntity);
    }

    //상품 수정
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        UserEntity userEntity = userRepository.findById(productDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + productDTO.getUserId()));

        ProductEntity existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. id: " + id));

        existingProduct.setName(productDTO.getName());
        existingProduct.setContent(productDTO.getContent());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setMediaUrl(productDTO.getMediaUrl());
        existingProduct.setCategory(productDTO.getCategory());
        existingProduct.setUser(userEntity);

        ProductEntity updatedProduct = productRepository.save(existingProduct);
        logger.info("상품 정보 업데이트 완료! " + updatedProduct);
        return ProductDTO.entityToDto(updatedProduct);
    }

    //상품 삭제
    @Override
    public void deleteProduct(Long id) {
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. id: " + id));

        productRepository.delete(productEntity);
        logger.info("상품 삭제 완료! id: " + id);
    }

    //카테고리별 상품 조회
    @Override
    public List<ProductDTO> getProductsByCategory(String category) {
        List<ProductEntity> products = productRepository.findByCategory(category);
        return products.stream()
                .map(ProductDTO::entityToDto)
                .collect(Collectors.toList());
    }

    //상품 검색
    @Override
    public List<ProductDTO> searchProductsByName(String name) {
        List<ProductEntity> products = productRepository.findByNameContainingIgnoreCase(name);
        return products.stream()
                .map(ProductDTO::entityToDto)
                .collect(Collectors.toList());
    }
}