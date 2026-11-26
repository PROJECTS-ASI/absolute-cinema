package com.absolutecinema.payment;

import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class CardValidator {

    private static final Pattern EXPIRY = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");

    /** Valida la tarjeta (RN-17) y devuelve la marca: "VISA" o "AMEX". */
    public String validate(PaymentRequest r) {
        String holder = r.getCardHolder() == null ? "" : r.getCardHolder().trim();
        if (holder.length() < 2 || holder.length() > 60) {
            throw new IllegalArgumentException("El titular de la tarjeta no es válido.");
        }

        if (r.getDni() == null || !r.getDni().matches("^\\d{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
        }

        String raw = r.getCardNumber() == null ? "" : r.getCardNumber();
        if (!raw.matches("^[\\d\\s-]+$")) {
            throw new IllegalArgumentException("El número de tarjeta no es válido.");
        }
        String digits = raw.replaceAll("\\D", "");

        String brand;
        if (digits.startsWith("4") && digits.length() == 16) {
            brand = "VISA";
        } else if ((digits.startsWith("34") || digits.startsWith("37")) && digits.length() == 15) {
            brand = "AMEX";
        } else {
            throw new IllegalArgumentException(
                    "Solo se admiten tarjetas Visa (16 dígitos) o American Express (15 dígitos).");
        }

        if (!passesLuhn(digits)) {
            throw new IllegalArgumentException("El número de tarjeta no es válido.");
        }

        validateExpiry(r.getExpiry());

        String expectedCvv = brand.equals("AMEX") ? "^\\d{4}$" : "^\\d{3}$";
        if (r.getCvv() == null || !r.getCvv().matches(expectedCvv)) {
            throw new IllegalArgumentException(
                    "El CVV debe tener " + (brand.equals("AMEX") ? "4" : "3") + " dígitos.");
        }

        return brand;
    }

    private void validateExpiry(String expiry) {
        Matcher m = expiry == null ? null : EXPIRY.matcher(expiry.trim());
        if (m == null || !m.matches()) {
            throw new IllegalArgumentException("La fecha de vencimiento debe tener formato MM/AA.");
        }
        YearMonth exp = YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        if (exp.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("La tarjeta está vencida.");
        }
    }

    private boolean passesLuhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}