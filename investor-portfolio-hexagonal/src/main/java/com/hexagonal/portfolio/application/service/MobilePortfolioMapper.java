package com.hexagonal.portfolio.application.service;
import com.hexagonal.portfolio.application.port.in.ConvertToMobilePortfolioUseCase;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse.*;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWisePortfolioResponse;
import com.hexagonal.portfolio.domain.service.MyMFBoxUtils;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Maps the raw InvestorPortfolioResponse into the mobile-specific
 * InvestorPortfolioNewMobileResponse for /getInvestorPortfolioNew?source=mobile.
 *
 * Structure: portfolio summary + list(broad_category → category_list → scheme_list).
 */
@Service
public class MobilePortfolioMapper implements ConvertToMobilePortfolioUseCase {

    private static final SimpleDateFormat DATE_FORMAT_OUT = new SimpleDateFormat("dd-MM-yyyy");

    private static final String LOGO_BASE = "https://api.advisorkhoj.com/nse/images/amc-logo/";

    // AMC name → logo filename mapping
    private static final Map<String, String> AMC_LOGO_MAP = new LinkedHashMap<>();
    static {
        AMC_LOGO_MAP.put("Aditya Birla Sun Life Mutual Fund",   "birla.png");
        AMC_LOGO_MAP.put("Bandhan Mutual Fund",                  "bandhan.png");
        AMC_LOGO_MAP.put("Baroda BNP Paribas Mutual Fund",       "bnp.png");
        AMC_LOGO_MAP.put("Canara Robeco Mutual Fund",            "canara.png");
        AMC_LOGO_MAP.put("DSP Mutual Fund",                      "dsp.png");
        AMC_LOGO_MAP.put("Edelweiss Mutual Fund",                "edelweiss.png");
        AMC_LOGO_MAP.put("Franklin Templeton Mutual Fund",       "franklin.png");
        AMC_LOGO_MAP.put("HDFC Mutual Fund",                     "hdfc.png");
        AMC_LOGO_MAP.put("ICICI Prudential Mutual Fund",         "icici.png");
        AMC_LOGO_MAP.put("Kotak Mahindra Mutual Fund",           "kotak.png");
        AMC_LOGO_MAP.put("Mirae Asset Mutual Fund",              "mirae.png");
        AMC_LOGO_MAP.put("Motilal Oswal Mutual Fund",            "motilal.png");
        AMC_LOGO_MAP.put("Nippon India Mutual Fund",             "nippon.png");
        AMC_LOGO_MAP.put("Tata Mutual Fund",                     "tata.png");
        AMC_LOGO_MAP.put("SBI Mutual Fund",                      "sbi.png");
        AMC_LOGO_MAP.put("Axis Mutual Fund",                     "axis.png");
        AMC_LOGO_MAP.put("UTI Mutual Fund",                      "uti.png");
        AMC_LOGO_MAP.put("Sundaram Mutual Fund",                 "sundaram.png");
        AMC_LOGO_MAP.put("PPFAS Mutual Fund",                    "ppfas.png");
    }

    @Override
    public InvestorPortfolioNewMobileResponse convert(InvestorPortfolioResponse raw) {
        InvestorPortfolioNewMobileResponse response = new InvestorPortfolioNewMobileResponse();
        response.setStatus(200);
        response.setStatusMsg("Success");
        response.setMsg("Success");

        double total = nvl(raw != null ? raw.getTotalCurrentValue() : null);

        response.setCurrentCost(round2(nvl(raw != null ? raw.getTotalCurrentcost() : null)));
        response.setCurrentValue(round2(total));
        response.setUnrealisedGain(round2(nvl(raw != null ? raw.getTotalUnReliasedGain() : null)));
        response.setRealisedGain(round2(nvl(raw != null ? raw.getTotalReliasedGain() : null)));
        response.setAbsoluteReturn(round2(nvl(raw != null ? raw.getTotalAbsoluteReturn() : null)));
        response.setPortfolioWeight(total > 0 ? 100.0 : 0.0);
        response.setXirr(round2(nvl(raw != null ? raw.getTotalCAGR() : null)));

        List<InvestorSchemeWisePortfolioResponse> schemes =
                (raw != null && raw.getInvestorSchemeWisePortfolioResponses() != null)
                        ? raw.getInvestorSchemeWisePortfolioResponses()
                        : Collections.emptyList();

        response.setList(buildBroadCategoryList(schemes, total));
        return response;
    }

