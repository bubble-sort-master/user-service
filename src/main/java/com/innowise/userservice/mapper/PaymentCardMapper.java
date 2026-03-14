package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.*;
import com.innowise.userservice.entity.PaymentCard;
import org.mapstruct.*;

import java.util.List;

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

  @Mapping(target = "numberMasked", expression = "java(maskCardNumber(paymentCard.getNumber()))")
  CardShortDto toShortDto(PaymentCard paymentCard);

  default String maskCardNumber(String number) {
    if (number == null || number.length() < 8) {
      return "**** **** **** ****";
    }
    String first4 = number.substring(0,4);
    String last4 = number.substring(number.length() - 4);
    return first4 + " **** **** " + last4;
  }
}