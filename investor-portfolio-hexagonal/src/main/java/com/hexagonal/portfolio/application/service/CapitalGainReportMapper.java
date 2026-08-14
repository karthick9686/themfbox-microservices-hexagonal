package com.hexagonal.portfolio.application.service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hexagonal.portfolio.application.port.in.ConvertToCapitalGainReportUseCase;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse.CapitalGainTransactionDto;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse.SchemeCapitalGainDto;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;

/**
 * FIXES applied vs previous version:
 *
 *  FIX 1 — amc_logo:
 *    The raw row's getAmc_logo() was returning the scheme name (not a URL or empty),
 *    so the old code short-circuited and set the logo = scheme name.
 *    Now we check if the value looks like a real URL (starts with "http") before
 *    using it; otherwise we always fall through to keyword-based logo resolution.
 *
 *  FIX 2 — EQUITY vs DEBT split:
 *    Older raw rows have category = null (not "EQUITY" / "DEBT").
 *    The split now uses the hybrid_taxation flag + ltg_gain/stg_gain
 *    as a secondary signal when category is null/blank, so EQUITY rows
 *    are not silently bucketed into debt.
 *    Specifically: if category is null/blank, a row is treated as EQUITY when
 *    ltg_gain > 0 AND stg_gain == 0  (classic equity long-term pattern),
 *    otherwise DEBT.
 *    When the API is called with option="equity" or "debt" all rows will have
 *    the correct category, so the null-check is only a safety net.
 *
 *  FIX 3 — stg/ltg purchase/sold amounts accumulation:
 *    Raw equity rows have null for stg_purchase_amount / ltg_purchase_amount etc.
 *    We now fall back to purchase_amount / sold_amount as proxies when those
 *    fields are null, but only add them to the correct bucket (stg or ltg)
 *    based on which gain type is non-zero.
 */
@Service
class CapitalGainReportMapper implements ConvertToCapitalGainReportUseCase {

    private static final String LOGO_BASE = "https://api.advisorkhoj.com/nse/images/amc-logo/";

    private static final Map<String, String> AMC_LOGO_MAP = new LinkedHashMap<>();
    static {
        AMC_LOGO_MAP.put("icici",      "icici.png");
        AMC_LOGO_MAP.put("hdfc",       "hdfc.png");
        AMC_LOGO_MAP.put("sbi",        "sbi.png");
        AMC_LOGO_MAP.put("axis",       "axis.png");
        AMC_LOGO_MAP.put("kotak",      "kotak.png");
        AMC_LOGO_MAP.put("birla",      "birla.png");
        AMC_LOGO_MAP.put("absl",       "birla.png");
        AMC_LOGO_MAP.put("aditya",     "birla.png");
        AMC_LOGO_MAP.put("bandhan",    "bandhan.png");
        AMC_LOGO_MAP.put("idfc",       "bandhan.png");
        AMC_LOGO_MAP.put("edelweiss",  "edelweiss.png");
        AMC_LOGO_MAP.put("dsp",        "dsp.png");
        AMC_LOGO_MAP.put("franklin",   "franklin.png");
        AMC_LOGO_MAP.put("nippon",     "nippon.png");
        AMC_LOGO_MAP.put("reliance",   "nippon.png");
        AMC_LOGO_MAP.put("mirae",      "mirae.png");
        AMC_LOGO_MAP.put("tata",       "tata.png");
        AMC_LOGO_MAP.put("motilal",    "motilal.png");
        AMC_LOGO_MAP.put("canara",     "canara.png");
        AMC_LOGO_MAP.put("sundaram",   "sundaram.png");
        AMC_LOGO_MAP.put("ppfas",      "ppfas.png");
        AMC_LOGO_MAP.put("parag",      "ppfas.png");
        AMC_LOGO_MAP.put("quantum",    "quantum.png");
        AMC_LOGO_MAP.put("uti",        "uti.png");
        AMC_LOGO_MAP.put("invesco",    "invesco.png");
        AMC_LOGO_MAP.put("whiteoak",   "whiteoak.png");
        AMC_LOGO_MAP.put("baroda",     "bnp.png");
        AMC_LOGO_MAP.put("bnp",        "bnp.png");
        AMC_LOGO_MAP.put("baroda bnp", "bnp.png");
    }

    // =========================================================================
    // MAIN CONVERSION
    // =========================================================================