    // =========================================================================
    // broad_category → category_list → scheme_list
    // =========================================================================
    private List<BroadCategoryDto> buildBroadCategoryList(
            List<InvestorSchemeWisePortfolioResponse> schemes, double total) {

        // Group schemes by broad category (preserving encounter order)
        Map<String, List<InvestorSchemeWisePortfolioResponse>> byBroad = new LinkedHashMap<>();
        for (InvestorSchemeWisePortfolioResponse s : schemes) {
            String broad = resolveBroadCategory(s);
            byBroad.computeIfAbsent(broad, k -> new ArrayList<>()).add(s);
        }

        List<BroadCategoryDto> result = new ArrayList<>();
        for (Map.Entry<String, List<InvestorSchemeWisePortfolioResponse>> broadEntry : byBroad.entrySet()) {
            List<InvestorSchemeWisePortfolioResponse> broadSchemes = broadEntry.getValue();

            BroadCategoryDto broadDto = new BroadCategoryDto();
            broadDto.setBroadCategory(broadEntry.getKey());

            double bCost  = sumCost(broadSchemes);
            double bValue = sumValue(broadSchemes);
            broadDto.setCurrentCost(round2(bCost));
            broadDto.setCurrentValue(round2(bValue));
            broadDto.setUnrealisedGain(round2(sumUnrealised(broadSchemes)));
            broadDto.setRealisedGain(round2(sumRealised(broadSchemes)));
            broadDto.setAbsRtn(bCost > 0 ? round2(((bValue - bCost) / bCost) * 100) : 0.0);
            broadDto.setPortfolioWeight(total > 0 ? round2((bValue / total) * 100) : 0.0);

            // Group this broad category's schemes by detailed category
            Map<String, List<InvestorSchemeWisePortfolioResponse>> byCategory = new LinkedHashMap<>();
            for (InvestorSchemeWisePortfolioResponse s : broadSchemes) {
                String cat = resolveDetailedCategory(s);
                byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(s);
            }

            List<CategoryDto> categoryList = new ArrayList<>();
            for (Map.Entry<String, List<InvestorSchemeWisePortfolioResponse>> catEntry : byCategory.entrySet()) {
                List<InvestorSchemeWisePortfolioResponse> catSchemes = catEntry.getValue();

                // Keep only schemes that currently hold value (current_value > 0)
                List<SchemeDto> schemeList = new ArrayList<>();
                for (InvestorSchemeWisePortfolioResponse s : catSchemes) {
                    if (s.getTotalCurrentValue() > 0) {
                        schemeList.add(buildSchemeDto(s, total));
                    }
                }

                // Skip fully redeemed / zero-value categories
                if (schemeList.isEmpty()) {
                    continue;
                }

                CategoryDto catDto = new CategoryDto();
                catDto.setCategory(catEntry.getKey());

                double cCost  = sumCost(catSchemes);
                double cValue = sumValue(catSchemes);
                catDto.setCurrentCost(round2(cCost));
                catDto.setCurrentValue(round2(cValue));
                catDto.setUnrealisedGain(round2(sumUnrealised(catSchemes)));
                catDto.setRealisedGain(round2(sumRealised(catSchemes)));
                catDto.setAbsRtn(cCost > 0 ? round2(((cValue - cCost) / cCost) * 100) : 0.0);
                catDto.setPortfolioWeight(total > 0 ? round2((cValue / total) * 100) : 0.0);
                catDto.setSchemeList(schemeList);

                categoryList.add(catDto);
            }
            broadDto.setCategoryList(categoryList);

            result.add(broadDto);
        }
        return result;
    }

