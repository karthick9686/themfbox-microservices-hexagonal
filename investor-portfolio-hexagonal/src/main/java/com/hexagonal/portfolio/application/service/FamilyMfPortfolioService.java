package com.hexagonal.portfolio.application.service;
import com.hexagonal.portfolio.application.port.in.GetFamilyMfPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.out.LoadFamilyMappingPort;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse.*;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWisePortfolioResponse;
import com.hexagonal.portfolio.domain.model.UsersMapping;
import com.hexagonal.portfolio.domain.model.XirrResponse;
import com.hexagonal.portfolio.domain.service.XIRR;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Builds the family-grouped response for /getInvestorPortfolioNew?source=family.
 *
 * For a family head (userId) it resolves the mapped members, pulls each member's
 * portfolio via {@link InvestorPortfolioServiceNew}, and assembles:
 *   - mf_scheme_summary  : one entry per member with ALL holdings
 *   - sip_scheme_summary : one entry per member with active-SIP holdings only
 *   - mf_summary / sip_summary : family-level roll-ups
 *
 * Family-level XIRR is recomputed exactly by concatenating every member's cashflow
 * list (the same {@code cagr_list} the portfolio service already builds) and running
 * the shared {@link XIRR} solver — identical to how a single investor's XIRR is derived.
 */
@Service
public class FamilyMfPortfolioService implements GetFamilyMfPortfolioUseCase {

    @Autowired
    private LoadFamilyMappingPort loadFamilyMappingPort;

    @Autowired
    private GetInvestorPortfolioUseCase getInvestorPortfolioUseCase;

    private static final SimpleDateFormat DATE_FORMAT_OUT = new SimpleDateFormat("dd-MM-yyyy");
    private static final String LOGO_BASE = "https://api.advisorkhoj.com/nse/images/amc-logo/";

    private static final Map<String, String> AMC_LOGO_MAP = new LinkedHashMap<>();
    static {
        AMC_LOGO_MAP.put("Aditya Birla Sun Life Mutual Fund", "birla.png");
        AMC_LOGO_MAP.put("Bandhan Mutual Fund", "bandhan.png");
        AMC_LOGO_MAP.put("Baroda BNP Paribas Mutual Fund", "bnp.png");
        AMC_LOGO_MAP.put("Canara Robeco Mutual Fund", "canara.png");
        AMC_LOGO_MAP.put("DSP Mutual Fund", "dsp.png");
        AMC_LOGO_MAP.put("Edelweiss Mutual Fund", "edelweiss.png");
        AMC_LOGO_MAP.put("Franklin Templeton Mutual Fund", "franklin.png");
        AMC_LOGO_MAP.put("HDFC Mutual Fund", "hdfc.png");
        AMC_LOGO_MAP.put("HSBC Mutual Fund", "hsbc.png");
        AMC_LOGO_MAP.put("ICICI Prudential Mutual Fund", "icici.png");
        AMC_LOGO_MAP.put("Kotak Mahindra Mutual Fund", "kotak.png");
        AMC_LOGO_MAP.put("Mirae Asset Mutual Fund", "mirae.png");
        AMC_LOGO_MAP.put("Motilal Oswal Mutual Fund", "motilal.png");
        AMC_LOGO_MAP.put("Nippon India Mutual Fund", "nippon.png");
        AMC_LOGO_MAP.put("Tata Mutual Fund", "tata.png");
        AMC_LOGO_MAP.put("SBI Mutual Fund", "sbi.png");
        AMC_LOGO_MAP.put("Axis Mutual Fund", "axis.png");
        AMC_LOGO_MAP.put("UTI Mutual Fund", "uti.png");
        AMC_LOGO_MAP.put("Sundaram Mutual Fund", "sundaram.png");
        AMC_LOGO_MAP.put("PPFAS Mutual Fund", "ppfas.png");
        AMC_LOGO_MAP.put("WhiteOak Capital Mutual Fund", "whiteoak.png");
        AMC_LOGO_MAP.put("DSP Mutual Fund ", "dsp.png");
    }