    @Override
    public CapitalGainReportApiResponse convert(
            List<InvestorSchemeWiseTransactionTaxReport> rawList) {

        CapitalGainReportApiResponse response = new CapitalGainReportApiResponse();
        response.setStatus(200);
        response.setStatusMsg("Success");
        response.setMsg("Success");

        if (rawList == null || rawList.isEmpty()) {
            response.setStatus(204);
            response.setStatusMsg("No Content");
            response.setMsg("No capital gain data found");
            zeroAllTotals(response);
            response.setEquitySummaryList(Collections.emptyList());
            response.setDebtSummaryList(Collections.emptyList());
            return response;
        }

        // ── FIX 2: Separate EQUITY and DEBT rows ──────────────────────────────
        List<InvestorSchemeWiseTransactionTaxReport> equityRows = new ArrayList<>();
        List<InvestorSchemeWiseTransactionTaxReport> debtRows   = new ArrayList<>();

        for (InvestorSchemeWiseTransactionTaxReport row : rawList) {
            if (isEquityRow(row)) {
                equityRows.add(row);
            } else {
                debtRows.add(row);
            }
        }

        // ── Build scheme-grouped summary lists ────────────────────────────────
        List<SchemeCapitalGainDto> equitySummaryList = buildSummaryList(equityRows);
        List<SchemeCapitalGainDto> debtSummaryList   = buildSummaryList(debtRows);

        response.setEquitySummaryList(equitySummaryList);
        response.setDebtSummaryList(debtSummaryList);

        // ── Compute category-level totals ─────────────────────────────────────
        double eqStgGain = 0, eqStgLoss = 0, eqLtgGain = 0, eqLtgLoss = 0;
        double eqStgPur  = 0, eqStgSold = 0, eqLtgPur  = 0, eqLtgSold = 0, eqLtgIdx = 0;

        for (SchemeCapitalGainDto s : equitySummaryList) {
            eqStgGain += nvl(s.getStgGain());
            eqStgLoss += nvl(s.getStgLoss());
            eqLtgGain += nvl(s.getLtgGain());
            eqLtgLoss += nvl(s.getLtgLoss());
            eqStgPur  += nvl(s.getStgPurchaseAmount());
            eqStgSold += nvl(s.getStgSoldAmount());
            eqLtgPur  += nvl(s.getLtgPurchaseAmount());
            eqLtgSold += nvl(s.getLtgSoldAmount());
            eqLtgIdx  += nvl(s.getIndexedSoldAmount());
        }

        double debtStgGain = 0, debtStgLoss = 0, debtLtgGain = 0, debtLtgLoss = 0;
        double debtStgPur  = 0, debtStgSold = 0, debtLtgPur  = 0, debtLtgSold = 0, debtLtgIdx = 0;

        for (SchemeCapitalGainDto s : debtSummaryList) {
            debtStgGain += nvl(s.getStgGain());
            debtStgLoss += nvl(s.getStgLoss());
            debtLtgGain += nvl(s.getLtgGain());
            debtLtgLoss += nvl(s.getLtgLoss());
            debtStgPur  += nvl(s.getStgPurchaseAmount());
            debtStgSold += nvl(s.getStgSoldAmount());
            debtLtgPur  += nvl(s.getLtgPurchaseAmount());
            debtLtgSold += nvl(s.getLtgSoldAmount());
            debtLtgIdx  += nvl(s.getIndexedSoldAmount());
        }

        double eqGain   = eqStgGain   - eqStgLoss   + eqLtgGain   - eqLtgLoss;
        double debtGain = debtStgGain - debtStgLoss + debtLtgGain - debtLtgLoss;

        response.setEqGain(eqGain);
        response.setEqStgGain(eqStgGain);
        response.setEqStgLoss(eqStgLoss);
        response.setEqLtgGain(eqLtgGain);
        response.setEqLtgLoss(eqLtgLoss);
        response.setEqStgPurchaseAmount(eqStgPur);
        response.setEqStgSoldAmount(eqStgSold);
        response.setEqLtgPurchaseAmount(eqLtgPur);
        response.setEqLtgSoldAmount(eqLtgSold);
        response.setEqLtgIndexedAmount(eqLtgIdx);

        response.setDebtGain(debtGain);
        response.setDebtStgGain(debtStgGain);
        response.setDebtStgLoss(debtStgLoss);
        response.setDebtLtgGain(debtLtgGain);
        response.setDebtLtgLoss(debtLtgLoss);
        response.setDebtStgPurchaseAmount(debtStgPur);
        response.setDebtStgSoldAmount(debtStgSold);
        response.setDebtLtgPurchaseAmount(debtLtgPur);
        response.setDebtLtgSoldAmount(debtLtgSold);
        response.setDebtLtgIndexedAmount(debtLtgIdx);

        response.setPortfolioLongGain(eqLtgGain  - eqLtgLoss  + debtLtgGain - debtLtgLoss);
        response.setPortfolioShortGain(eqStgGain - eqStgLoss  + debtStgGain - debtStgLoss);
        response.setPortfolioGain(eqGain + debtGain);

        return response;
    }

