package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {
}