    @Override
    public FamilyMfPortfolioResponse getFamilyPortfolio(
            Integer familyHeadId, String financial_year, String folio_type,
            String selected_date, String clientName, String page) {

        FamilyMfPortfolioResponse response = new FamilyMfPortfolioResponse();
        response.setStatus(200);
        response.setStatusMsg("Success");
        response.setMsg("Success");

        List<MemberMfSummary> mfSchemeSummary = new ArrayList<>();
        List<MemberSipSummary> sipSchemeSummary = new ArrayList<>();

        // Family-level accumulators
        double famCost = 0, famValue = 0, famDivReinv = 0, famDivPaid = 0;
        double famUnrealised = 0, famRealised = 0, famDayChange = 0;
        double famSipMonthly = 0;
        List<XirrResponse> familyCagrList = new ArrayList<>();

        List<UsersMapping> members = loadFamilyMappingPort.getFamilyMappedInvestor(familyHeadId, clientName);
        if (members == null) {
            members = Collections.emptyList();
        }

        for (UsersMapping member : members) {
            Integer memberId = member.getInvestor_id();
            String investorName = member.getInvestor_name();
            String pan = member.getPan();
            String mappingName = member.getMapping_name();
            String familyStatus = (mappingName != null && mappingName.equalsIgnoreCase(investorName))
                    ? "Family Head" : "Family Member";

            InvestorPortfolioResponse memberPortfolio = getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    memberId, financial_year, folio_type, selected_date, clientName, page);

            if (memberPortfolio == null) {
                continue;
            }

            List<InvestorSchemeWisePortfolioResponse> schemes =
                    memberPortfolio.getInvestorSchemeWisePortfolioResponses() != null
                            ? memberPortfolio.getInvestorSchemeWisePortfolioResponses()
                            : Collections.emptyList();

            double mCost = nvl(memberPortfolio.getTotalCurrentcost());
            double mValue = nvl(memberPortfolio.getTotalCurrentValue());
            double mUnrealised = nvl(memberPortfolio.getTotalUnReliasedGain());
            double mRealised = nvl(memberPortfolio.getTotalReliasedGain());
            double mDivReinv = nvl(memberPortfolio.getTotalDividendReinvestment());
            double mDivPaid = nvl(memberPortfolio.getTotalDividendPaid());
            double mDayChange = nvl(memberPortfolio.getPortfolio_day_change_value());

            // ── MF member block (all holdings) ────────────────────────────────
            MemberMfSummary mf = new MemberMfSummary();
            mf.setUserId(memberId);
            mf.setInvestorName(investorName);
            mf.setPan(pan);
            mf.setSalutation(memberPortfolio.getSalutation() != null ? memberPortfolio.getSalutation() : "");
            mf.setFamilyStatus(familyStatus);
            mf.setCurrentCost(round2(mCost));
            mf.setCurrentValue(round2(mValue));
            mf.setUnrealisedGain(round2(mUnrealised));
            mf.setRealisedGain(round2(mRealised));
            mf.setAbsRtn(mCost > 0 ? round2((mUnrealised / mCost) * 100) : 0.0);
            mf.setXirr(round2(nvl(memberPortfolio.getTotalCAGR())));

            List<MfSchemeDto> mfSchemes = new ArrayList<>();
            List<SipSchemeDto> sipSchemes = new ArrayList<>();
            List<XirrResponse> memberSipCagrList = new ArrayList<>();

            for (InvestorSchemeWisePortfolioResponse s : schemes) {
                mfSchemes.add(buildMfSchemeDto(s));

                if (isActiveSip(s)) {
                    sipSchemes.add(buildSipSchemeDto(s));
                    famSipMonthly += nvl(s.getMonthly_amt());
                    if (s.getXirr_list() != null) {
                        memberSipCagrList.addAll(s.getXirr_list());
                    }
                }
            }
            mf.setSchemeList(mfSchemes);
            mfSchemeSummary.add(mf);

            // ── SIP member block (active-SIP holdings only) ───────────────────
            if (!sipSchemes.isEmpty()) {
                MemberSipSummary sip = new MemberSipSummary();
                sip.setUserId(memberId);
                sip.setInvestorName(investorName);
                sip.setPan(pan);
                sip.setSalutation(memberPortfolio.getSalutation() != null ? memberPortfolio.getSalutation() : "");
                sip.setFamilyStatus(familyStatus);
                sip.setSipAmount(0.0);
                sip.setCurrentCost(round2(mCost));
                sip.setCurrentValue(round2(mValue));
                sip.setUnrealisedGain(round2(mUnrealised));
                sip.setRealisedGain(round2(mRealised));
                sip.setAbsRtn(mCost > 0 ? round2((mUnrealised / mCost) * 100) : 0.0);
                sip.setXirr(round2(computeXirr(memberSipCagrList)));
                sip.setSchemeList(sipSchemes);
                sipSchemeSummary.add(sip);
            }

            // ── Family accumulation ───────────────────────────────────────────
            famCost += mCost;
            famValue += mValue;
            famUnrealised += mUnrealised;
            famRealised += mRealised;
            famDivReinv += mDivReinv;
            famDivPaid += mDivPaid;
            famDayChange += mDayChange;
            if (memberPortfolio.getCagr_list() != null) {
                familyCagrList.addAll(memberPortfolio.getCagr_list());
            }
        }

