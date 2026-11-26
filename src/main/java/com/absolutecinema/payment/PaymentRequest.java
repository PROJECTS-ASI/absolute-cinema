package com.absolutecinema.payment;

import java.math.BigDecimal;

/**
 * Datos del pago. Los campos de tarjeta solo se usan para validar y se descartan:
 * nunca se guardan ni se registran (RNF-12). No agregar toString() ni loguear este objeto.
 */
public class PaymentRequest {

    private PaymentMethod method;
    private BigDecimal amount;      // solo se usa en MANUAL_AMOUNT
    private String cardHolder;
    private String dni;
    private String cardNumber;
    private String expiry;          // MM/YY
    private String cvv;

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}