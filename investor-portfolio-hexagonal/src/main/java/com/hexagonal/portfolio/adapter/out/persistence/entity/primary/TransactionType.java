package com.hexagonal.portfolio.adapter.out.persistence.entity.primary;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "transaction_type")
@Data
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 45)
    private String registrar;

    @Column(columnDefinition = "TEXT")
    private String positive_transaction;

    @Column(columnDefinition = "TEXT")
    private String negative_transaction;

    @Column(columnDefinition = "TEXT")
    private String neutral_transaction;
}