        // ── mf_summary ────────────────────────────────────────────────────────
        MfSummary mfSummary = new MfSummary();
        mfSummary.setTotalCurrCost(round2(famCost));
        mfSummary.setTotalCurrValue(round2(famValue));
        mfSummary.setTotalDivReinv(round2(famDivReinv));
        mfSummary.setTotalDivPaid(round2(famDivPaid));
        mfSummary.setTotalUnrealisedGain(round2(famUnrealised));
        mfSummary.setTotalRealisedGain(round2(famRealised));
        mfSummary.setTotalAbsRtn(famCost > 0 ? round2((famUnrealised / famCost) * 100) : 0.0);
        mfSummary.setTotalXirr(round2(computeXirr(familyCagrList)));
        mfSummary.setDayChangeValue(round2(famDayChange));
        mfSummary.setDayChangePercentage(
                (famValue - famDayChange) != 0 ? round2((famDayChange / (famValue - famDayChange)) * 100) : 0.0);
        response.setMfSummary(mfSummary);

        // ── sip_summary (only sip_amount is derivable without a SIP-master lookup) ─
        SipSummary sipSummary = new SipSummary();
        sipSummary.setSipCurrValue(0.0);
        sipSummary.setSipCurrCost(0.0);
        sipSummary.setSipAmount(round2(famSipMonthly));
        sipSummary.setSipCount(0.0);
        sipSummary.setSipXirr(0.0);
        sipSummary.setSipDayChangeAmount(0.0);
        sipSummary.setSipDayChangePercentage(0.0);
        sipSummary.setSipGainLoss(0.0);
        response.setSipSummary(sipSummary);

