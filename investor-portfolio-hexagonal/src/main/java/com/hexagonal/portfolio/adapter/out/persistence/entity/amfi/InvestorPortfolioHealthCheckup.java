package com.hexagonal.portfolio.adapter.out.persistence.entity.amfi;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "investor_portfolio_health_checkup")
public class InvestorPortfolioHealthCheckup
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    private String scheme_company;
    private String scheme_name;
    private String scheme_amfi_short_name;
    private String scheme_amfi_code;
    private String rating;
    private Integer rating_value;
    private String scheme_amfi_common;
    private String scheme_plan_type;
    private String dividend_scheme;
    private String riskometer;
    private String scheme_broad_category;
    private String scheme_advisorkhoj_category;
    private String returns_cmp_3year;
    private String returns_cmp_3year_rank;
    private String returns_cmp_3year_total_rank;
    private Date returns_as_on_date;
    private String scheme_sub_category;
}
