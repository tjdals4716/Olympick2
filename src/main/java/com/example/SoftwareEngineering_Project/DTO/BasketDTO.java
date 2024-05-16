package com.example.SoftwareEngineering_Project.DTO;

import com.example.SoftwareEngineering_Project.Entity.BasketEntity;
import com.example.SoftwareEngineering_Project.Entity.ProductEntity;
import com.example.SoftwareEngineering_Project.Entity.UserEntity;
import com.example.SoftwareEngineering_Project.Enum.BasketStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BasketDTO {
    private Long id;
    private Long count;
    private BasketStatus basketStatus;
    private Long userId;
    private Long productId;

    public static BasketDTO entityToDto(BasketEntity basketEntity) {
        return new BasketDTO(
                basketEntity.getId(),
                basketEntity.getCount(),
                basketEntity.getBasketStatus(),
                basketEntity.getUser().getId(),
                basketEntity.getProduct().getId()
        );
    }

    public BasketEntity dtoToEntity(UserEntity user, ProductEntity product) {
        return new BasketEntity(id, count, basketStatus, user, product);
    }
}
