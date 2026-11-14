package com.runtimeheal.backend.sample.dto;

import java.math.BigDecimal;

/**
 * Request DTO representing a payment transfer in the sample billing service.
 */
public class PaymentRequest {

    private String accountId;
    private BigDecimal amount;
    private String currency;

    public PaymentRequest() {
    }

    public PaymentRequest(String accountId, BigDecimal amount, String currency) {
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
