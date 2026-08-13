package com.hexagonal.portfolio.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CapitalGainReportApiResponse
{
    @JsonProperty("status")
    private int status;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("msg")
    private String msg;

    // ── Portfolio totals ──────────────────────────────────────────────────────
    @JsonProperty("portfolio_long_gain")
    private Double portfolioLongGain;

    @JsonProperty("portfolio_short_gain")
    private Double portfolioShortGain;

    @JsonProperty("portfolio_gain")
    private Double portfolioGain;

    // ── Equity totals ─────────────────────────────────────────────────────────
    @JsonProperty("eq_gain")
    private Double eqGain;

    @JsonProperty("eq_stg_gain")
    private Double eqStgGain;

    @JsonProperty("eq_stg_loss")
    private Double eqStgLoss;

    @JsonProperty("eq_ltg_gain")
    private Double eqLtgGain;

    @JsonProperty("eq_ltg_loss")
    private Double eqLtgLoss;

    @JsonProperty("eq_stg_purchase_amount")
    private Double eqStgPurchaseAmount;

    @JsonProperty("eq_stg_sold_amount")
    private Double eqStgSoldAmount;

    @JsonProperty("eq_ltg_purchase_amount")
    private Double eqLtgPurchaseAmount;

    @JsonProperty("eq_ltg_sold_amount")
    private Double eqLtgSoldAmount;

    @JsonProperty("eq_ltg_indexed_amount")
    private Double eqLtgIndexedAmount;

    // ── Debt totals ───────────────────────────────────────────────────────────
    @JsonProperty("debt_gain")
    private Double debtGain;

    @JsonProperty("debt_stg_gain")
    private Double debtStgGain;

    @JsonProperty("debt_stg_loss")
    private Double debtStgLoss;

    @JsonProperty("debt_ltg_gain")
    private Double debtLtgGain;

    @JsonProperty("debt_ltg_loss")
    private Double debtLtgLoss;

    @JsonProperty("debt_stg_purchase_amount")
    private Double debtStgPurchaseAmount;

    @JsonProperty("debt_stg_sold_amount")
    private Double debtStgSoldAmount;

    @JsonProperty("debt_ltg_purchase_amount")
    private Double debtLtgPurchaseAmount;

    @JsonProperty("debt_ltg_sold_amount")
    private Double debtLtgSoldAmount;

    @JsonProperty("debt_ltg_indexed_amount")
    private Double debtLtgIndexedAmount;

    // ── Scheme-grouped lists ──────────────────────────────────────────────────
    @JsonProperty("equity_summary_list")
    private List<SchemeCapitalGainDto> equitySummaryList;

    @JsonProperty("debt_summary_list")
    private List<SchemeCapitalGainDto> debtSummaryList;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getStatusMsg() { return statusMsg; }
    public void setStatusMsg(String statusMsg) { this.statusMsg = statusMsg; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public Double getPortfolioLongGain() { return portfolioLongGain; }
    public void setPortfolioLongGain(Double v) { this.portfolioLongGain = v; }

    public Double getPortfolioShortGain() { return portfolioShortGain; }
    public void setPortfolioShortGain(Double v) { this.portfolioShortGain = v; }

    public Double getPortfolioGain() { return portfolioGain; }
    public void setPortfolioGain(Double v) { this.portfolioGain = v; }

    public Double getEqGain() { return eqGain; }
    public void setEqGain(Double v) { this.eqGain = v; }

    public Double getEqStgGain() { return eqStgGain; }
    public void setEqStgGain(Double v) { this.eqStgGain = v; }

    public Double getEqStgLoss() { return eqStgLoss; }
    public void setEqStgLoss(Double v) { this.eqStgLoss = v; }

    public Double getEqLtgGain() { return eqLtgGain; }
    public void setEqLtgGain(Double v) { this.eqLtgGain = v; }

    public Double getEqLtgLoss() { return eqLtgLoss; }
    public void setEqLtgLoss(Double v) { this.eqLtgLoss = v; }

    public Double getEqStgPurchaseAmount() { return eqStgPurchaseAmount; }
    public void setEqStgPurchaseAmount(Double v) { this.eqStgPurchaseAmount = v; }

    public Double getEqStgSoldAmount() { return eqStgSoldAmount; }
    public void setEqStgSoldAmount(Double v) { this.eqStgSoldAmount = v; }

    public Double getEqLtgPurchaseAmount() { return eqLtgPurchaseAmount; }
    public void setEqLtgPurchaseAmount(Double v) { this.eqLtgPurchaseAmount = v; }

    public Double getEqLtgSoldAmount() { return eqLtgSoldAmount; }
    public void setEqLtgSoldAmount(Double v) { this.eqLtgSoldAmount = v; }

    public Double getEqLtgIndexedAmount() { return eqLtgIndexedAmount; }
    public void setEqLtgIndexedAmount(Double v) { this.eqLtgIndexedAmount = v; }

    public Double getDebtGain() { return debtGain; }
    public void setDebtGain(Double v) { this.debtGain = v; }

    public Double getDebtStgGain() { return debtStgGain; }
    public void setDebtStgGain(Double v) { this.debtStgGain = v; }

    public Double getDebtStgLoss() { return debtStgLoss; }
    public void setDebtStgLoss(Double v) { this.debtStgLoss = v; }

    public Double getDebtLtgGain() { return debtLtgGain; }
    public void setDebtLtgGain(Double v) { this.debtLtgGain = v; }

    public Double getDebtLtgLoss() { return debtLtgLoss; }
    public void setDebtLtgLoss(Double v) { this.debtLtgLoss = v; }

    public Double getDebtStgPurchaseAmount() { return debtStgPurchaseAmount; }
    public void setDebtStgPurchaseAmount(Double v) { this.debtStgPurchaseAmount = v; }

    public Double getDebtStgSoldAmount() { return debtStgSoldAmount; }
    public void setDebtStgSoldAmount(Double v) { this.debtStgSoldAmount = v; }

    public Double getDebtLtgPurchaseAmount() { return debtLtgPurchaseAmount; }
    public void setDebtLtgPurchaseAmount(Double v) { this.debtLtgPurchaseAmount = v; }

    public Double getDebtLtgSoldAmount() { return debtLtgSoldAmount; }
    public void setDebtLtgSoldAmount(Double v) { this.debtLtgSoldAmount = v; }

    public Double getDebtLtgIndexedAmount() { return debtLtgIndexedAmount; }
    public void setDebtLtgIndexedAmount(Double v) { this.debtLtgIndexedAmount = v; }

    public List<SchemeCapitalGainDto> getEquitySummaryList() { return equitySummaryList; }
    public void setEquitySummaryList(List<SchemeCapitalGainDto> v) { this.equitySummaryList = v; }

    public List<SchemeCapitalGainDto> getDebtSummaryList() { return debtSummaryList; }
    public void setDebtSummaryList(List<SchemeCapitalGainDto> v) { this.debtSummaryList = v; }

    // =========================================================================
    // INNER: Scheme-level summary (one per scheme+folio combination)
    // Contains aggregated gain/loss + individual transaction_list inside
    // =========================================================================
    public static class SchemeCapitalGainDto {

        @JsonProperty("scheme")
        private String scheme;

        @JsonProperty("scheme_amfi_code")
        private String schemeAmfiCode;

        @JsonProperty("scheme_amfi_short_name")
        private String schemeAmfiShortName;

        @JsonProperty("scheme_code")
        private String schemeCode;

        @JsonProperty("scheme_registrar")
        private String schemeRegistrar;

        @JsonProperty("isin_no")
        private String isinNo;

        @JsonProperty("folio")
        private String folio;

        @JsonProperty("category")
        private String category;

        @JsonProperty("amc_logo")
        private String amcLogo;

        // Aggregated amounts
        @JsonProperty("sold_units")
        private Double soldUnits;

        @JsonProperty("sold_amount")
        private Double soldAmount;

        @JsonProperty("purchase_units")
        private Double purchaseUnits;

        @JsonProperty("purchase_amount")
        private Double purchaseAmount;

        @JsonProperty("stg_purchase_amount")
        private Double stgPurchaseAmount;

        @JsonProperty("stg_sold_amount")
        private Double stgSoldAmount;

        @JsonProperty("ltg_purchase_amount")
        private Double ltgPurchaseAmount;

        @JsonProperty("ltg_sold_amount")
        private Double ltgSoldAmount;

        @JsonProperty("stg_total_gain")
        private Double stgTotalGain;

        @JsonProperty("stg_gain")
        private Double stgGain;

        @JsonProperty("ltg_total_gain")
        private Double ltgTotalGain;

        @JsonProperty("ltg_gain")
        private Double ltgGain;

        @JsonProperty("stg_loss")
        private Double stgLoss;

        @JsonProperty("ltg_loss")
        private Double ltgLoss;

        @JsonProperty("indexed_cost")
        private Double indexedCost;

        @JsonProperty("indexed_total_gain")
        private Double indexedTotalGain;

        @JsonProperty("indexed_gain")
        private Double indexedGain;

        @JsonProperty("indexed_loss")
        private Double indexedLoss;

        @JsonProperty("indexed_sold_amount")
        private Double indexedSoldAmount;

        @JsonProperty("total_tax")
        private Double totalTax;

        @JsonProperty("stt")
        private Double stt;

        @JsonProperty("gain_loss")
        private Double gainLoss;

        @JsonProperty("grandfathered_units")
        private Double grandfatheredUnits;

        @JsonProperty("grandfathered_amount")
        private Double grandfatheredAmount;

        @JsonProperty("grandfathered_nav")
        private Double grandfatheredNav;

        @JsonProperty("grandfathered_purchase_amount")
        private Double grandfatheredPurchaseAmount;

        @JsonProperty("grandfathered_sold_amount")
        private Double grandfatheredSoldAmount;

        @JsonProperty("non_grandfathered_purchase_amount")
        private Double nonGrandfatheredPurchaseAmount;

        @JsonProperty("non_grandfathered_sold_amount")
        private Double nonGrandfatheredSoldAmount;

        @JsonProperty("grandfathered_cost_apply")
        private boolean grandfatheredCostApply;

        @JsonProperty("redeemed_units")
        private Double redeemedUnits;

        @JsonProperty("redeemed_nav")
        private Double redeemedNav;

        @JsonProperty("no_of_days")
        private Integer noOfDays;

        @JsonProperty("transaction_count")
        private Integer transactionCount;

        // String display fields
        @JsonProperty("puramt_str")
        private String puramtStr;

        @JsonProperty("soldamt_str")
        private String soldamtStr;

        @JsonProperty("gainloss_str")
        private String gainlossStr;

        @JsonProperty("whatifamount_str")
        private String whatifamountStr;

        @JsonProperty("purchase_amount_str")
        private String purchaseAmountStr;

        @JsonProperty("sold_amount_str")
        private String soldAmountStr;

        @JsonProperty("gain_loss_str")
        private String gainLossStr;

        @JsonProperty("sold_units_str")
        private String soldUnitsStr;

        @JsonProperty("purchase_units_str")
        private String purchaseUnitsStr;

        @JsonProperty("redeemed_units_str")
        private String redeemedUnitsStr;

        @JsonProperty("ltg_gain_str")
        private String ltgGainStr;

        @JsonProperty("purchase_date_str")
        private String purchaseDateStr;

        @JsonProperty("sold_date_str")
        private String soldDateStr;

        /** Individual capital-gain rows for this scheme+folio */
        @JsonProperty("transaction_list")
        private List<CapitalGainTransactionDto> transactionList;

        // ── Getters & Setters ─────────────────────────────────────────────────

        public String getScheme() { return scheme; }
        public void setScheme(String v) { this.scheme = v; }

        public String getSchemeAmfiCode() { return schemeAmfiCode; }
        public void setSchemeAmfiCode(String v) { this.schemeAmfiCode = v; }

        public String getSchemeAmfiShortName() { return schemeAmfiShortName; }
        public void setSchemeAmfiShortName(String v) { this.schemeAmfiShortName = v; }

        public String getSchemeCode() { return schemeCode; }
        public void setSchemeCode(String v) { this.schemeCode = v; }

        public String getSchemeRegistrar() { return schemeRegistrar; }
        public void setSchemeRegistrar(String v) { this.schemeRegistrar = v; }

        public String getIsinNo() { return isinNo; }
        public void setIsinNo(String v) { this.isinNo = v; }

        public String getFolio() { return folio; }
        public void setFolio(String v) { this.folio = v; }

        public String getCategory() { return category; }
        public void setCategory(String v) { this.category = v; }

        public String getAmcLogo() { return amcLogo; }
        public void setAmcLogo(String v) { this.amcLogo = v; }

        public Double getSoldUnits() { return soldUnits; }
        public void setSoldUnits(Double v) { this.soldUnits = v; }

        public Double getSoldAmount() { return soldAmount; }
        public void setSoldAmount(Double v) { this.soldAmount = v; }

        public Double getPurchaseUnits() { return purchaseUnits; }
        public void setPurchaseUnits(Double v) { this.purchaseUnits = v; }

        public Double getPurchaseAmount() { return purchaseAmount; }
        public void setPurchaseAmount(Double v) { this.purchaseAmount = v; }

        public Double getStgPurchaseAmount() { return stgPurchaseAmount; }
        public void setStgPurchaseAmount(Double v) { this.stgPurchaseAmount = v; }

        public Double getStgSoldAmount() { return stgSoldAmount; }
        public void setStgSoldAmount(Double v) { this.stgSoldAmount = v; }

        public Double getLtgPurchaseAmount() { return ltgPurchaseAmount; }
        public void setLtgPurchaseAmount(Double v) { this.ltgPurchaseAmount = v; }

        public Double getLtgSoldAmount() { return ltgSoldAmount; }
        public void setLtgSoldAmount(Double v) { this.ltgSoldAmount = v; }

        public Double getStgTotalGain() { return stgTotalGain; }
        public void setStgTotalGain(Double v) { this.stgTotalGain = v; }

        public Double getStgGain() { return stgGain; }
        public void setStgGain(Double v) { this.stgGain = v; }

        public Double getLtgTotalGain() { return ltgTotalGain; }
        public void setLtgTotalGain(Double v) { this.ltgTotalGain = v; }

        public Double getLtgGain() { return ltgGain; }
        public void setLtgGain(Double v) { this.ltgGain = v; }

        public Double getStgLoss() { return stgLoss; }
        public void setStgLoss(Double v) { this.stgLoss = v; }

        public Double getLtgLoss() { return ltgLoss; }
        public void setLtgLoss(Double v) { this.ltgLoss = v; }

        public Double getIndexedCost() { return indexedCost; }
        public void setIndexedCost(Double v) { this.indexedCost = v; }

        public Double getIndexedTotalGain() { return indexedTotalGain; }
        public void setIndexedTotalGain(Double v) { this.indexedTotalGain = v; }

        public Double getIndexedGain() { return indexedGain; }
        public void setIndexedGain(Double v) { this.indexedGain = v; }

        public Double getIndexedLoss() { return indexedLoss; }
        public void setIndexedLoss(Double v) { this.indexedLoss = v; }

        public Double getIndexedSoldAmount() { return indexedSoldAmount; }
        public void setIndexedSoldAmount(Double v) { this.indexedSoldAmount = v; }

        public Double getTotalTax() { return totalTax; }
        public void setTotalTax(Double v) { this.totalTax = v; }

        public Double getStt() { return stt; }
        public void setStt(Double v) { this.stt = v; }

        public Double getGainLoss() { return gainLoss; }
        public void setGainLoss(Double v) { this.gainLoss = v; }

        public Double getGrandfatheredUnits() { return grandfatheredUnits; }
        public void setGrandfatheredUnits(Double v) { this.grandfatheredUnits = v; }

        public Double getGrandfatheredAmount() { return grandfatheredAmount; }
        public void setGrandfatheredAmount(Double v) { this.grandfatheredAmount = v; }

        public Double getGrandfatheredNav() { return grandfatheredNav; }
        public void setGrandfatheredNav(Double v) { this.grandfatheredNav = v; }

        public Double getGrandfatheredPurchaseAmount() { return grandfatheredPurchaseAmount; }
        public void setGrandfatheredPurchaseAmount(Double v) { this.grandfatheredPurchaseAmount = v; }

        public Double getGrandfatheredSoldAmount() { return grandfatheredSoldAmount; }
        public void setGrandfatheredSoldAmount(Double v) { this.grandfatheredSoldAmount = v; }

        public Double getNonGrandfatheredPurchaseAmount() { return nonGrandfatheredPurchaseAmount; }
        public void setNonGrandfatheredPurchaseAmount(Double v) { this.nonGrandfatheredPurchaseAmount = v; }

        public Double getNonGrandfatheredSoldAmount() { return nonGrandfatheredSoldAmount; }
        public void setNonGrandfatheredSoldAmount(Double v) { this.nonGrandfatheredSoldAmount = v; }

        public boolean isGrandfatheredCostApply() { return grandfatheredCostApply; }
        public void setGrandfatheredCostApply(boolean v) { this.grandfatheredCostApply = v; }

        public Double getRedeemedUnits() { return redeemedUnits; }
        public void setRedeemedUnits(Double v) { this.redeemedUnits = v; }

        public Double getRedeemedNav() { return redeemedNav; }
        public void setRedeemedNav(Double v) { this.redeemedNav = v; }

        public Integer getNoOfDays() { return noOfDays; }
        public void setNoOfDays(Integer v) { this.noOfDays = v; }

        public Integer getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Integer v) { this.transactionCount = v; }

        public String getPuramtStr() { return puramtStr; }
        public void setPuramtStr(String v) { this.puramtStr = v; }

        public String getSoldamtStr() { return soldamtStr; }
        public void setSoldamtStr(String v) { this.soldamtStr = v; }

        public String getGainlossStr() { return gainlossStr; }
        public void setGainlossStr(String v) { this.gainlossStr = v; }

        public String getWhatifamountStr() { return whatifamountStr; }
        public void setWhatifamountStr(String v) { this.whatifamountStr = v; }

        public String getPurchaseAmountStr() { return purchaseAmountStr; }
        public void setPurchaseAmountStr(String v) { this.purchaseAmountStr = v; }

        public String getSoldAmountStr() { return soldAmountStr; }
        public void setSoldAmountStr(String v) { this.soldAmountStr = v; }

        public String getGainLossStr() { return gainLossStr; }
        public void setGainLossStr(String v) { this.gainLossStr = v; }

        public String getSoldUnitsStr() { return soldUnitsStr; }
        public void setSoldUnitsStr(String v) { this.soldUnitsStr = v; }

        public String getPurchaseUnitsStr() { return purchaseUnitsStr; }
        public void setPurchaseUnitsStr(String v) { this.purchaseUnitsStr = v; }

        public String getRedeemedUnitsStr() { return redeemedUnitsStr; }
        public void setRedeemedUnitsStr(String v) { this.redeemedUnitsStr = v; }

        public String getLtgGainStr() { return ltgGainStr; }
        public void setLtgGainStr(String v) { this.ltgGainStr = v; }

        public String getPurchaseDateStr() { return purchaseDateStr; }
        public void setPurchaseDateStr(String v) { this.purchaseDateStr = v; }

        public String getSoldDateStr() { return soldDateStr; }
        public void setSoldDateStr(String v) { this.soldDateStr = v; }

        public List<CapitalGainTransactionDto> getTransactionList() { return transactionList; }
        public void setTransactionList(List<CapitalGainTransactionDto> v) { this.transactionList = v; }
    }

    // =========================================================================
    // INNER: Individual capital-gain row (inside transaction_list)
    // Mirrors InvestorSchemeWiseTransactionTaxReport exactly
    // =========================================================================
    public static class CapitalGainTransactionDto {

        @JsonProperty("scheme")
        private String scheme;

        @JsonProperty("scheme_amfi_short_name")
        private String schemeAmfiShortName;

        @JsonProperty("scheme_code")
        private String schemeCode;

        @JsonProperty("isin_no")
        private String isinNo;

        @JsonProperty("folio")
        private String folio;

        @JsonProperty("category")
        private String category;

        @JsonProperty("sold_date")
        private Object soldDate;

        @JsonProperty("sold_repeated_date")
        private Object soldRepeatedDate;

        @JsonProperty("sold_units")
        private Double soldUnits;

        @JsonProperty("sold_amount")
        private Double soldAmount;

        @JsonProperty("sold_nav")
        private Double soldNav;

        @JsonProperty("purchase_date")
        private Object purchaseDate;

        @JsonProperty("purchase_units")
        private Double purchaseUnits;

        @JsonProperty("purchase_amount")
        private Double purchaseAmount;

        @JsonProperty("purchase_nav")
        private Double purchaseNav;

        @JsonProperty("no_of_days")
        private Integer noOfDays;

        @JsonProperty("stg_gain")
        private Double stgGain;

        @JsonProperty("ltg_gain")
        private Double ltgGain;

        @JsonProperty("stg_purchase_amount")
        private Double stgPurchaseAmount;

        @JsonProperty("stg_sold_amount")
        private Double stgSoldAmount;

        @JsonProperty("ltg_purchase_amount")
        private Double ltgPurchaseAmount;

        @JsonProperty("ltg_sold_amount")
        private Double ltgSoldAmount;

        @JsonProperty("stg_total_gain")
        private Double stgTotalGain;

        @JsonProperty("ltg_total_gain")
        private Double ltgTotalGain;

        @JsonProperty("stg_loss")
        private Double stgLoss;

        @JsonProperty("ltg_loss")
        private Double ltgLoss;

        @JsonProperty("indexed_cost")
        private Double indexedCost;

        @JsonProperty("indexed_sold_amount")
        private Double indexedSoldAmount;

        @JsonProperty("indexed_total_gain")
        private Double indexedTotalGain;

        @JsonProperty("indexed_gain")
        private Double indexedGain;

        @JsonProperty("indexed_loss")
        private Double indexedLoss;

        @JsonProperty("total_tax")
        private Double totalTax;

        @JsonProperty("stt")
        private Double stt;

        @JsonProperty("sold_transaction_type")
        private String soldTransactionType;

        @JsonProperty("purchase_transaction_type")
        private String purchaseTransactionType;

        @JsonProperty("redeemed_units")
        private Double redeemedUnits;

        @JsonProperty("redeemed_nav")
        private Double redeemedNav;

        @JsonProperty("grandfathered_units")
        private Double grandfatheredUnits;

        @JsonProperty("grandfathered_amount")
        private Double grandfatheredAmount;

        @JsonProperty("grandfathered_nav")
        private Double grandfatheredNav;

        @JsonProperty("grandfathered_purchase_amount")
        private Double grandfatheredPurchaseAmount;

        @JsonProperty("grandfathered_sold_amount")
        private Double grandfatheredSoldAmount;

        @JsonProperty("non_grandfathered_purchase_amount")
        private Double nonGrandfatheredPurchaseAmount;

        @JsonProperty("non_grandfathered_sold_amount")
        private Double nonGrandfatheredSoldAmount;

        @JsonProperty("grandfathered_cost_apply")
        private boolean grandfatheredCostApply;

        @JsonProperty("actual_amount")
        private Double actualAmount;

        @JsonProperty("gain_loss")
        private Double gainLoss;

        @JsonProperty("amc_logo")
        private String amcLogo;

        @JsonProperty("purchase_date_str")
        private String purchaseDateStr;

        @JsonProperty("sold_date_str")
        private String soldDateStr;

        @JsonProperty("ltg_gain_str")
        private String ltgGainStr;

        @JsonProperty("gain_loss_str")
        private String gainLossStr;

        @JsonProperty("sold_units_str")
        private String soldUnitsStr;

        @JsonProperty("purchase_units_str")
        private String purchaseUnitsStr;

        @JsonProperty("redeemed_units_str")
        private String redeemedUnitsStr;

        @JsonProperty("purchase_amount_str")
        private String purchaseAmountStr;

        @JsonProperty("sold_amount_str")
        private String soldAmountStr;

        // ── Getters & Setters (abbreviated — add full set as needed) ──────────

        public String getScheme() { return scheme; }
        public void setScheme(String v) { this.scheme = v; }

        public String getSchemeAmfiShortName() { return schemeAmfiShortName; }
        public void setSchemeAmfiShortName(String v) { this.schemeAmfiShortName = v; }

        public String getSchemeCode() { return schemeCode; }
        public void setSchemeCode(String v) { this.schemeCode = v; }

        public String getIsinNo() { return isinNo; }
        public void setIsinNo(String v) { this.isinNo = v; }

        public String getFolio() { return folio; }
        public void setFolio(String v) { this.folio = v; }

        public String getCategory() { return category; }
        public void setCategory(String v) { this.category = v; }

        public Object getSoldDate() { return soldDate; }
        public void setSoldDate(Object v) { this.soldDate = v; }

        public Object getSoldRepeatedDate() { return soldRepeatedDate; }
        public void setSoldRepeatedDate(Object v) { this.soldRepeatedDate = v; }

        public Double getSoldUnits() { return soldUnits; }
        public void setSoldUnits(Double v) { this.soldUnits = v; }

        public Double getSoldAmount() { return soldAmount; }
        public void setSoldAmount(Double v) { this.soldAmount = v; }

        public Double getSoldNav() { return soldNav; }
        public void setSoldNav(Double v) { this.soldNav = v; }

        public Object getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(Object v) { this.purchaseDate = v; }

        public Double getPurchaseUnits() { return purchaseUnits; }
        public void setPurchaseUnits(Double v) { this.purchaseUnits = v; }

        public Double getPurchaseAmount() { return purchaseAmount; }
        public void setPurchaseAmount(Double v) { this.purchaseAmount = v; }

        public Double getPurchaseNav() { return purchaseNav; }
        public void setPurchaseNav(Double v) { this.purchaseNav = v; }

        public Integer getNoOfDays() { return noOfDays; }
        public void setNoOfDays(Integer v) { this.noOfDays = v; }

        public Double getStgGain() { return stgGain; }
        public void setStgGain(Double v) { this.stgGain = v; }

        public Double getLtgGain() { return ltgGain; }
        public void setLtgGain(Double v) { this.ltgGain = v; }

        public Double getStgPurchaseAmount() { return stgPurchaseAmount; }
        public void setStgPurchaseAmount(Double v) { this.stgPurchaseAmount = v; }

        public Double getStgSoldAmount() { return stgSoldAmount; }
        public void setStgSoldAmount(Double v) { this.stgSoldAmount = v; }

        public Double getLtgPurchaseAmount() { return ltgPurchaseAmount; }
        public void setLtgPurchaseAmount(Double v) { this.ltgPurchaseAmount = v; }

        public Double getLtgSoldAmount() { return ltgSoldAmount; }
        public void setLtgSoldAmount(Double v) { this.ltgSoldAmount = v; }

        public Double getStgTotalGain() { return stgTotalGain; }
        public void setStgTotalGain(Double v) { this.stgTotalGain = v; }

        public Double getLtgTotalGain() { return ltgTotalGain; }
        public void setLtgTotalGain(Double v) { this.ltgTotalGain = v; }

        public Double getStgLoss() { return stgLoss; }
        public void setStgLoss(Double v) { this.stgLoss = v; }

        public Double getLtgLoss() { return ltgLoss; }
        public void setLtgLoss(Double v) { this.ltgLoss = v; }

        public Double getIndexedCost() { return indexedCost; }
        public void setIndexedCost(Double v) { this.indexedCost = v; }

        public Double getIndexedSoldAmount() { return indexedSoldAmount; }
        public void setIndexedSoldAmount(Double v) { this.indexedSoldAmount = v; }

        public Double getIndexedTotalGain() { return indexedTotalGain; }
        public void setIndexedTotalGain(Double v) { this.indexedTotalGain = v; }

        public Double getIndexedGain() { return indexedGain; }
        public void setIndexedGain(Double v) { this.indexedGain = v; }

        public Double getIndexedLoss() { return indexedLoss; }
        public void setIndexedLoss(Double v) { this.indexedLoss = v; }

        public Double getTotalTax() { return totalTax; }
        public void setTotalTax(Double v) { this.totalTax = v; }

        public Double getStt() { return stt; }
        public void setStt(Double v) { this.stt = v; }

        public String getSoldTransactionType() { return soldTransactionType; }
        public void setSoldTransactionType(String v) { this.soldTransactionType = v; }

        public String getPurchaseTransactionType() { return purchaseTransactionType; }
        public void setPurchaseTransactionType(String v) { this.purchaseTransactionType = v; }

        public Double getRedeemedUnits() { return redeemedUnits; }
        public void setRedeemedUnits(Double v) { this.redeemedUnits = v; }

        public Double getRedeemedNav() { return redeemedNav; }
        public void setRedeemedNav(Double v) { this.redeemedNav = v; }

        public Double getGrandfatheredUnits() { return grandfatheredUnits; }
        public void setGrandfatheredUnits(Double v) { this.grandfatheredUnits = v; }

        public Double getGrandfatheredAmount() { return grandfatheredAmount; }
        public void setGrandfatheredAmount(Double v) { this.grandfatheredAmount = v; }

        public Double getGrandfatheredNav() { return grandfatheredNav; }
        public void setGrandfatheredNav(Double v) { this.grandfatheredNav = v; }

        public Double getGrandfatheredPurchaseAmount() { return grandfatheredPurchaseAmount; }
        public void setGrandfatheredPurchaseAmount(Double v) { this.grandfatheredPurchaseAmount = v; }

        public Double getGrandfatheredSoldAmount() { return grandfatheredSoldAmount; }
        public void setGrandfatheredSoldAmount(Double v) { this.grandfatheredSoldAmount = v; }

        public Double getNonGrandfatheredPurchaseAmount() { return nonGrandfatheredPurchaseAmount; }
        public void setNonGrandfatheredPurchaseAmount(Double v) { this.nonGrandfatheredPurchaseAmount = v; }

        public Double getNonGrandfatheredSoldAmount() { return nonGrandfatheredSoldAmount; }
        public void setNonGrandfatheredSoldAmount(Double v) { this.nonGrandfatheredSoldAmount = v; }

        public boolean isGrandfatheredCostApply() { return grandfatheredCostApply; }
        public void setGrandfatheredCostApply(boolean v) { this.grandfatheredCostApply = v; }

        public Double getActualAmount() { return actualAmount; }
        public void setActualAmount(Double v) { this.actualAmount = v; }

        public Double getGainLoss() { return gainLoss; }
        public void setGainLoss(Double v) { this.gainLoss = v; }

        public String getAmcLogo() { return amcLogo; }
        public void setAmcLogo(String v) { this.amcLogo = v; }

        public String getPurchaseDateStr() { return purchaseDateStr; }
        public void setPurchaseDateStr(String v) { this.purchaseDateStr = v; }

        public String getSoldDateStr() { return soldDateStr; }
        public void setSoldDateStr(String v) { this.soldDateStr = v; }

        public String getLtgGainStr() { return ltgGainStr; }
        public void setLtgGainStr(String v) { this.ltgGainStr = v; }

        public String getGainLossStr() { return gainLossStr; }
        public void setGainLossStr(String v) { this.gainLossStr = v; }

        public String getSoldUnitsStr() { return soldUnitsStr; }
        public void setSoldUnitsStr(String v) { this.soldUnitsStr = v; }

        public String getPurchaseUnitsStr() { return purchaseUnitsStr; }
        public void setPurchaseUnitsStr(String v) { this.purchaseUnitsStr = v; }

        public String getRedeemedUnitsStr() { return redeemedUnitsStr; }
        public void setRedeemedUnitsStr(String v) { this.redeemedUnitsStr = v; }

        public String getPurchaseAmountStr() { return purchaseAmountStr; }
        public void setPurchaseAmountStr(String v) { this.purchaseAmountStr = v; }

        public String getSoldAmountStr() { return soldAmountStr; }
        public void setSoldAmountStr(String v) { this.soldAmountStr = v; }
    }
}
