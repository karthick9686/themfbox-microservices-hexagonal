package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadInflationIndexPort;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.InflationIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The legacy caller passed {@code PageRequest.of(0, 1)} at the call site; that paging
 * detail is applied here so the application layer stays free of Spring Data types.
 * The query, ordering and row cap are unchanged.
 */
@Component
@RequiredArgsConstructor
class InflationIndexPersistenceAdapter implements LoadInflationIndexPort {

    private final InflationIndexRepository inflationIndexRepository;

    @Override
    public List<Double> findInflationIndex(String date) {
        return inflationIndexRepository.findInflationIndex(date, PageRequest.of(0, 1));
    }
}
