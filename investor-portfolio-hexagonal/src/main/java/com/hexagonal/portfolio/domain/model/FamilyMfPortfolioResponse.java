package com.hexagonal.portfolio.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Family-grouped response for /getInvestorPortfolioNew?source=family.
 *
 * Shape:
 *   status / status_msg / msg
 *   mf_summary            → family-level MF totals
 *   sip_summary           → family-level SIP totals
 *   mf_scheme_summary[]   → one entry per family member (all holdings)
 *   sip_scheme_summary[]  → one entry per family member that has active-SIP holdings
 */
public class FamilyMfPortfolioResponse {

    @JsonProperty("status")
    private int status;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("mf_summary")
    private MfSummary mfSummary;

    @JsonProperty("sip_summary")
    private SipSummary sipSummary;

    @JsonProperty("mf_scheme_summary")
    private List<MemberMfSummary> mfSchemeSummary;

    @JsonProperty("sip_scheme_summary")
    private List<MemberSipSummary> sipSchemeSummary;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getStatusMsg() { return statusMsg; }
    public void setStatusMsg(String statusMsg) { this.statusMsg = statusMsg; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public MfSummary getMfSummary() { return mfSummary; }
    public void setMfSummary(MfSummary mfSummary) { this.mfSummary = mfSummary; }

    public SipSummary getSipSummary() { return sipSummary; }
    public void setSipSummary(SipSummary sipSummary) { this.sipSummary = sipSummary; }

    public List<MemberMfSummary> getMfSchemeSummary() { return mfSchemeSummary; }
    public void setMfSchemeSummary(List<MemberMfSummary> mfSchemeSummary) { this.mfSchemeSummary = mfSchemeSummary; }

    public List<MemberSipSummary> getSipSchemeSummary() { return sipSchemeSummary; }
    public void setSipSchemeSummary(List<MemberSipSummary> sipSchemeSummary) { this.sipSchemeSummary = sipSchemeSummary; }

    // ── Family-level MF summary ───────────────────────────────────────────────
    public static class MfSummary {
        @JsonProperty("total_curr_cost")        private double totalCurrCost;
        @JsonProperty("total_curr_value")       private double totalCurrValue;
        @JsonProperty("total_div_reinv")        private double totalDivReinv;
        @JsonProperty("total_div_paid")         private double totalDivPaid;
        @JsonProperty("total_unrealised_gain")  private double totalUnrealisedGain;
        @JsonProperty("total_realised_gain")    private double totalRealisedGain;
        @JsonProperty("total_abs_rtn")          private double totalAbsRtn;
        @JsonProperty("total_xirr")             private double totalXirr;
        @JsonProperty("day_change_value")       private double dayChangeValue;
        @JsonProperty("day_change_percentage")  private double dayChangePercentage;

        public double getTotalCurrCost() { return totalCurrCost; }
        public void setTotalCurrCost(double v) { this.totalCurrCost = v; }
        public double getTotalCurrValue() { return totalCurrValue; }
        public void setTotalCurrValue(double v) { this.totalCurrValue = v; }
        public double getTotalDivReinv() { return totalDivReinv; }
        public void setTotalDivReinv(double v) { this.totalDivReinv = v; }
        public double getTotalDivPaid() { return totalDivPaid; }
        public void setTotalDivPaid(double v) { this.totalDivPaid = v; }
        public double getTotalUnrealisedGain() { return totalUnrealisedGain; }
        public void setTotalUnrealisedGain(double v) { this.totalUnrealisedGain = v; }
        public double getTotalRealisedGain() { return totalRealisedGain; }
        public void setTotalRealisedGain(double v) { this.totalRealisedGain = v; }
        public double getTotalAbsRtn() { return totalAbsRtn; }
        public void setTotalAbsRtn(double v) { this.totalAbsRtn = v; }
        public double getTotalXirr() { return totalXirr; }
        public void setTotalXirr(double v) { this.totalXirr = v; }
        public double getDayChangeValue() { return dayChangeValue; }
        public void setDayChangeValue(double v) { this.dayChangeValue = v; }
        public double getDayChangePercentage() { return dayChangePercentage; }
        public void setDayChangePercentage(double v) { this.dayChangePercentage = v; }
    }

    // ── Family-level SIP summary ──────────────────────────────────────────────
    public static class SipSummary {
        @JsonProperty("sip_curr_value")             private double sipCurrValue;
        @JsonProperty("sip_curr_cost")              private double sipCurrCost;
        @JsonProperty("sip_amount")                 private double sipAmount;
        @JsonProperty("sip_count")                  private double sipCount;
        @JsonProperty("sip_xirr")                   private double sipXirr;
        @JsonProperty("sip_day_change_amount")      private double sipDayChangeAmount;
        @JsonProperty("sip_day_change_percentage")  private double sipDayChangePercentage;
        @JsonProperty("sip_gain_loss")              private double sipGainLoss;

        public double getSipCurrValue() { return sipCurrValue; }
        public void setSipCurrValue(double v) { this.sipCurrValue = v; }
        public double getSipCurrCost() { return sipCurrCost; }
        public void setSipCurrCost(double v) { this.sipCurrCost = v; }
        public double getSipAmount() { return sipAmount; }
        public void setSipAmount(double v) { this.sipAmount = v; }
        public double getSipCount() { return sipCount; }
        public void setSipCount(double v) { this.sipCount = v; }
        public double getSipXirr() { return sipXirr; }
        public void setSipXirr(double v) { this.sipXirr = v; }
        public double getSipDayChangeAmount() { return sipDayChangeAmount; }
        public void setSipDayChangeAmount(double v) { this.sipDayChangeAmount = v; }
        public double getSipDayChangePercentage() { return sipDayChangePercentage; }
        public void setSipDayChangePercentage(double v) { this.sipDayChangePercentage = v; }
        public double getSipGainLoss() { return sipGainLoss; }
        public void setSipGainLoss(double v) { this.sipGainLoss = v; }
    }

    // ── Per-member MF summary ─────────────────────────────────────────────────
    public static class MemberMfSummary {
        @JsonProperty("user_id")          private Integer userId;
        @JsonProperty("investor_name")    private String investorName;
        @JsonProperty("pan")              private String pan;
        @JsonProperty("salutation")       private String salutation;
        @JsonProperty("family_status")    private String familyStatus;
        @JsonProperty("current_cost")     private double currentCost;
        @JsonProperty("current_value")    private double currentValue;
        @JsonProperty("unrealised_gain")  private double unrealisedGain;
        @JsonProperty("realised_gain")    private double realisedGain;
        @JsonProperty("abs_rtn")          private double absRtn;
        @JsonProperty("xirr")             private double xirr;
        @JsonProperty("scheme_list")      private List<MfSchemeDto> schemeList;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer v) { this.userId = v; }
        public String getInvestorName() { return investorName; }
        public void setInvestorName(String v) { this.investorName = v; }
        public String getPan() { return pan; }
        public void setPan(String v) { this.pan = v; }
        public String getSalutation() { return salutation; }
        public void setSalutation(String v) { this.salutation = v; }
        public String getFamilyStatus() { return familyStatus; }
        public void setFamilyStatus(String v) { this.familyStatus = v; }
        public double getCurrentCost() { return currentCost; }
        public void setCurrentCost(double v) { this.currentCost = v; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double v) { this.currentValue = v; }
        public double getUnrealisedGain() { return unrealisedGain; }
        public void setUnrealisedGain(double v) { this.unrealisedGain = v; }
        public double getRealisedGain() { return realisedGain; }
        public void setRealisedGain(double v) { this.realisedGain = v; }
        public double getAbsRtn() { return absRtn; }
        public void setAbsRtn(double v) { this.absRtn = v; }
        public double getXirr() { return xirr; }
        public void setXirr(double v) { this.xirr = v; }
        public List<MfSchemeDto> getSchemeList() { return schemeList; }
        public void setSchemeList(List<MfSchemeDto> v) { this.schemeList = v; }
    }

