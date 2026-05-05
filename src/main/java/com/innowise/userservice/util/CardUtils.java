package com.innowise.userservice.util;

public final class CardUtils {
    private CardUtils() {}

    public static String maskCardNumber(String number) {
        if (number == null || number.length() < 8) {
            return "**** **** **** ****";
        }
        String first4 = number.substring(0, 4);
        String last4 = number.substring(number.length() - 4);
        return first4 + " **** **** " + last4;
    }
}