        response.setMfSchemeSummary(mfSchemeSummary);
        response.setSipSchemeSummary(sipSchemeSummary);
        return response;
    }

    // =========================================================================
    // Scheme mapping
    // =========================================================================
    private MfSchemeDto buildMfSchemeDto(InvestorSchemeWisePortfolioResponse s) {
        MfSchemeDto dto = new MfSchemeDto();
        dto.setSchemeAmfi(s.getScheme());
        dto.setSchemeAmfiCode(s.getScheme_amfi_code());
        dto.setSchemeCode(s.getScheme_code());
        dto.setSchemeAmfiShortName(s.getScheme_amfi_short_name());
        dto.setSchemeAmc(resolveAmcName(s));
        dto.setSchemeAmcCode(s.getAmc_code());
        dto.setSchemeAmcLogo(resolveLogo(s));
        dto.setSchemeBroadCategory(s.getScheme_class());
        dto.setSchemeCategory(s.getScheme_advisorkhoj_category());
        dto.setFolio(s.getFoliono());
        dto.setFolioDesc(s.getFoliono());
        dto.setBrokerCode(s.getBroker_code());
        dto.setCurrCost(round2(s.getCurrentCostOfInvestment()));
        dto.setCurrValue(round2(s.getTotalCurrentValue()));
        dto.setUnits(s.getTotalUnits());
        dto.setUnrealisedProfitLoss(round2(s.getUnrealisedProfitLoss()));
        dto.setRealisedProfitLoss(round2(s.getRealisedProfitLoss()));
        dto.setRealisedGainLoss(round2(s.getRealisedGainLoss()));
        dto.setDayChangeValue(nvl(s.getDay_change_value()));
        dto.setDayChangePercentage(nvl(s.getDay_change_percentage()));
        dto.setAbsoluteReturn(s.getAbsolute_return());
        dto.setXirr(s.getCagr());
        dto.setSipScheme(bool(s.getIsSipScheme()));
        dto.setSip(bool(s.getIsActiveSipScheme()));
        dto.setStpInScheme(bool(s.getIsStpInScheme()));
        dto.setStpOutScheme(bool(s.getIsStpOutScheme()));
        dto.setStp(bool(s.getIsActiveStpScheme()));
        dto.setSwpScheme(bool(s.getIsSwpScheme()));
        dto.setSwp(bool(s.getIsActiveSwpScheme()));
        dto.setManualEntry(bool(s.getIsManualEntry()));
        dto.setManualEntryVal("");
        dto.setGoalName(s.getGoal_name() != null ? s.getGoal_name() : "");
        dto.setFreeUnits(nvl(s.getLoadFreeUnits()));
        dto.setAvgDays(s.getAverage_days());
        // MF block mirrors the reference payload: latest-NAV fields are left unset here.
        dto.setLatestNav(0.0);
        dto.setLatestNavDate(null);
        dto.setLatestNavDateStr(null);
        return dto;
    }

    private SipSchemeDto buildSipSchemeDto(InvestorSchemeWisePortfolioResponse s) {
        SipSchemeDto dto = new SipSchemeDto();
        dto.setSchemeAmfi(s.getScheme());
        dto.setSchemeAmfiCode(s.getScheme_amfi_code());
        dto.setSchemeCode(s.getScheme_code());
        dto.setSchemeAmfiShortName(s.getScheme_amfi_short_name());
        dto.setSchemeAmc(resolveAmcName(s));
        dto.setSchemeAmcCode(s.getAmc_code());
        dto.setSchemeAmcLogo(resolveLogo(s));
        dto.setSchemeBroadCategory(s.getScheme_class());
        dto.setSchemeCategory(s.getScheme_advisorkhoj_category());
        dto.setFolio(s.getFoliono());
        dto.setBrokerCode(s.getBroker_code());
        // SIP registration dates require a SIP-master lookup (not performed here).
        dto.setSipStartDate("");
        dto.setSipEndDate("");
        dto.setSipEcsDate("");
        dto.setSipFrequency(s.getSip_frequency() != null ? s.getSip_frequency() : "");
        dto.setSipAmount(nvl(s.getSip_amount()));
        dto.setSipMonthlyAmount(nvl(s.getMonthly_amt()));
        dto.setUnits(s.getTotalUnits());
        dto.setAvgNav(s.getPurchaseNav());
        dto.setCurrCost(round2(s.getCurrentCostOfInvestment()));
        dto.setLatestNav(s.getLatestNav());
        dto.setNavDate(s.getLatestNavDate() != null ? DATE_FORMAT_OUT.format(s.getLatestNavDate()) : "");
        dto.setCurrValue(round2(s.getTotalCurrentValue()));
        dto.setUnrealisedProfitLoss(round2(s.getUnrealisedProfitLoss()));
        dto.setRealisedProfitLoss(round2(s.getRealisedProfitLoss()));
        dto.setRealisedGainLoss(round2(s.getRealisedGainLoss()));
        dto.setDayChangeValue(nvl(s.getDay_change_value()));
        dto.setDayChangePercentage(nvl(s.getDay_change_percentage()));
        dto.setAbsoluteReturn(s.getAbsolute_return());
        dto.setXirr(s.getCagr());
        return dto;
    }

    // =========================================================================
    // XIRR (exact, over the concatenated cashflow list)
    // =========================================================================
    private double computeXirr(List<XirrResponse> cagrList) {
        if (cagrList == null || cagrList.isEmpty()) {
            return 0.0;
        }
        cagrList.sort(Comparator.comparing(XirrResponse::getTrxn_date));

        double[] values = new double[cagrList.size()];
        double[] dates = new double[cagrList.size()];

        Calendar epoch = Calendar.getInstance();
        epoch.setTimeInMillis(0);

        int i = 0;
        for (XirrResponse xir : cagrList) {
            values[i] = xir.getAmount();
            Calendar cal = Calendar.getInstance();
            cal.setTime(xir.getTrxn_date());
            dates[i] = (double) XIRR.getDateDiff(cal, epoch);
            i++;
        }

        double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
        if (xirrValue == 0) {
            xirrValue = XIRR.Bisection_method(values, dates, 0.1);
        }
        if (xirrValue == 0.1) {
            xirrValue = 0.0;
        }
        return xirrValue * 100;
    }

    // =========================================================================
    // AMC / logo resolution
    // =========================================================================
    private String resolveAmcName(InvestorSchemeWisePortfolioResponse s) {
        if (s.getScheme_company() != null && !s.getScheme_company().isEmpty()) {
            return s.getScheme_company();
        }
        if (s.getAmc_name() != null && !s.getAmc_name().isEmpty()) {
            return s.getAmc_name();
        }
        return "";
    }

    private String resolveLogo(InvestorSchemeWisePortfolioResponse s) {
        if (s.getAmc_logo() != null && !s.getAmc_logo().isEmpty()) {
            return s.getAmc_logo();
        }
        String logo = AMC_LOGO_MAP.get(resolveAmcName(s));
        return logo != null ? LOGO_BASE + logo : "";
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    /** A holding is treated as a SIP row when the portfolio flags it as an active SIP. */
    private boolean isActiveSip(InvestorSchemeWisePortfolioResponse s) {
        return bool(s.getIsActiveSipScheme()) || bool(s.getIsSipScheme());
    }

    private boolean bool(Boolean b) {
        return b != null && b;
    }

    private double nvl(Double d) {
        return d == null ? 0.0 : d;
    }

    private double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}
