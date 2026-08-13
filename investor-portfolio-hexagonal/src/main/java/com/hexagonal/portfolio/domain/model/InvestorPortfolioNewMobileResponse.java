package com.hexagonal.portfolio.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mobile-specific response for /getInvestorPortfolioNew (source=mobile).
 * Structure: portfolio summary + list(broad_category → category_list → scheme_list).
 */
public class InvestorPortfolioNewMobileResponse {

    @JsonProperty("status")
    private int status;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("current_cost")
    private Double currentCost;

    @JsonProperty("current_value")
    private Double currentValue;

    @JsonProperty("unrealised_gain")
    private Double unrealisedGain;

    @JsonProperty("realised_gain")
    private Double realisedGain;

    @JsonProperty("absolute_return")
    private Double absoluteReturn;

    @JsonProperty("portfolio_weight")
    private Double portfolioWeight;

    @JsonProperty("xirr")
    private Double xirr;

    @JsonProperty("list")
    private List<BroadCategoryDto> list;

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getStatusMsg() { return statusMsg; }
    public void setStatusMsg(String statusMsg) { this.statusMsg = statusMsg; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public Double getCurrentCost() { return currentCost; }
    public void setCurrentCost(Double currentCost) { this.currentCost = currentCost; }

    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

    public Double getUnrealisedGain() { return unrealisedGain; }
    public void setUnrealisedGain(Double unrealisedGain) { this.unrealisedGain = unrealisedGain; }

    public Double getRealisedGain() { return realisedGain; }
    public void setRealisedGain(Double realisedGain) { this.realisedGain = realisedGain; }

    public Double getAbsoluteReturn() { return absoluteReturn; }
    public void setAbsoluteReturn(Double absoluteReturn) { this.absoluteReturn = absoluteReturn; }

    public Double getPortfolioWeight() { return portfolioWeight; }
    public void setPortfolioWeight(Double portfolioWeight) { this.portfolioWeight = portfolioWeight; }

    public Double getXirr() { return xirr; }
    public void setXirr(Double xirr) { this.xirr = xirr; }

    public List<BroadCategoryDto> getList() { return list; }
    public void setList(List<BroadCategoryDto> list) { this.list = list; }

    // ── Inner DTOs ───────────────────────────────────────────────────────────

    public static class BroadCategoryDto {
        @JsonProperty("broad_category")
        private String broadCategory;

        @JsonProperty("current_cost")
        private Double currentCost;

        @JsonProperty("current_value")
        private Double currentValue;

        @JsonProperty("unrealised_gain")
        private Double unrealisedGain;

        @JsonProperty("realised_gain")
        private Double realisedGain;

        @JsonProperty("abs_rtn")
        private Double absRtn;

        @JsonProperty("portfolio_weight")
        private Double portfolioWeight;

        @JsonProperty("category_list")
        private List<CategoryDto> categoryList;

        public String getBroadCategory() { return broadCategory; }
        public void setBroadCategory(String broadCategory) { this.broadCategory = broadCategory; }

        public Double getCurrentCost() { return currentCost; }
        public void setCurrentCost(Double currentCost) { this.currentCost = currentCost; }

        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

        public Double getUnrealisedGain() { return unrealisedGain; }
        public void setUnrealisedGain(Double unrealisedGain) { this.unrealisedGain = unrealisedGain; }

        public Double getRealisedGain() { return realisedGain; }
        public void setRealisedGain(Double realisedGain) { this.realisedGain = realisedGain; }

        public Double getAbsRtn() { return absRtn; }
        public void setAbsRtn(Double absRtn) { this.absRtn = absRtn; }

        public Double getPortfolioWeight() { return portfolioWeight; }
        public void setPortfolioWeight(Double portfolioWeight) { this.portfolioWeight = portfolioWeight; }

        public List<CategoryDto> getCategoryList() { return categoryList; }
        public void setCategoryList(List<CategoryDto> categoryList) { this.categoryList = categoryList; }
    }

    public static class CategoryDto {
        @JsonProperty("category")
        private String category;

        @JsonProperty("current_cost")
        private Double currentCost;

        @JsonProperty("current_value")
        private Double currentValue;

        @JsonProperty("unrealised_gain")
        private Double unrealisedGain;

        @JsonProperty("realised_gain")
        private Double realisedGain;

        @JsonProperty("abs_rtn")
        private Double absRtn;

        @JsonProperty("portfolio_weight")
        private Double portfolioWeight;

        @JsonProperty("scheme_list")
        private List<SchemeDto> schemeList;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Double getCurrentCost() { return currentCost; }
        public void setCurrentCost(Double currentCost) { this.currentCost = currentCost; }

        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

        public Double getUnrealisedGain() { return unrealisedGain; }
        public void setUnrealisedGain(Double unrealisedGain) { this.unrealisedGain = unrealisedGain; }

        public Double getRealisedGain() { return realisedGain; }
        public void setRealisedGain(Double realisedGain) { this.realisedGain = realisedGain; }

        public Double getAbsRtn() { return absRtn; }
        public void setAbsRtn(Double absRtn) { this.absRtn = absRtn; }

        public Double getPortfolioWeight() { return portfolioWeight; }
        public void setPortfolioWeight(Double portfolioWeight) { this.portfolioWeight = portfolioWeight; }

        public List<SchemeDto> getSchemeList() { return schemeList; }
        public void setSchemeList(List<SchemeDto> schemeList) { this.schemeList = schemeList; }
    }

    public static class SchemeDto {
        @JsonProperty("scheme_amfi")
        private String schemeAmfi;

        @JsonProperty("scheme_amfi_short_name")
        private String schemeAmfiShortName;

        @JsonProperty("logo")
        private String logo;

        @JsonProperty("investor_name")
        private String investorName;

        @JsonProperty("folio_no")
        private String folioNo;

        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("units")
        private Double units;

        @JsonProperty("avg_nav")
        private Double avgNav;

        @JsonProperty("current_cost")
        private Double currentCost;

        @JsonProperty("dividend")
        private Double dividend;

        @JsonProperty("latest_nav")
        private Double latestNav;

        @JsonProperty("nav_date")
        private String navDate;

        @JsonProperty("current_value")
        private Double currentValue;

        @JsonProperty("unrealised_gain")
        private Double unrealisedGain;

        @JsonProperty("realised_gain")
        private Double realisedGain;

        @JsonProperty("abs_rtn")
        private Double absRtn;

        @JsonProperty("scheme_weight")
        private Double schemeWeight;

        @JsonProperty("xirr")
        private Double xirr;

        public String getSchemeAmfi() { return schemeAmfi; }
        public void setSchemeAmfi(String schemeAmfi) { this.schemeAmfi = schemeAmfi; }

        public String getSchemeAmfiShortName() { return schemeAmfiShortName; }
        public void setSchemeAmfiShortName(String schemeAmfiShortName) { this.schemeAmfiShortName = schemeAmfiShortName; }

        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }

        public String getInvestorName() { return investorName; }
        public void setInvestorName(String investorName) { this.investorName = investorName; }

        public String getFolioNo() { return folioNo; }
        public void setFolioNo(String folioNo) { this.folioNo = folioNo; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public Double getUnits() { return units; }
        public void setUnits(Double units) { this.units = units; }

        public Double getAvgNav() { return avgNav; }
        public void setAvgNav(Double avgNav) { this.avgNav = avgNav; }

        public Double getCurrentCost() { return currentCost; }
        public void setCurrentCost(Double currentCost) { this.currentCost = currentCost; }

        public Double getDividend() { return dividend; }
        public void setDividend(Double dividend) { this.dividend = dividend; }

        public Double getLatestNav() { return latestNav; }
        public void setLatestNav(Double latestNav) { this.latestNav = latestNav; }

        public String getNavDate() { return navDate; }
        public void setNavDate(String navDate) { this.navDate = navDate; }

        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

        public Double getUnrealisedGain() { return unrealisedGain; }
        public void setUnrealisedGain(Double unrealisedGain) { this.unrealisedGain = unrealisedGain; }

        public Double getRealisedGain() { return realisedGain; }
        public void setRealisedGain(Double realisedGain) { this.realisedGain = realisedGain; }

        public Double getAbsRtn() { return absRtn; }
        public void setAbsRtn(Double absRtn) { this.absRtn = absRtn; }

        public Double getSchemeWeight() { return schemeWeight; }
        public void setSchemeWeight(Double schemeWeight) { this.schemeWeight = schemeWeight; }

        public Double getXirr() { return xirr; }
        public void setXirr(Double xirr) { this.xirr = xirr; }
    }
}
