package com.hexagonal.portfolio.domain.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class InvestorSchemeWiseTransactionResponse {

    @JsonProperty("AMC_CODE")
    String AMC_CODE;

    @JsonProperty("FOLIO_NO")
    String FOLIO_NO;

    @JsonProperty("PRODCODE")
    String PRODCODE;

    @JsonProperty("SCHEME")
    String SCHEME;

    @JsonProperty("scheme_amfi_short_name")
    String scheme_amfi_short_name;

    @JsonProperty("INV_NAME")
    String INV_NAME;

    @JsonProperty("TRXNTYPE")
    String TRXNTYPE;

    @JsonProperty("TRXNNO")
    Integer TRXNNO;

    @JsonProperty("TRXNMODE")
    String TRXNMODE;

    @JsonProperty("TRXNSTAT")
    String TRXNSTAT;

    @JsonProperty("USERCODE")
    String USERCODE;

    @JsonProperty("USRTRXNO")
    Integer USRTRXNO;

    @JsonProperty("TRADDATE")
    Date TRADDATE;

    @JsonProperty("POSTDATE")
    Date POSTDATE;

    @JsonProperty("PURPRICE")
    Double PURPRICE;

    @JsonProperty("UNITS")
    Double UNITS;

    @JsonProperty("AMOUNT")
    Double AMOUNT;

    @JsonProperty("BROKCODE")
    String BROKCODE;

    @JsonProperty("SUBBROK")
    String SUBBROK;

    @JsonProperty("BROKPERC")
    Double BROKPERC;

    @JsonProperty("BROKCOMM")
    Double BROKCOMM;

    @JsonProperty("ALTFOLIO")
    Integer ALTFOLIO;

    @JsonProperty("REP_DATE")
    Date REP_DATE;

    @JsonProperty("TIME1")
    String TIME1;

    @JsonProperty("TRXNSUBTYP")
    String TRXNSUBTYP;

    @JsonProperty("APPLICATIO")
    String APPLICATIO;

    @JsonProperty("TRXN_NATUR")
    String TRXN_NATUR;

    @JsonProperty("TAX")
    Double TAX;

    @JsonProperty("TOTAL_TAX")
    Double TOTAL_TAX;

    @JsonProperty("TE_15H")
    String TE_15H;

    @JsonProperty("MICR_NO")
    String MICR_NO;

    @JsonProperty("REMARKS")
    String REMARKS;

    @JsonProperty("SWFLAG")
    String SWFLAG;

    @JsonProperty("OLD_FOLIO")
    String OLD_FOLIO;

    @JsonProperty("SEQ_NO")
    Integer SEQ_NO;

    @JsonProperty("REINVEST_F")
    String REINVEST_F;

    @JsonProperty("MULT_BROK")
    String MULT_BROK;

    @JsonProperty("STT")
    Double STT;

    @JsonProperty("LOCATION")
    String LOCATION;

    @JsonProperty("SCHEME_TYP")
    String SCHEME_TYP;

    @JsonProperty("TAX_STATUS")
    String TAX_STATUS;

    @JsonProperty("LOADS")
    Double LOADS;

    @JsonProperty("SCANREFNO")
    String SCANREFNO;

    @JsonProperty("PAN")
    String PAN;

    @JsonProperty("INV_IIN")
    Integer INV_IIN;

    @JsonProperty("TARG_SRC_S")
    String TARG_SRC_S;

    @JsonProperty("TRXN_TYPE_")
    String TRXN_TYPE_;

    @JsonProperty("TICOB_TRTY")
    String TICOB_TRTY;

    @JsonProperty("TICOB_TRNO")
    String TICOB_TRNO;

    @JsonProperty("TICOB_POST")
    String TICOB_POST;

    @JsonProperty("DP_ID")
    String DP_ID;

    @JsonProperty("TRXN_CHARG")
    Double TRXN_CHARG;

    @JsonProperty("ELIGIB_AMT")
    Double ELIGIB_AMT;

    @JsonProperty("SRC_OF_TXN")
    String SRC_OF_TXN;

    @JsonProperty("TRXN_SUFFI")
    String TRXN_SUFFI;

    @JsonProperty("SIPTRXNNO")
    Double SIPTRXNNO;

    @JsonProperty("TER_LOCATI")
    String TER_LOCATI;

    @JsonProperty("EUIN")
    String EUIN;

    @JsonProperty("EUIN_VALID")
    String EUIN_VALID;

    @JsonProperty("EUIN_OPTED")
    String EUIN_OPTED;

    @JsonProperty("SUB_BRK_AR")
    String SUB_BRK_AR;

    @JsonProperty("TOTAL_UNITS")
    Double TOTAL_UNITS;

    @JsonProperty("purchase_average_price")
    Double purchase_average_price;

    @JsonProperty("total_amount")
    Double total_amount;

    @JsonProperty("current_cost")
    Double current_cost;

    @JsonProperty("current_value")
    Double current_value;

    @JsonProperty("total_inflow_amount")
    Double total_inflow_amount;

    @JsonProperty("user_id")
    Integer user_id;

    @JsonProperty("branch")
    String branch;

    @JsonProperty("rm_name")
    String rm_name;

    @JsonProperty("subbroker_name")
    String subbroker_name;

    @JsonProperty("redemption_date")
    Date redemption_date;

    @JsonProperty("redemption_nav")
    Double redemption_nav;

    @JsonProperty("redemption_units")
    Double redemption_units;

    @JsonProperty("redemption_amount")
    Double redemption_amount;

    @JsonProperty("STAMP_DUTY")
    Double STAMP_DUTY;

    @JsonProperty("ind_amt")
    String ind_amt = "0";

    @JsonProperty("TRADDATE_str")
    String TRADDATE_str = "";

    @JsonProperty("PURPRICE_str")
    String PURPRICE_str = "0";

    @JsonProperty("UNITS_str")
    String UNITS_str = "0";

    @JsonProperty("STAMP_DUTY_str")
    String STAMP_DUTY_str = "0";

    @JsonProperty("TOTAL_TAX_str")
    String TOTAL_TAX_str = "0";

    @JsonProperty("STT_str")
    String STT_str = "0";

    @JsonProperty("total_amount_str")
    String total_amount_str = "0";

    @JsonProperty("TOTAL_UNITS_str")
    String TOTAL_UNITS_str = "0";

    @JsonProperty("Registrar")
    String Registrar;

    @JsonProperty("AMOUNT_str")
    String AMOUNT_str;

    @JsonProperty("BANK_NAME")
    String BANK_NAME;

    @JsonProperty("ACCOUNT_NUMBER")
    String ACCOUNT_NUMBER;

    @JsonProperty("LOADS_str")
    String LOADS_str = "0";

    @JsonProperty("purchase_units")
    Double purchase_units;

    @JsonProperty("logo")
    private String logo;
}
