package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.TransactionType;

import java.util.List;

/** Loads the raw registrar transaction-type rows used by the tax report. */
public interface LoadTransactionTypePort {

    List<TransactionType> findAll();
}
