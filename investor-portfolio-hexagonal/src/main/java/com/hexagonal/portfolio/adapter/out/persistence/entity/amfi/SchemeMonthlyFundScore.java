package com.hexagonal.portfolio.adapter.out.persistence.entity.amfi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheme_monthly_fund_score")
@Getter
@Setter
@NoArgsConstructor
public class SchemeMonthlyFundScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "scheme_name", length = 500)
    private String schemeName;

    @Column(name = "scheme_amfi_code", length = 45)
    private String schemeAmfiCode;

    @Column(name = "scheme_category", length = 255)
    private String schemeCategory;

    @Column(name = "consistency_score")
    private Double consistencyScore;

    @Column(name = "consistency_score_month", length = 45)
    private String consistencyScoreMonth;

    @Column(name = "consistency_score_year", length = 45)
    private String consistencyScoreYear;

    @Column(name = "performance_bucket", length = 45)
    private String performanceBucket;
}
