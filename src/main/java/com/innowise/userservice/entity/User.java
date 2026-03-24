package com.innowise.userservice.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "paymentCards", callSuper = true)
@EqualsAndHashCode(exclude = "paymentCards", callSuper = true)
@Entity
@Table(name = "users")
public class User extends BaseEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "surname", nullable = false, length = 100)
  private String surname;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "active")
  private Boolean active = true;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PaymentCard> paymentCards = new ArrayList<>();

  @Formula("(select count(c.id) from payment_cards c where c.user_id = id)")
  private int cardsCount;
}
