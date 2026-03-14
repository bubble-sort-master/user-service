package com.innowise.userservice.exception;

public class MaximumCardsLimitExceededException extends RuntimeException {
  public MaximumCardsLimitExceededException(int maxCards) {
    super("User already has maximum number of cards (%d)".formatted(maxCards));
  }
}