package com.innowise.userservice.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString(exclude = "user", callSuper = true)
@EqualsAndHashCode(exclude = "user", callSuper = true)
@Entity
@Table(name = "payment_cards")
public class PaymentCard extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 20, unique = true)
  private String number;

  @Column(nullable = false, length = 200)
  private String holder;

  @Column(name = "expiration_date", nullable = false)
  private LocalDate expirationDate;

  @Column(nullable = false)
  private Boolean active = true;

}