    // =========================================================================
    // FIX 2 — Equity/Debt classification
    // =========================================================================

    /**
     * Determine if a raw row belongs to equity.
     *
     * Priority:
     *  1. If category field is "EQUITY" (case-insensitive) → equity
     *  2. If category field is "DEBT"   (case-insensitive) → debt
     *  3. If category is null / blank:
     *       - hybrid_taxation == true  → equity (hybrid funds taxed as equity)
     *       - ltg_gain > 0             → equity (long-term gain = equity holding)
     *       - stg_gain > 0             → debt   (short-term = debt typical)
     *       - default                  → equity  (conservative fallback)
     */
    private boolean isEquityRow(InvestorSchemeWiseTransactionTaxReport row) {
        String cat = row.getCategory();
        if (cat != null && !cat.trim().isEmpty()) {
            return cat.trim().equalsIgnoreCase("EQUITY");
        }
        // category is null or blank — use gain-type as heuristic
        if (row.isHybrid_taxation()) return true;
        double ltg = nvl(row.getLtg_gain()) - nvl(row.getLtg_loss());
        double stg = nvl(row.getStg_gain()) - nvl(row.getStg_loss());
        if (ltg != 0) return true;   // long-term gain/loss → equity
        if (stg != 0) return false;  // short-term only    → debt
        return true;                 // no gain data → default equity
    }

    // =========================================================================
    // GROUP rows by scheme + folio → SchemeCapitalGainDto list
    // =========================================================================

    private List<SchemeCapitalGainDto> buildSummaryList(
            List<InvestorSchemeWiseTransactionTaxReport> rows) {

        Map<String, SchemeCapitalGainDto>            summaryMap = new LinkedHashMap<>();
        Map<String, List<CapitalGainTransactionDto>> txnMap     = new LinkedHashMap<>();

        for (InvestorSchemeWiseTransactionTaxReport row : rows) {
            String key = nvlStr(row.getScheme()) + "|" + nvlStr(row.getFolio());

            if (!summaryMap.containsKey(key)) {
                SchemeCapitalGainDto dto = initSummaryDto(row);
                summaryMap.put(key, dto);
                txnMap.put(key, new ArrayList<>());
            }

            accumulateIntoSummary(summaryMap.get(key), row);
            txnMap.get(key).add(mapTransaction(row));
        }

        List<SchemeCapitalGainDto> result = new ArrayList<>();
        for (String key : summaryMap.keySet()) {
            SchemeCapitalGainDto dto = summaryMap.get(key);
            // Final gain_loss = ltg_gain - ltg_loss + stg_gain - stg_loss
            dto.setGainLoss(nvl(dto.getLtgGain()) - nvl(dto.getLtgLoss())
                    + nvl(dto.getStgGain()) - nvl(dto.getStgLoss()));
            dto.setTransactionList(txnMap.get(key));
            result.add(dto);
        }
        return result;
    }

    // ── Init a blank summary dto from the first row of a scheme+folio group ──

