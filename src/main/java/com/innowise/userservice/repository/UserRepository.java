package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  @EntityGraph(attributePaths = "paymentCards")
  Optional<User> findWithPaymentCardsById(Long id);

  @EntityGraph(attributePaths = "paymentCards")
  Optional<User> findWithPaymentCardsByEmail(String email);

  @EntityGraph(attributePaths = "paymentCards")
  Page<User> findAll(Specification<User> spec, Pageable pageable);

  @EntityGraph(attributePaths = "paymentCards")
  List<User> findWithPaymentCardsByIdIn(List<Long> ids);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
  void setActive(@Param("id") Long id, @Param("active") boolean active);

  @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
  Optional<User> findByIdIncludingInactive(@Param("id") Long id);
}