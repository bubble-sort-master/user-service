package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.entity.PaymentCard;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentCardMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "active", ignore = true)
  PaymentCard toEntity(CardCreateDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "number", ignore = true)
  void updateFromDto(CardUpdateDto dto, @MappingTarget PaymentCard entity);

  @Mapping(target = "numberMasked",
          expression = "java(com.innowise.userservice.util.CardUtils.maskCardNumber(paymentCard.getNumber()))")
  CardShortDto toShortDto(PaymentCard paymentCard);
}