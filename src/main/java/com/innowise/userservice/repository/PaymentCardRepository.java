package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

  List<PaymentCard> findByUserId(Long userId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE PaymentCard c SET c.active = :active WHERE c.id = :id")
  void setActive(@Param("id") Long id, @Param("active") boolean active);

  @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId", nativeQuery = true)
  long countAllCardsByUserId(@Param("userId") Long userId);
}