    private SchemeCapitalGainDto initSummaryDto(
            InvestorSchemeWiseTransactionTaxReport row) {

        SchemeCapitalGainDto dto = new SchemeCapitalGainDto();
        dto.setScheme(row.getScheme());
        dto.setSchemeAmfiCode(nvlStr(row.getScheme_amfi_code()));
        dto.setSchemeAmfiShortName(row.getScheme_amfi_short_name());
        dto.setSchemeCode(nvlStr(row.getScheme_code()));
        dto.setSchemeRegistrar(nvlStr(row.getScheme_registrar()));
        dto.setIsinNo(nvlStr(row.getIsin_no()));
        dto.setFolio(row.getFolio());
        dto.setCategory(nvlStr(row.getCategory()));
        // FIX 1: resolve logo properly
        dto.setAmcLogo(resolveAmcLogo(row));

        // Zero all aggregates
        dto.setStgGain(0.0);            dto.setStgLoss(0.0);
        dto.setLtgGain(0.0);            dto.setLtgLoss(0.0);
        dto.setStgTotalGain(0.0);       dto.setLtgTotalGain(0.0);
        dto.setStgPurchaseAmount(0.0);  dto.setStgSoldAmount(0.0);
        dto.setLtgPurchaseAmount(0.0);  dto.setLtgSoldAmount(0.0);
        dto.setIndexedCost(0.0);        dto.setIndexedTotalGain(0.0);
        dto.setIndexedGain(0.0);        dto.setIndexedLoss(0.0);
        dto.setIndexedSoldAmount(0.0);
        dto.setTotalTax(0.0);           dto.setStt(0.0);
        dto.setGainLoss(0.0);
        dto.setSoldUnits(0.0);          dto.setSoldAmount(0.0);
        dto.setPurchaseUnits(0.0);      dto.setPurchaseAmount(0.0);
        dto.setGrandfatheredUnits(0.0); dto.setGrandfatheredAmount(0.0);
        dto.setGrandfatheredNav(0.0);
        dto.setGrandfatheredPurchaseAmount(0.0);
        dto.setGrandfatheredSoldAmount(0.0);
        dto.setNonGrandfatheredPurchaseAmount(0.0);
        dto.setNonGrandfatheredSoldAmount(0.0);
        dto.setRedeemedUnits(0.0);      dto.setRedeemedNav(0.0);
        dto.setNoOfDays(0);             dto.setTransactionCount(0);
        dto.setPuramtStr("");           dto.setSoldamtStr("");
        dto.setGainlossStr("");         dto.setWhatifamountStr("");
        dto.setPurchaseAmountStr("");   dto.setSoldAmountStr("");
        dto.setGainLossStr("");         dto.setSoldUnitsStr("");
        dto.setPurchaseUnitsStr("");    dto.setRedeemedUnitsStr("");
        dto.setLtgGainStr("");         dto.setPurchaseDateStr("");
        dto.setSoldDateStr("");
        return dto;
    }

    // ── Accumulate one raw row into the running summary ───────────────────────

