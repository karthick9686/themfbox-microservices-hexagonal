package com.hexagonal.portfolio.adapter.out.persistence.entity.amfi;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "inflation_index")
public class InflationIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // If id is auto-increment
    @Column(name = "id")
    private Long id;

    @Column(name = "financial_year")
    private String financial_year;

    @Column(name = "start_year")
    private Date start_year;

    @Column(name = "end_year")
    private Date end_year;

    @Column(name = "cost_inflation_index")
    private Double cost_inflation_index;
}
