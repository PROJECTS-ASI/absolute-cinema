
package com.absolutecinema.payment;

import java.time.YearMonth;

import org.springframework.stereotype.Component;

@Component
public class CardValidator {

    public String validate(PaymentRequest request) {

        // Validar número de tarjeta
        String number = request.getCardNumber();

        if (number == null) {
            throw new IllegalArgumentException(
                    "El número de tarjeta es obligatorio");
        }

        number = number.replaceAll("[ -]", "");

        if (!number.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Número de tarjeta inválido");
        }

        // Identificar marca de tarjeta
        String brand;

        if (number.matches("^4\\d{12}(\\d{3})?(\\d{3})?$")) {
            brand = "VISA";

        } else if (number.matches("^3[47]\\d{13}$")) {
            brand = "AMEX";

        } else {
            throw new IllegalArgumentException(
                    "Solo se aceptan Visa o American Express");
        }

        // Validar algoritmo Luhn
        if (!luhn(number)) {
            throw new IllegalArgumentException(
                    "Número de tarjeta inválido (Luhn)");
        }

        // Validar titular
        if (request.getCardHolder() == null ||
                request.getCardHolder().trim().length() < 3) {
            throw new IllegalArgumentException(
                    "El titular es obligatorio");
        }

        // Validar DNI
        if (request.getDni() == null ||
                !request.getDni().matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "El DNI debe tener 8 dígitos");
        }

        // Validar vencimiento (MM/YY o MM/YYYY)
        String expiration = request.getExpirationDate();

        if (expiration == null ||
                !expiration.matches("(0[1-9]|1[0-2])/(\\d{2}|\\d{4})")) {
            throw new IllegalArgumentException(
                    "Formato de vencimiento inválido. Use MM/YY o MM/YYYY");
        }

        String[] parts = expiration.split("/");

        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        if (parts[1].length() == 2) {
            year += 2000;
        }

        YearMonth expiry = YearMonth.of(year, month);

        if (expiry.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException(
                    "La tarjeta está vencida");
        }

        // Validar CVV
        String cvv = request.getCvv();

        boolean validCvv = cvv != null &&
                (brand.equals("VISA")
                        ? cvv.matches("\\d{3}")
                        : cvv.matches("\\d{4}"));

        if (!validCvv) {
            throw new IllegalArgumentException(
                    "CVV inválido");
        }

        return brand;
    }

    // Algoritmo de Luhn
    private boolean luhn(String number) {

        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {

            int digit = Character.getNumericValue(
                    number.charAt(i));

            if (alternate) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}