    private void accumulateIntoSummary(SchemeCapitalGainDto s,
                                       InvestorSchemeWiseTransactionTaxReport row) {

        s.setStgGain(s.getStgGain()     + nvl(row.getStg_gain()));
        s.setStgLoss(s.getStgLoss()     + nvl(row.getStg_loss()));
        s.setLtgGain(s.getLtgGain()     + nvl(row.getLtg_gain()));
        s.setLtgLoss(s.getLtgLoss()     + nvl(row.getLtg_loss()));
        s.setStgTotalGain(s.getStgTotalGain() + nvl(row.getStg_total_gain()));
        s.setLtgTotalGain(s.getLtgTotalGain() + nvl(row.getLtg_total_gain()));

        // FIX 3: stg/ltg amounts — use raw field if set, else fall back to
        // purchase_amount / sold_amount routed to the correct bucket.
        if (row.getStg_purchase_amount() != null) {
            s.setStgPurchaseAmount(s.getStgPurchaseAmount() + row.getStg_purchase_amount());
            s.setStgSoldAmount(s.getStgSoldAmount()         + nvl(row.getStg_sold_amount()));
        } else if (nvl(row.getStg_gain()) > 0 || nvl(row.getStg_loss()) > 0) {
            // null field but has stg gain/loss → use actual amounts as proxy
            s.setStgPurchaseAmount(s.getStgPurchaseAmount() + nvl(row.getPurchase_amount()));
            s.setStgSoldAmount(s.getStgSoldAmount()         + nvl(row.getSold_amount()));
        }

        if (row.getLtg_purchase_amount() != null) {
            s.setLtgPurchaseAmount(s.getLtgPurchaseAmount() + row.getLtg_purchase_amount());
            s.setLtgSoldAmount(s.getLtgSoldAmount()         + nvl(row.getLtg_sold_amount()));
        } else if (nvl(row.getLtg_gain()) > 0 || nvl(row.getLtg_loss()) > 0) {
            // null field but has ltg gain/loss → use actual amounts as proxy
            s.setLtgPurchaseAmount(s.getLtgPurchaseAmount() + nvl(row.getPurchase_amount()));
            s.setLtgSoldAmount(s.getLtgSoldAmount()         + nvl(row.getSold_amount()));
        }

        s.setIndexedSoldAmount(s.getIndexedSoldAmount() + nvl(row.getIndexed_sold_amount()));
        s.setIndexedGain(s.getIndexedGain()             + nvl(row.getIndexed_gain()));
        s.setIndexedLoss(s.getIndexedLoss()             + nvl(row.getIndexed_loss()));
        s.setIndexedCost(s.getIndexedCost()             + nvl(row.getIndexed_cost()));
        s.setTotalTax(s.getTotalTax()                   + nvl(row.getTotal_tax()));
        s.setStt(s.getStt()                             + nvl(row.getStt()));
        s.setSoldUnits(s.getSoldUnits()                 + nvl(row.getSold_units()));
        s.setSoldAmount(s.getSoldAmount()               + nvl(row.getSold_amount()));
        s.setPurchaseUnits(s.getPurchaseUnits()         + nvl(row.getPurchase_units()));
        s.setPurchaseAmount(s.getPurchaseAmount()       + nvl(row.getPurchase_amount()));
        s.setGrandfatheredUnits(s.getGrandfatheredUnits()   + nvl(row.getGrandfathered_units()));
        s.setGrandfatheredAmount(s.getGrandfatheredAmount() + nvl(row.getGrandfathered_amount()));
        s.setNonGrandfatheredPurchaseAmount(
                s.getNonGrandfatheredPurchaseAmount() + nvl(row.getNon_grandfathered_purchase_amount()));
        s.setNonGrandfatheredSoldAmount(
                s.getNonGrandfatheredSoldAmount()     + nvl(row.getNon_grandfathered_sold_amount()));
        s.setRedeemedUnits(s.getRedeemedUnits()         + nvl(row.getRedeemed_units()));
        s.setTransactionCount(s.getTransactionCount()   + 1);
    }

    // =========================================================================
    // Map one raw row → CapitalGainTransactionDto
    // =========================================================================

