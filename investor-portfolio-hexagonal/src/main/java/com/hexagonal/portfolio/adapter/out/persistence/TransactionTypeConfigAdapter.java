package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.TransactionType;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.TransactionTypeRepository;
import com.hexagonal.portfolio.application.port.out.TransactionTypeConfigPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads the registrar transaction-type vocabularies once at startup, exactly as the legacy
 * {@code TransactionTypeConfigService} did, so the valuation loops never hit the database.
 */
@Component
@RequiredArgsConstructor
class TransactionTypeConfigAdapter implements TransactionTypeConfigPort {

    private final TransactionTypeRepository transactionTypeRepository;

    private List<String> camsPositive = new ArrayList<>();
    private List<String> camsNeutral = new ArrayList<>();
    private List<String> karvyPositive = new ArrayList<>();
    private List<String> karvyNeutral = new ArrayList<>();
    private List<String> mfManualPositive = new ArrayList<>();
    private List<String> mfManualNeutral = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<TransactionType> list = transactionTypeRepository.findAll();
        for (TransactionType tt : list) {
            String registrar = tt.getRegistrar();
            String[] pos = tt.getPositive_transaction().split("[\\,]+");
            String[] neu = tt.getNeutral_transaction().split("[\\,]+");
            if (registrar.equalsIgnoreCase("cams")) {
                camsPositive = Arrays.asList(pos);
                camsNeutral = Arrays.asList(neu);
            } else if (registrar.equalsIgnoreCase("karvy")) {
                karvyPositive = Arrays.asList(pos);
                karvyNeutral = Arrays.asList(neu);
            } else if (registrar.equalsIgnoreCase("mf_manual")) {
                mfManualPositive = Arrays.asList(pos);
                mfManualNeutral = Arrays.asList(neu);
            }
        }
    }

    @Override
    public List<String> getCamsPositive() {
        return camsPositive;
    }

    @Override
    public List<String> getCamsNeutral() {
        return camsNeutral;
    }

    @Override
    public List<String> getKarvyPositive() {
        return karvyPositive;
    }

    @Override
    public List<String> getKarvyNeutral() {
        return karvyNeutral;
    }

    @Override
    public List<String> getMfManualPositive() {
        return mfManualPositive;
    }

    @Override
    public List<String> getMfManualNeutral() {
        return mfManualNeutral;
    }
}
