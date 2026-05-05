package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = PaymentCardMapper.class
)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "paymentCards", ignore = true)
  User toEntity(UserCreateDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "paymentCards", ignore = true)
  void updateFromDto(UserUpdateDto dto, @MappingTarget User entity);

  @Mapping(target = "cardsCount", source = "cardsCount")
  UserShortDto toShortDto(User user);

  @Mapping(target = "cards", source = "paymentCards")
  UserWithCardsDto toWithCardsDto(User user);
}