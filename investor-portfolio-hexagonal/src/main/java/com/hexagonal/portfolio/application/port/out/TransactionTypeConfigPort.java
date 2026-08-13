package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/** Supplies the registrar-specific positive/neutral transaction-type vocabularies. */
public interface TransactionTypeConfigPort {

    List<String> getCamsPositive();

    List<String> getCamsNeutral();

    List<String> getKarvyPositive();

    List<String> getKarvyNeutral();

    List<String> getMfManualPositive();

    List<String> getMfManualNeutral();
}
