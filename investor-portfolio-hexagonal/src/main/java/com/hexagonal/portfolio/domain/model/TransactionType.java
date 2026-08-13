package com.hexagonal.portfolio.domain.model;


import lombok.Data;
@Data
public class TransactionType {
    private int id;
    private String registrar;
    private String positive_transaction;
    private String negative_transaction;
    private String neutral_transaction;
}