    // ── Per-member SIP summary ────────────────────────────────────────────────
    public static class MemberSipSummary {
        @JsonProperty("user_id")          private Integer userId;
        @JsonProperty("investor_name")    private String investorName;
        @JsonProperty("pan")              private String pan;
        @JsonProperty("salutation")       private String salutation;
        @JsonProperty("family_status")    private String familyStatus;
        @JsonProperty("sip_amount")       private double sipAmount;
        @JsonProperty("current_cost")     private double currentCost;
        @JsonProperty("current_value")    private double currentValue;
        @JsonProperty("unrealised_gain")  private double unrealisedGain;
        @JsonProperty("realised_gain")    private double realisedGain;
        @JsonProperty("abs_rtn")          private double absRtn;
        @JsonProperty("xirr")             private double xirr;
        @JsonProperty("scheme_list")      private List<SipSchemeDto> schemeList;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer v) { this.userId = v; }
        public String getInvestorName() { return investorName; }
        public void setInvestorName(String v) { this.investorName = v; }
        public String getPan() { return pan; }
        public void setPan(String v) { this.pan = v; }
        public String getSalutation() { return salutation; }
        public void setSalutation(String v) { this.salutation = v; }
        public String getFamilyStatus() { return familyStatus; }
        public void setFamilyStatus(String v) { this.familyStatus = v; }
        public double getSipAmount() { return sipAmount; }
        public void setSipAmount(double v) { this.sipAmount = v; }
        public double getCurrentCost() { return currentCost; }
        public void setCurrentCost(double v) { this.currentCost = v; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double v) { this.currentValue = v; }
        public double getUnrealisedGain() { return unrealisedGain; }
        public void setUnrealisedGain(double v) { this.unrealisedGain = v; }
        public double getRealisedGain() { return realisedGain; }
        public void setRealisedGain(double v) { this.realisedGain = v; }
        public double getAbsRtn() { return absRtn; }
        public void setAbsRtn(double v) { this.absRtn = v; }
        public double getXirr() { return xirr; }
        public void setXirr(double v) { this.xirr = v; }
        public List<SipSchemeDto> getSchemeList() { return schemeList; }
        public void setSchemeList(List<SipSchemeDto> v) { this.schemeList = v; }
    }

    // ── MF scheme row ─────────────────────────────────────────────────────────
    public static class MfSchemeDto {
        @JsonProperty("scheme_amfi")             private String schemeAmfi;
        @JsonProperty("scheme_amfi_code")        private String schemeAmfiCode;
        @JsonProperty("scheme_code")             private String schemeCode;
        @JsonProperty("scheme_amfi_short_name")  private String schemeAmfiShortName;
        @JsonProperty("scheme_amc")              private String schemeAmc;
        @JsonProperty("scheme_amc_code")         private String schemeAmcCode;
        @JsonProperty("scheme_amc_logo")         private String schemeAmcLogo;
        @JsonProperty("scheme_broad_category")   private String schemeBroadCategory;
        @JsonProperty("scheme_category")         private String schemeCategory;
        @JsonProperty("folio")                   private String folio;
        @JsonProperty("folio_desc")              private String folioDesc;
        @JsonProperty("broker_code")             private String brokerCode;
        @JsonProperty("curr_cost")               private double currCost;
        @JsonProperty("curr_value")              private double currValue;
        @JsonProperty("units")                   private double units;
        @JsonProperty("unrealisedProfitLoss")    private double unrealisedProfitLoss;
        @JsonProperty("realisedProfitLoss")      private double realisedProfitLoss;
        @JsonProperty("realisedGainLoss")        private double realisedGainLoss;
        @JsonProperty("day_change_value")        private double dayChangeValue;
        @JsonProperty("day_change_percentage")   private double dayChangePercentage;
        @JsonProperty("absolute_return")         private double absoluteReturn;
        @JsonProperty("xirr")                    private double xirr;
        @JsonProperty("isSipScheme")             private boolean isSipScheme;
        @JsonProperty("isSip")                   private boolean isSip;
        @JsonProperty("isStpInScheme")           private boolean isStpInScheme;
        @JsonProperty("isStpOutScheme")          private boolean isStpOutScheme;
        @JsonProperty("isStp")                   private boolean isStp;
        @JsonProperty("isSwpScheme")             private boolean isSwpScheme;
        @JsonProperty("isSwp")                   private boolean isSwp;
        @JsonProperty("isManualEntry")           private boolean isManualEntry;
        @JsonProperty("manualEntryVal")          private String manualEntryVal;
        @JsonProperty("goal_name")               private String goalName;
        @JsonProperty("free_units")              private double freeUnits;
        @JsonProperty("avg_days")                private Integer avgDays;
        @JsonProperty("latestNav")               private double latestNav;
        @JsonProperty("latestNavDate")           private String latestNavDate;
        @JsonProperty("latestNavDateStr")        private String latestNavDateStr;

        public String getSchemeAmfi() { return schemeAmfi; }
        public void setSchemeAmfi(String v) { this.schemeAmfi = v; }
        public String getSchemeAmfiCode() { return schemeAmfiCode; }
        public void setSchemeAmfiCode(String v) { this.schemeAmfiCode = v; }
        public String getSchemeCode() { return schemeCode; }
        public void setSchemeCode(String v) { this.schemeCode = v; }
        public String getSchemeAmfiShortName() { return schemeAmfiShortName; }
        public void setSchemeAmfiShortName(String v) { this.schemeAmfiShortName = v; }
        public String getSchemeAmc() { return schemeAmc; }
        public void setSchemeAmc(String v) { this.schemeAmc = v; }
        public String getSchemeAmcCode() { return schemeAmcCode; }
        public void setSchemeAmcCode(String v) { this.schemeAmcCode = v; }
        public String getSchemeAmcLogo() { return schemeAmcLogo; }
        public void setSchemeAmcLogo(String v) { this.schemeAmcLogo = v; }
        public String getSchemeBroadCategory() { return schemeBroadCategory; }
        public void setSchemeBroadCategory(String v) { this.schemeBroadCategory = v; }
        public String getSchemeCategory() { return schemeCategory; }
        public void setSchemeCategory(String v) { this.schemeCategory = v; }
        public String getFolio() { return folio; }
        public void setFolio(String v) { this.folio = v; }
        public String getFolioDesc() { return folioDesc; }
        public void setFolioDesc(String v) { this.folioDesc = v; }
        public String getBrokerCode() { return brokerCode; }
        public void setBrokerCode(String v) { this.brokerCode = v; }
        public double getCurrCost() { return currCost; }
        public void setCurrCost(double v) { this.currCost = v; }
        public double getCurrValue() { return currValue; }
        public void setCurrValue(double v) { this.currValue = v; }
        public double getUnits() { return units; }
        public void setUnits(double v) { this.units = v; }
        public double getUnrealisedProfitLoss() { return unrealisedProfitLoss; }
        public void setUnrealisedProfitLoss(double v) { this.unrealisedProfitLoss = v; }
        public double getRealisedProfitLoss() { return realisedProfitLoss; }
        public void setRealisedProfitLoss(double v) { this.realisedProfitLoss = v; }
        public double getRealisedGainLoss() { return realisedGainLoss; }
        public void setRealisedGainLoss(double v) { this.realisedGainLoss = v; }
        public double getDayChangeValue() { return dayChangeValue; }
        public void setDayChangeValue(double v) { this.dayChangeValue = v; }
        public double getDayChangePercentage() { return dayChangePercentage; }
        public void setDayChangePercentage(double v) { this.dayChangePercentage = v; }
        public double getAbsoluteReturn() { return absoluteReturn; }
        public void setAbsoluteReturn(double v) { this.absoluteReturn = v; }
        public double getXirr() { return xirr; }
        public void setXirr(double v) { this.xirr = v; }
        public boolean getIsSipScheme() { return isSipScheme; }
        public void setSipScheme(boolean v) { this.isSipScheme = v; }
        public boolean getIsSip() { return isSip; }
        public void setSip(boolean v) { this.isSip = v; }
        public boolean getIsStpInScheme() { return isStpInScheme; }
        public void setStpInScheme(boolean v) { this.isStpInScheme = v; }
        public boolean getIsStpOutScheme() { return isStpOutScheme; }
        public void setStpOutScheme(boolean v) { this.isStpOutScheme = v; }
        public boolean getIsStp() { return isStp; }
        public void setStp(boolean v) { this.isStp = v; }
        public boolean getIsSwpScheme() { return isSwpScheme; }
        public void setSwpScheme(boolean v) { this.isSwpScheme = v; }
        public boolean getIsSwp() { return isSwp; }
        public void setSwp(boolean v) { this.isSwp = v; }
        public boolean getIsManualEntry() { return isManualEntry; }
        public void setManualEntry(boolean v) { this.isManualEntry = v; }
        public String getManualEntryVal() { return manualEntryVal; }
        public void setManualEntryVal(String v) { this.manualEntryVal = v; }
        public String getGoalName() { return goalName; }
        public void setGoalName(String v) { this.goalName = v; }
        public double getFreeUnits() { return freeUnits; }
        public void setFreeUnits(double v) { this.freeUnits = v; }
        public Integer getAvgDays() { return avgDays; }
        public void setAvgDays(Integer v) { this.avgDays = v; }
        public double getLatestNav() { return latestNav; }
        public void setLatestNav(double v) { this.latestNav = v; }
        public String getLatestNavDate() { return latestNavDate; }
        public void setLatestNavDate(String v) { this.latestNavDate = v; }
        public String getLatestNavDateStr() { return latestNavDateStr; }
        public void setLatestNavDateStr(String v) { this.latestNavDateStr = v; }
    }

    // ── SIP scheme row ────────────────────────────────────────────────────────
    public static class SipSchemeDto {
        @JsonProperty("scheme_amfi")             private String schemeAmfi;
        @JsonProperty("scheme_amfi_code")        private String schemeAmfiCode;
        @JsonProperty("scheme_code")             private String schemeCode;
        @JsonProperty("scheme_amfi_short_name")  private String schemeAmfiShortName;
        @JsonProperty("scheme_amc")              private String schemeAmc;
        @JsonProperty("scheme_amc_code")         private String schemeAmcCode;
        @JsonProperty("scheme_amc_logo")         private String schemeAmcLogo;
        @JsonProperty("scheme_broad_category")   private String schemeBroadCategory;
        @JsonProperty("scheme_category")         private String schemeCategory;
        @JsonProperty("folio")                   private String folio;
        @JsonProperty("broker_code")             private String brokerCode;
        @JsonProperty("sip_start_date")          private String sipStartDate;
        @JsonProperty("sip_end_date")            private String sipEndDate;
        @JsonProperty("sip_ecs_date")            private String sipEcsDate;
        @JsonProperty("sip_frequency")           private String sipFrequency;
        @JsonProperty("sip_amount")              private double sipAmount;
        @JsonProperty("sip_monthly_amount")      private double sipMonthlyAmount;
        @JsonProperty("units")                   private double units;
        @JsonProperty("avg_nav")                 private double avgNav;
        @JsonProperty("curr_cost")               private double currCost;
        @JsonProperty("latest_nav")              private double latestNav;
        @JsonProperty("nav_date")                private String navDate;
        @JsonProperty("curr_value")              private double currValue;
        @JsonProperty("unrealisedProfitLoss")    private double unrealisedProfitLoss;
        @JsonProperty("realisedProfitLoss")      private double realisedProfitLoss;
        @JsonProperty("realisedGainLoss")        private double realisedGainLoss;
        @JsonProperty("day_change_value")        private double dayChangeValue;
        @JsonProperty("day_change_percentage")   private double dayChangePercentage;
        @JsonProperty("absolute_return")         private double absoluteReturn;
        @JsonProperty("xirr")                    private double xirr;

        public String getSchemeAmfi() { return schemeAmfi; }
        public void setSchemeAmfi(String v) { this.schemeAmfi = v; }
        public String getSchemeAmfiCode() { return schemeAmfiCode; }
        public void setSchemeAmfiCode(String v) { this.schemeAmfiCode = v; }
        public String getSchemeCode() { return schemeCode; }
        public void setSchemeCode(String v) { this.schemeCode = v; }
        public String getSchemeAmfiShortName() { return schemeAmfiShortName; }
        public void setSchemeAmfiShortName(String v) { this.schemeAmfiShortName = v; }
        public String getSchemeAmc() { return schemeAmc; }
        public void setSchemeAmc(String v) { this.schemeAmc = v; }
        public String getSchemeAmcCode() { return schemeAmcCode; }
        public void setSchemeAmcCode(String v) { this.schemeAmcCode = v; }
        public String getSchemeAmcLogo() { return schemeAmcLogo; }
        public void setSchemeAmcLogo(String v) { this.schemeAmcLogo = v; }
        public String getSchemeBroadCategory() { return schemeBroadCategory; }
        public void setSchemeBroadCategory(String v) { this.schemeBroadCategory = v; }
        public String getSchemeCategory() { return schemeCategory; }
        public void setSchemeCategory(String v) { this.schemeCategory = v; }
        public String getFolio() { return folio; }
        public void setFolio(String v) { this.folio = v; }
        public String getBrokerCode() { return brokerCode; }
        public void setBrokerCode(String v) { this.brokerCode = v; }
        public String getSipStartDate() { return sipStartDate; }
        public void setSipStartDate(String v) { this.sipStartDate = v; }
        public String getSipEndDate() { return sipEndDate; }
        public void setSipEndDate(String v) { this.sipEndDate = v; }
        public String getSipEcsDate() { return sipEcsDate; }
        public void setSipEcsDate(String v) { this.sipEcsDate = v; }
        public String getSipFrequency() { return sipFrequency; }
        public void setSipFrequency(String v) { this.sipFrequency = v; }
        public double getSipAmount() { return sipAmount; }
        public void setSipAmount(double v) { this.sipAmount = v; }
        public double getSipMonthlyAmount() { return sipMonthlyAmount; }
        public void setSipMonthlyAmount(double v) { this.sipMonthlyAmount = v; }
        public double getUnits() { return units; }
        public void setUnits(double v) { this.units = v; }
        public double getAvgNav() { return avgNav; }
        public void setAvgNav(double v) { this.avgNav = v; }
        public double getCurrCost() { return currCost; }
        public void setCurrCost(double v) { this.currCost = v; }
        public double getLatestNav() { return latestNav; }
        public void setLatestNav(double v) { this.latestNav = v; }
        public String getNavDate() { return navDate; }
        public void setNavDate(String v) { this.navDate = v; }
        public double getCurrValue() { return currValue; }
        public void setCurrValue(double v) { this.currValue = v; }
        public double getUnrealisedProfitLoss() { return unrealisedProfitLoss; }
        public void setUnrealisedProfitLoss(double v) { this.unrealisedProfitLoss = v; }
        public double getRealisedProfitLoss() { return realisedProfitLoss; }
        public void setRealisedProfitLoss(double v) { this.realisedProfitLoss = v; }
        public double getRealisedGainLoss() { return realisedGainLoss; }
        public void setRealisedGainLoss(double v) { this.realisedGainLoss = v; }
        public double getDayChangeValue() { return dayChangeValue; }
        public void setDayChangeValue(double v) { this.dayChangeValue = v; }
        public double getDayChangePercentage() { return dayChangePercentage; }
        public void setDayChangePercentage(double v) { this.dayChangePercentage = v; }
        public double getAbsoluteReturn() { return absoluteReturn; }
        public void setAbsoluteReturn(double v) { this.absoluteReturn = v; }
        public double getXirr() { return xirr; }
        public void setXirr(double v) { this.xirr = v; }
    }
}
