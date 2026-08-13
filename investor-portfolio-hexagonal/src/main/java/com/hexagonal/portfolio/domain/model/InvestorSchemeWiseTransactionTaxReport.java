package com.hexagonal.portfolio.domain.model;

import lombok.Data;

import java.util.Date;

@Data
public class InvestorSchemeWiseTransactionTaxReport
{
    String scheme;
    String scheme_amfi_code;
    String scheme_amfi_short_name;
    String scheme_code;
    String scheme_registrar;
    String isin_no;
    String folio;
    String category;
    String transaction_type;
    Date sold_date;
    Date sold_repeated_date;
    Double sold_units;
    Double sold_amount;
    Double sold_nav;
    Date purchase_date;
    Double purchase_units;
    Double purchase_amount;
    Double purchase_nav;
    Double indexed_nav;
    int no_of_days;
    int transaction_count;
    Double stg_purchase_amount;
    Double stg_sold_amount;
    Double ltg_purchase_amount;
    Double ltg_sold_amount;
    Double stg_total_gain;
    Double stg_gain;
    Double ltg_total_gain;
    Double ltg_gain;
    Double stg_loss;
    Double ltg_loss;
    Double indexed_cost;
    Double indexed_sold_amount;
    Double indexed_total_gain;
    Double indexed_gain;
    Double indexed_loss;
    Double total_tax;
    Double stt;
    String sold_transaction_type;
    String purchase_transaction_type;
    Double redeemed_units;
    Double redeemed_nav;
    Double grandfathered_units;
    Double grandfathered_amount;
    Double grandfathered_purchase_amount;
    Double grandfathered_sold_amount;
    Double grandfathered_nav;
    boolean grandfathered_cost_apply;
    Double non_grandfathered_purchase_amount;
    Double non_grandfathered_sold_amount;
    String purchase_date_str;
    String sold_date_str;
    Double gain_loss;
    Date latest_nav_date;
    String latest_nav_date_str;
    Double latest_nav;
    Double what_if_amount;
    Double actual_amount;
    Double gain_loss_abs_return;
    Double what_if_abs_return;
    Double scheme_total_units;
    Double scheme_current_value;
    Double xirr;
    String puramt_str;
    String soldamt_str;
    String  gainloss_str;
    String whatifamount_str;

    String purchase_amount_str;
    String sold_amount_str;
    String scheme_total_units_str;
    String scheme_current_value_str;
    String gain_loss_str;
    String what_if_amount_str;

    String sold_units_str;
    String purchase_units_str;

    String actual_amount_str;
    String redeemed_units_str;

    String ltg_gain_str;
    boolean hybrid_taxation;
    String amc_logo;
    String scheme_total_row = "";
}
