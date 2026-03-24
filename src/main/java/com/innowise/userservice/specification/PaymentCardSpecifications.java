package com.innowise.userservice.specification;

import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;

public final class PaymentCardSpecifications {

  private PaymentCardSpecifications() {}

  public static Specification<PaymentCard> hasUserName(String name) {
    return (root, query, cb) -> {
      if (name == null || name.isBlank()) {
        return cb.conjunction();
      }
      Join<PaymentCard, User> userJoin = root.join("user");
      return cb.like(
              cb.lower(userJoin.get("name")),
              "%" + name.toLowerCase() + "%"
      );
    };
  }

  public static Specification<PaymentCard> hasUserSurname(String surname) {
    return (root, query, cb) -> {
      if (surname == null || surname.isBlank()) {
        return cb.conjunction();
      }
      Join<PaymentCard, User> userJoin = root.join("user");
      return cb.like(
              cb.lower(userJoin.get("surname")),
              "%" + surname.toLowerCase() + "%"
      );
    };
  }

  public static Specification<PaymentCard> searchByUserNameAndSurname(String name, String surname) {
    return Specification.where(hasUserName(name)).and(hasUserSurname(surname));
  }
}