    private SchemeDto buildSchemeDto(InvestorSchemeWisePortfolioResponse s, double total) {
        SchemeDto dto = new SchemeDto();
        System.out.println("hello" + s.getScheme());
        dto.setSchemeAmfi(s.getScheme());
        dto.setSchemeAmfiShortName(s.getScheme_amfi_short_name());
        dto.setLogo(resolveLogo(s));
        dto.setInvestorName(s.getInvestorName() != null ? s.getInvestorName() : "");
        dto.setFolioNo(s.getFoliono());

        if (s.getInvestmentStartDate() != null)
        {
            dto.setStartDate(DATE_FORMAT_OUT.format(s.getInvestmentStartDate()));
        } else if (s.getInvestmentStartDate_str() != null)
        {
            dto.setStartDate(s.getInvestmentStartDate_str());
        } else
        {
            dto.setStartDate("");
        }

        dto.setUnits(round4(s.getTotalUnits()));
        dto.setAvgNav(round4(s.getPurchaseNav()));

        double cost  = s.getCurrentCostOfInvestment();
        double value = s.getTotalCurrentValue();
        dto.setCurrentCost(round2(cost));
        dto.setDividend(round2(s.getTotal_dividend_paid() + s.getTotal_dividend_reinvest()));
        dto.setLatestNav(round4(s.getLatestNav()));

        if (s.getLatestNavDate() != null)
        {
            dto.setNavDate(DATE_FORMAT_OUT.format(s.getLatestNavDate()));
        } else if (s.getLatestNavDate_str() != null) {
            dto.setNavDate(s.getLatestNavDate_str());
        } else {
            dto.setNavDate("");
        }

        dto.setCurrentValue(round2(value));
        dto.setUnrealisedGain(round2(s.getUnrealisedProfitLoss()));
        dto.setRealisedGain(round2(s.getRealisedGainLoss()));
        dto.setAbsRtn(cost > 0 ? round2(((value - cost) / cost) * 100) : 0.0);
        dto.setSchemeWeight(total > 0 ? round2((value / total) * 100) : 0.0);
        dto.setXirr(round2(s.getCagr()));

        return dto;
    }

    /**
     * Resolve the AMC logo URL for a holding. The brand map is keyed on the first word of a name,
     * and some AMFI scheme names do not lead with the AMC brand (so {@code scheme} alone falls back
     * to {@code empty.png} for those schemes). Prefer the canonical AMC name {@code scheme_company}
     * (e.g. "HDFC Mutual Fund"), which is reliably brand-first, then the short name, then the raw
     * scheme name. Returns a full URL, defaulting to the {@code empty.png} placeholder.
     */
    private String resolveLogo(InvestorSchemeWisePortfolioResponse s) {
        String[] candidates = {
                s.getScheme_company(),
                s.getScheme_amfi_short_name(),
                s.getScheme()
        };
        for (String name : candidates) {
            if (name != null && !name.trim().isEmpty()) {
                String file = MyMFBoxUtils.getLogoByAmcNameOrSchemeName(name);
                if (!"empty.png".equalsIgnoreCase(file)) {
                    return LOGO_BASE + file;
                }
            }
        }
        return LOGO_BASE + "empty.png";
    }

    private String resolveDetailedCategory(InvestorSchemeWisePortfolioResponse s) {
        String cat = s.getScheme_advisorkhoj_category();
        if (cat != null && !cat.trim().isEmpty()) return cat.trim();
        String cls = s.getScheme_class();
        if (cls != null && !cls.trim().isEmpty()) return cls.trim();
        return "Others";
    }

    private String resolveBroadCategory(InvestorSchemeWisePortfolioResponse s) {
        String detailed = resolveDetailedCategory(s);
        int idx = detailed.indexOf(':');
        return idx > 0 ? detailed.substring(0, idx).trim() : detailed;
    }

    private double sumCost(List<InvestorSchemeWisePortfolioResponse> list) {
        return list.stream().mapToDouble(InvestorSchemeWisePortfolioResponse::getCurrentCostOfInvestment).sum();
    }

    private double sumValue(List<InvestorSchemeWisePortfolioResponse> list) {
        return list.stream().mapToDouble(InvestorSchemeWisePortfolioResponse::getTotalCurrentValue).sum();
    }

    private double sumUnrealised(List<InvestorSchemeWisePortfolioResponse> list) {
        return list.stream().mapToDouble(InvestorSchemeWisePortfolioResponse::getUnrealisedProfitLoss).sum();
    }

    private double sumRealised(List<InvestorSchemeWisePortfolioResponse> list) {
        return list.stream().mapToDouble(InvestorSchemeWisePortfolioResponse::getRealisedGainLoss).sum();
    }

    private double nvl(Double d) {
        return d == null ? 0.0 : d;
    }

    private double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private double round4(double d) {
        return Math.round(d * 10000.0) / 10000.0;
    }
}
