package com.hexagonal.portfolio.adapter.out.persistence.entity.amfi;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "amfi_mf_nav")
@Data
public class AmfiMfNav {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    private String mf_company;


    private String scheme_nature;


    private String scheme_code;


    private String scheme_name;


    private Double net_asset_value;


    private Date nav_date;

    private Double rebased_nav;
}