    private CapitalGainTransactionDto mapTransaction(
            InvestorSchemeWiseTransactionTaxReport row) {

        CapitalGainTransactionDto dto = new CapitalGainTransactionDto();
        dto.setScheme(row.getScheme());
        dto.setSchemeAmfiShortName(row.getScheme_amfi_short_name());
        dto.setSchemeCode(row.getScheme_code());
        dto.setIsinNo(row.getIsin_no());
        dto.setFolio(row.getFolio());
        dto.setCategory(row.getCategory());
        dto.setSoldDate(row.getSold_date());
        dto.setSoldRepeatedDate(row.getSold_repeated_date());
        dto.setSoldUnits(row.getSold_units());
        dto.setSoldAmount(row.getSold_amount());
        dto.setSoldNav(row.getSold_nav());
        dto.setPurchaseDate(row.getPurchase_date());
        dto.setPurchaseUnits(row.getPurchase_units());
        dto.setPurchaseAmount(row.getPurchase_amount());
        dto.setPurchaseNav(row.getPurchase_nav());
        dto.setNoOfDays(row.getNo_of_days());
        dto.setStgGain(row.getStg_gain());
        dto.setLtgGain(row.getLtg_gain());
        dto.setStgPurchaseAmount(row.getStg_purchase_amount());
        dto.setStgSoldAmount(row.getStg_sold_amount());
        dto.setLtgPurchaseAmount(row.getLtg_purchase_amount());
        dto.setLtgSoldAmount(row.getLtg_sold_amount());
        dto.setStgTotalGain(row.getStg_total_gain());
        dto.setLtgTotalGain(row.getLtg_total_gain());
        dto.setStgLoss(row.getStg_loss());
        dto.setLtgLoss(row.getLtg_loss());
        dto.setIndexedCost(row.getIndexed_cost());
        dto.setIndexedSoldAmount(row.getIndexed_sold_amount());
        dto.setIndexedTotalGain(row.getIndexed_total_gain());
        dto.setIndexedGain(row.getIndexed_gain());
        dto.setIndexedLoss(row.getIndexed_loss());
        dto.setTotalTax(row.getTotal_tax());
        dto.setStt(row.getStt());
        dto.setSoldTransactionType(row.getSold_transaction_type());
        dto.setPurchaseTransactionType(row.getPurchase_transaction_type());
        dto.setRedeemedUnits(row.getRedeemed_units());
        dto.setRedeemedNav(row.getRedeemed_nav());
        dto.setGrandfatheredUnits(row.getGrandfathered_units());
        dto.setGrandfatheredAmount(row.getGrandfathered_amount());
        dto.setGrandfatheredNav(row.getGrandfathered_nav());
        dto.setGrandfatheredPurchaseAmount(row.getGrandfathered_purchase_amount());
        dto.setGrandfatheredSoldAmount(row.getGrandfathered_sold_amount());
        dto.setNonGrandfatheredPurchaseAmount(row.getNon_grandfathered_purchase_amount());
        dto.setNonGrandfatheredSoldAmount(row.getNon_grandfathered_sold_amount());
        dto.setGrandfatheredCostApply(row.isGrandfathered_cost_apply());
        dto.setActualAmount(row.getActual_amount());
        dto.setGainLoss(row.getGain_loss());
        // FIX 1: use proper logo for transaction rows too
        dto.setAmcLogo(resolveAmcLogo(row));
        dto.setPurchaseDateStr(nvlStr(row.getPurchase_date_str()));
        dto.setSoldDateStr(nvlStr(row.getSold_date_str()));
        dto.setLtgGainStr(nvlStr(row.getLtg_gain_str()));
        dto.setGainLossStr(nvlStr(row.getGain_loss_str()));
        dto.setSoldUnitsStr(nvlStr(row.getSold_units_str()));
        dto.setPurchaseUnitsStr(nvlStr(row.getPurchase_units_str()));
        dto.setRedeemedUnitsStr(nvlStr(row.getRedeemed_units_str()));
        dto.setPurchaseAmountStr(nvlStr(row.getPurchase_amount_str()));
        dto.setSoldAmountStr(nvlStr(row.getSold_amount_str()));
        return dto;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * FIX 1 — Only use the raw amc_logo field if it looks like a real URL.
     * If it's blank or just the scheme name, fall through to keyword matching.
     */
    private String resolveAmcLogo(InvestorSchemeWiseTransactionTaxReport row) {
        String rawLogo = row.getAmc_logo();
        if (rawLogo != null && rawLogo.toLowerCase().startsWith("http")) {
            return rawLogo;
        }
        return resolveLogoFromSchemeName(row.getScheme());
    }

    private String resolveLogoFromSchemeName(String schemeName) {
        if (schemeName == null || schemeName.isEmpty()) return "";
        String lower = schemeName.toLowerCase();
        for (Map.Entry<String, String> entry : AMC_LOGO_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return LOGO_BASE + entry.getValue();
            }
        }
        return "";
    }

    private void zeroAllTotals(CapitalGainReportApiResponse r) {
        r.setPortfolioLongGain(0.0);   r.setPortfolioShortGain(0.0); r.setPortfolioGain(0.0);
        r.setEqGain(0.0);              r.setEqStgGain(0.0);          r.setEqStgLoss(0.0);
        r.setEqLtgGain(0.0);           r.setEqLtgLoss(0.0);
        r.setEqStgPurchaseAmount(0.0); r.setEqStgSoldAmount(0.0);
        r.setEqLtgPurchaseAmount(0.0); r.setEqLtgSoldAmount(0.0);    r.setEqLtgIndexedAmount(0.0);
        r.setDebtGain(0.0);            r.setDebtStgGain(0.0);        r.setDebtStgLoss(0.0);
        r.setDebtLtgGain(0.0);         r.setDebtLtgLoss(0.0);
        r.setDebtStgPurchaseAmount(0.0); r.setDebtStgSoldAmount(0.0);
        r.setDebtLtgPurchaseAmount(0.0); r.setDebtLtgSoldAmount(0.0); r.setDebtLtgIndexedAmount(0.0);
    }

    private double nvl(Double d) { return d == null ? 0.0 : d; }
    private String nvlStr(String s) { return s == null ? "" : s; }
}
