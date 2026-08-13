package com.hexagonal.portfolio.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundScoreInfo {
    private Double consistencyScore;
    private String performanceBucket;
}
