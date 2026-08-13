package com.hexagonal.portfolio.application.service;

import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.out.*;
import com.hexagonal.portfolio.domain.model.FundScoreInfo;
import com.hexagonal.portfolio.domain.model.*;
import com.hexagonal.portfolio.domain.service.MfboxUtils;
import com.hexagonal.portfolio.domain.service.MyMFBoxUtils;
import com.hexagonal.portfolio.domain.service.TransactionDataUtils;
import com.hexagonal.portfolio.domain.service.XIRR;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Application service implementing the investor-portfolio valuation use case.
 *
 * <p>The valuation logic is carried over line-for-line from the legacy
 * {@code InvestorPortfolioServiceImpl}: processCamsScheme / processKarvyScheme /
 * processManualScheme cover units, cost basis, realised and unrealised gain, dividends,
 * SIP/STP/SWP flags, AMFI scheme mapping and NAV lookups, XIRR and category totals.
 * Rounding is deliberately left as-is ({@link DecimalFormat} round-trips through
 * {@code Double.parseDouble}) so the emitted figures match the legacy endpoint exactly.
 *
 * <p>The only structural change is dependency direction: instead of injecting Spring Data
 * repositories, this service depends on the driven ports in
 * {@code com.hexagonal.portfolio.application.port.out}, which the persistence adapters
 * implement. Nothing here knows about JPA, HTTP or the database.
 */
@Service
public class InvestorPortfolioService implements GetInvestorPortfolioUseCase {

    @Value("${amc.logo.url}")
    private String amcLogoPath;

    @Autowired
    private TransactionTypeConfigPort transactionTypeConfigPort;

    @Autowired
    private LoadInvestorPort loadInvestorPort;

    @Autowired
    private LoadSipRegistrationPort loadSipRegistrationPort;

    @Autowired
    private LoadCamsTransactionPort loadCamsTransactionPort;

    @Autowired
    private LoadKarvyTransactionPort loadKarvyTransactionPort;

    @Autowired
    private LoadManualTransactionPort loadManualTransactionPort;

    // ---- newly required repositories (create these interfaces if they don't exist yet) ----
    @Autowired
    private LoadCamsFolioMasterPort loadCamsFolioMasterPort;

    @Autowired
    private LoadKarvyFolioMasterPort loadKarvyFolioMasterPort;

    @Autowired
    private LoadSchemeMasterPort loadSchemeMasterPort;

    @Autowired
    private LoadLatestNavPort loadLatestNavPort;

    @Autowired
    private LoadNavHistoryPort loadNavHistoryPort;

    @Autowired
    private LoadFundScorePort loadFundScorePort;

    @Autowired
    private LoadHealthCheckupPort loadHealthCheckupPort;

    @Override
    public InvestorPortfolioResponse getInvestorPortfolioNew(
            Integer user_id, String financial_year, String folio_type,
            String selected_date, String client_name, String page) {

        DecimalFormat round_dcf = new DecimalFormat("00000");
        DecimalFormat cost_dcf = new DecimalFormat("0.00");
        DecimalFormat unit_decimal = new DecimalFormat("0.0000");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        Format format = java.text.NumberFormat.getNumberInstance(new Locale("en", "in"));

        InvestorPortfolioResponse investorPortfolioResponse = new InvestorPortfolioResponse();
        List<InvestorSchemeWisePortfolioResponse> investorSchemeWisePortfolioResponseList = new ArrayList<>();

        List<XirrResponse> final_cagr_list = new ArrayList<>();
        List<XirrResponse> equity_cagr_list = new ArrayList<>();
        List<XirrResponse> debt_cagr_list = new ArrayList<>();
        List<XirrResponse> hybrid_cagr_list = new ArrayList<>();
        List<XirrResponse> solution_cagr_list = new ArrayList<>();
        List<XirrResponse> other_cagr_list = new ArrayList<>();

        try {
            // ---- Transaction type config ----
            List<String> camsPositiveTransactionArrayList = transactionTypeConfigPort.getCamsPositive();
            List<String> camsNeutralTransactionArrayList = transactionTypeConfigPort.getCamsNeutral();
            List<String> karvyPositiveTransactionArrayList = transactionTypeConfigPort.getKarvyPositive();
            List<String> karvyNeutralTransactionArrayList = transactionTypeConfigPort.getKarvyNeutral();
            List<String> mf_manualPositiveTransactionArrayList = transactionTypeConfigPort.getMfManualPositive();
            List<String> mf_manualNeutralTransactionArrayList = transactionTypeConfigPort.getMfManualNeutral();
            List<String> karvy_neutral_dividend_transaction_type_list = MfboxUtils.KarvyNeutralDividendTransactionType();

            // ---- Transaction type lists ----
            List<String> cams_switch_in_list = Arrays.asList("Switch In", "NFO SI", "STP In", "TICOB", "TIX", "TIXT", "Transfer In");
            List<String> cams_switch_out_list = Arrays.asList("Switch Out", "Full Switch Out", "Partial Switch Out", "TOCOB", "STP Out", "TOX", "TOXT", "Transfer Out");
            List<String> cams_dividend_list = Arrays.asList("Dividend Reinvest", "BONUS");
            List<String> karvy_dividend_list = Arrays.asList("Div. Reinvestment", "ReInvestment Rejection", "ReInvestment Rejection Reversal", "Dividend Sweep In", "Dividend Sweep In Rej.", "Bonus Units", "Bonus Rejection", "Segregated Units");
            List<String> karvy_switch_in_list = Arrays.asList("Lateral Shift In", "Lat. Shift In Rej.", "Lat. Shift In Rej. Reversal", "S T P In", "S T P In Rej", "S T P In Rej Reversal", "Switch Over In", "Swit. Over In Rej.", "Swit. Over In Rej. Reversal", "STRIP In", "STRIP In Rejection", "Transmission In", "Transmission In Rejection", "Demat Transfer In", "Demat Transfer in Rej", "Asset Allocation In", "Asset Allocation In Rejection");
            List<String> karvy_switch_out_list = Arrays.asList("Lateral Shift Out", "Lat. Shift Out Rej.", "Lat. Shift Out Rej. Reversal", "S T P Out", "S T P Out Rej", "S T P Out Rej Reversal", "Switch Over Out", "Swit. Over Out Rej.", "Swit. Over Out Rej. Reversal", "STRIP Out", "STRIP Out Rejection", "Transmission Out", "Transmission Out Rejection", "Demat Transfer Out", "Demat Transfer Out Rej", "Demat Transfer Out Rej Reversal", "Asset Allocation Out", "Asset Allocation Out Rejection");
            List<String> manual_switch_in_list = Arrays.asList("Switch In", "STP In");
            List<String> manual_switch_out_list = Arrays.asList("Full Switch Out", "Partial Switch Out", "STP Out");
            List<String> cams_sip_list = Arrays.asList("Fresh Purchase Systematic", "Additional Purchase Systematic", "NFOFS", "NFOAS");
            List<String> cams_sip_list2 = Arrays.asList("Fresh Purchase", "Additional Purchase");

            // ---- User info ----
            Calendar calc2 = Calendar.getInstance();
            Date today = calc2.getTime();

            Optional<User> userOpt = loadInvestorPort.findByIdAndClientName(user_id, client_name);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                investorPortfolioResponse.setId(user.getId());
                investorPortfolioResponse.setType_id(user.getType_id());
                investorPortfolioResponse.setInvestorName(user.getName());
                investorPortfolioResponse.setInvestorPan(user.getPan());
                investorPortfolioResponse.setAddress1(user.getStreet_1());
                investorPortfolioResponse.setAddress2(user.getStreet_2());
                investorPortfolioResponse.setAddress3(user.getStreet_3());
                investorPortfolioResponse.setCity(user.getCity());
                investorPortfolioResponse.setPincode(user.getPincode());
                investorPortfolioResponse.setState(user.getState());
                investorPortfolioResponse.setGuardian_name(user.getGuard_name());
                investorPortfolioResponse.setGuardian_pan(user.getGuard_pan());
                investorPortfolioResponse.setPhone_office(user.getPhone_office());
                investorPortfolioResponse.setPhone_residence(user.getPhone_residence());
                investorPortfolioResponse.setEmail(user.getEmail());
                investorPortfolioResponse.setDate_of_birth(user.getDate_of_birth());
                investorPortfolioResponse.setMobile(user.getMobile());
                investorPortfolioResponse.setSalutation(user.getSalutation());
                investorPortfolioResponse.setMf_oneday_change(user.isMf_oneday_change());
                investorPortfolioResponse.setUser_risk(user.getUser_risk() != null ? user.getUser_risk() : "");
            }

            // ---- Date calculations ----
            Calendar cal = Calendar.getInstance();
            Date today_date = cal.getTime();
            int current_year = cal.get(Calendar.YEAR);
            cal = Calendar.getInstance();
            cal.set(current_year, 11, 31);
            Date year_end_date = cal.getTime();
            String financial_year_end_date_str = sdf1.format(year_end_date);

            if (!financial_year.equalsIgnoreCase("All")) {
                String[] financial_year_arr = financial_year.split("-");
                if (financial_year_arr.length > 1) {
                    String financial_end_year = financial_year_arr[1];
                    if (StringHelper.isNotEmpty(financial_end_year)) {
                        int end_year = Integer.parseInt(financial_end_year);
                        cal = Calendar.getInstance();
                        cal.set(end_year, 2, 31);
                        Date financial_year_end_date = cal.getTime();
                        if (financial_year_end_date.compareTo(today_date) < 0) {
                            financial_year_end_date_str = sdf1.format(financial_year_end_date);
                        }
                    }
                }
            }

            cal = Calendar.getInstance();
            int CurrentYear = cal.get(Calendar.YEAR);
            int CurrentMonth = (cal.get(Calendar.MONTH) + 1);
            String financiyalYearFrom;
            if (CurrentMonth < 4) {
                financiyalYearFrom = "01-04-" + (CurrentYear - 1);
            } else {
                financiyalYearFrom = "01-04-" + (CurrentYear);
            }
            Date financial_year_strt_date = sdf2.parse(financiyalYearFrom);

            if (StringHelper.isNotEmpty(selected_date)) {
                Date specific_date = sdf2.parse(selected_date);
                financial_year_end_date_str = sdf1.format(specific_date);
                cal = Calendar.getInstance();
                cal.setTime(specific_date);
                CurrentYear = cal.get(Calendar.YEAR);
                CurrentMonth = (cal.get(Calendar.MONTH) + 1);
                if (CurrentMonth < 4) {
                    financiyalYearFrom = "01-04-" + (CurrentYear - 1);
                    financial_year = (CurrentYear - 1) + "-" + CurrentYear;
                } else {
                    financiyalYearFrom = "01-04-" + (CurrentYear);
                    financial_year = CurrentYear + "-" + (CurrentYear + 1);
                }
                financial_year_strt_date = sdf2.parse(financiyalYearFrom);
            }

            cal = Calendar.getInstance();
            if (StringHelper.isNotEmpty(selected_date)) {
                Date specific_date = sdf2.parse(selected_date);
                cal.setTime(specific_date);
            }
            cal.add(Calendar.DATE, -35);
            Date fourty_days_befor_date = cal.getTime();

            Calendar calc = Calendar.getInstance();
            calc.add(Calendar.DATE, -1);
            Date yesterday = calc.getTime();
            yesterday = sdf1.parse(sdf1.format(yesterday));

            // ======================================================
            // CAMS + KARVY processing
            // ======================================================
            Date financialYearEndDate = null;
            if (!folio_type.equalsIgnoreCase("MF bought from others")) {

                List<InvestorSipCams49> sip_cams = loadSipRegistrationPort.findActiveStpSwpNew(user_id, client_name);
                List<InvestorSipCams49> active_sip_cams = loadSipRegistrationPort.findActiveSipNew(user_id, client_name);
                List<InvestorSipCams49> closed_sip_cams = loadSipRegistrationPort.findClosedSipStpSwpNew(user_id, client_name);

                boolean sameSchemeMultipleArnAdvisors = MfboxUtils.getSameSchemeMultipleArnAdvisors(client_name);

                // ---- CAMS ----
                financialYearEndDate = sdf1.parse(financial_year_end_date_str);
                List<InvestorTransactionCams> allCamsTransactions = loadCamsTransactionPort.findAllByUserAndDate(user_id, client_name, financialYearEndDate);
                List<Object[]> camsSchemeList = getCamsSchemeList(allCamsTransactions, sameSchemeMultipleArnAdvisors);

                for (int s = 0; s < camsSchemeList.size(); s++) {
                    String folio_no = camsSchemeList.get(s)[0].toString();
                    String code = camsSchemeList.get(s)[1].toString();

                    List<InvestorTransactionCams> camsSchemeWiseInvestorTransactions;
                    if (sameSchemeMultipleArnAdvisors) {
                        String brokcode = camsSchemeList.get(s)[2].toString();
                        camsSchemeWiseInvestorTransactions = allCamsTransactions.stream()
                                .filter(trxn -> trxn.getFolio_no().equalsIgnoreCase(folio_no)
                                        && trxn.getProdcode().equalsIgnoreCase(code)
                                        && trxn.getBrokcode().equalsIgnoreCase(brokcode))
                                .collect(Collectors.toList());
                    } else {
                        camsSchemeWiseInvestorTransactions = allCamsTransactions.stream()
                                .filter(trxn -> trxn.getFolio_no().equalsIgnoreCase(folio_no)
                                        && trxn.getProdcode().equalsIgnoreCase(code))
                                .collect(Collectors.toList());
                    }

                    camsSchemeWiseInvestorTransactions = TransactionDataUtils.removeCamsMinusTransaction(camsSchemeWiseInvestorTransactions);
                    if (camsSchemeWiseInvestorTransactions.isEmpty()) continue;

                    InvestorSchemeWisePortfolioResponse resp = processCamsScheme(
                            user_id,
                            camsSchemeWiseInvestorTransactions, folio_no, code,
                            sip_cams, active_sip_cams, closed_sip_cams,
                            camsPositiveTransactionArrayList, camsNeutralTransactionArrayList,
                            cams_switch_in_list, cams_switch_out_list, cams_dividend_list,
                            cams_sip_list, cams_sip_list2,
                            folio_type, financial_year, financial_year_end_date_str,
                            financial_year_strt_date, fourty_days_befor_date, today, yesterday,
                            client_name, page,
                            round_dcf, cost_dcf, unit_decimal, sdf1, sdf2, format,
                            equity_cagr_list, debt_cagr_list, hybrid_cagr_list,
                            solution_cagr_list, other_cagr_list, final_cagr_list,
                            investorPortfolioResponse);

                    if (resp != null) {
                        investorSchemeWisePortfolioResponseList.add(resp);
                    }
                }

                // ---- Karvy ----
                List<InvestorTransactionKarvy> allKarvyTransactions = loadKarvyTransactionPort.findAllByUserAndDateNew(user_id, client_name, financialYearEndDate);
                List<Object[]> karvySchemeList = getKarvySchemeList(allKarvyTransactions, sameSchemeMultipleArnAdvisors);

                for (int s = 0; s < karvySchemeList.size(); s++) {
                    String folio_number = karvySchemeList.get(s)[0].toString();
                    String fund = karvySchemeList.get(s)[1].toString();
                    String scheme_code = karvySchemeList.get(s)[2].toString();

                    List<InvestorTransactionKarvy> karvySchemeWiseInvestorTransactions;
                    if (sameSchemeMultipleArnAdvisors) {
                        String brokcode = karvySchemeList.get(s)[3].toString();
                        karvySchemeWiseInvestorTransactions = allKarvyTransactions.stream()
                                .filter(trxn -> trxn.getFolio_number().equalsIgnoreCase(folio_number)
                                        && trxn.getFund().equalsIgnoreCase(fund)
                                        && trxn.getScheme_code().equalsIgnoreCase(scheme_code)
                                        && trxn.getAgent_code().equalsIgnoreCase(brokcode))
                                .collect(Collectors.toList());
                    } else {
                        karvySchemeWiseInvestorTransactions = allKarvyTransactions.stream()
                                .filter(trxn -> trxn.getFolio_number().equalsIgnoreCase(folio_number)
                                        && trxn.getFund().equalsIgnoreCase(fund)
                                        && trxn.getScheme_code().equalsIgnoreCase(scheme_code))
                                .collect(Collectors.toList());
                    }

                    karvySchemeWiseInvestorTransactions = TransactionDataUtils.removeKarvyMinusTransaction(karvySchemeWiseInvestorTransactions);
                    if (karvySchemeWiseInvestorTransactions.isEmpty()) continue;

                    InvestorSchemeWisePortfolioResponse resp = processKarvyScheme(
                            user_id,
                            karvySchemeWiseInvestorTransactions, folio_number, fund, scheme_code,
                            sip_cams, active_sip_cams, closed_sip_cams,
                            karvyPositiveTransactionArrayList, karvyNeutralTransactionArrayList,
                            karvy_dividend_list, karvy_switch_in_list, karvy_switch_out_list,
                            karvy_neutral_dividend_transaction_type_list,
                            folio_type, financial_year, financial_year_end_date_str,
                            financial_year_strt_date, fourty_days_befor_date, today, yesterday,
                            client_name, page,
                            round_dcf, cost_dcf, unit_decimal, sdf1, sdf2, format,
                            equity_cagr_list, debt_cagr_list, hybrid_cagr_list,
                            solution_cagr_list, other_cagr_list, final_cagr_list,
                            investorPortfolioResponse);

                    if (resp != null) {
                        investorSchemeWisePortfolioResponseList.add(resp);
                    }
                }
            }

            // ======================================================
            // Manual / Portfolio Transactions
            // ======================================================
            if (!folio_type.equalsIgnoreCase("MF Without other ARN")) {
                Date manualFinancialYearEndDate = sdf1.parse(financial_year_end_date_str);
                List<PortfolioTransactions> allManualTransactions =
                        loadManualTransactionPort.findAllByUserAndDateNew(user_id, client_name, manualFinancialYearEndDate);
                List<Object[]> manualSchemeList = getManualSchemeList(allManualTransactions);

                for (Object[] manualScheme : manualSchemeList) {
                    String folio_no = manualScheme[0].toString();
                    String scheme_code = manualScheme[1].toString();
                    List<PortfolioTransactions> portfolioSchemeWiseInvestorTransactions =
                            loadManualTransactionPort.findByFolioAndSchemeAndUserAndDateNew1(
                                    folio_no, scheme_code, user_id, client_name, manualFinancialYearEndDate);

                    if (portfolioSchemeWiseInvestorTransactions.isEmpty()) continue;

                    InvestorSchemeWisePortfolioResponse resp = processManualScheme(
                            portfolioSchemeWiseInvestorTransactions,
                            mf_manualPositiveTransactionArrayList, mf_manualNeutralTransactionArrayList,
                            manual_switch_in_list, manual_switch_out_list,
                            folio_type, financial_year, financial_year_end_date_str,
                            financial_year_strt_date, fourty_days_befor_date, today, yesterday,
                            client_name, page,
                            round_dcf, cost_dcf, unit_decimal, sdf1, sdf2, format,
                            equity_cagr_list, debt_cagr_list, hybrid_cagr_list,
                            solution_cagr_list, other_cagr_list, final_cagr_list,
                            investorPortfolioResponse);

                    if (resp != null) {
                        investorSchemeWisePortfolioResponseList.add(resp);
                    }
                }
            }

            // ======================================================
            // Holding-report category totals absolute return
            // ======================================================
            if (page.equalsIgnoreCase("holding-report")) {
                computeHoldingReportCategoryTotals(investorPortfolioResponse, cost_dcf);
            }

            // ======================================================
            // Total absolute return
            // ======================================================
            Double total_gain;
            Double total_invested_value;
            if (folio_type.equalsIgnoreCase("All")) {
                total_gain = investorPortfolioResponse.getTotalUnReliasedGain() + investorPortfolioResponse.getTotalReliasedGain();
                total_invested_value = investorPortfolioResponse.getTotalInvestedAmount();
            } else {
                total_gain = investorPortfolioResponse.getTotalUnReliasedGain() + investorPortfolioResponse.getTotalDividendPaid();
                total_invested_value = investorPortfolioResponse.getTotalCurrentcost();
            }
            if (total_invested_value > 0) {
                Double total_absolute_return = (total_gain / total_invested_value) * 100;
                investorPortfolioResponse.setTotalAbsoluteReturn(Double.parseDouble(cost_dcf.format(total_absolute_return)));
            } else {
                investorPortfolioResponse.setTotalAbsoluteReturn(0.0);
            }

            // ======================================================
            // Category-wise XIRR
            // ======================================================
            investorPortfolioResponse.setEquity_total_CAGR(computeXirr(equity_cagr_list, cost_dcf));
            investorPortfolioResponse.setDebt_total_CAGR(computeXirr(debt_cagr_list, cost_dcf));
            investorPortfolioResponse.setHybrid_total_CAGR(computeXirr(hybrid_cagr_list, cost_dcf));
            investorPortfolioResponse.setSolution_total_CAGR(computeXirr(solution_cagr_list, cost_dcf));
            investorPortfolioResponse.setOther_total_CAGR(computeXirr(other_cagr_list, cost_dcf));
            investorPortfolioResponse.setEquity_cagr_list(equity_cagr_list);
            investorPortfolioResponse.setDebt_cagr_list(debt_cagr_list);
            investorPortfolioResponse.setHybrid_cagr_list(hybrid_cagr_list);
            investorPortfolioResponse.setSolution_cagr_list(solution_cagr_list);
            investorPortfolioResponse.setOther_cagr_list(other_cagr_list);

            // ======================================================
            // Overall XIRR
            // ======================================================
            double xirrValue = 0;
            if (!final_cagr_list.isEmpty()) {
                final_cagr_list.sort(Comparator.comparing(XirrResponse::getTrxn_date));
                List<XirrResponse> cagr_list = new ArrayList<>(final_cagr_list);
                investorPortfolioResponse.setCagr_list(cagr_list);
                xirrValue = computeXirrRaw(final_cagr_list);
                xirrValue = xirrValue * 100;
            } else {
                investorPortfolioResponse.setCagr_list(new ArrayList<>());
            }
            investorPortfolioResponse.setTotalCAGR(Double.parseDouble(cost_dcf.format(xirrValue)));
            investorPortfolioResponse.setTotalDividend(investorPortfolioResponse.getTotalDividendPaid() + investorPortfolioResponse.getTotalDividendReinvestment());

            // ======================================================
            // Day change portfolio level
            // ======================================================
            Double portfolio_day_change_value = 0.0;
            Double portfolio_day_change_percentage = 0.0;
            if (!investorSchemeWisePortfolioResponseList.isEmpty()) {
                for (InvestorSchemeWisePortfolioResponse scheme : investorSchemeWisePortfolioResponseList) {
                    Date start_date = scheme.getInvestmentStartDate();
                    Date nav_date = scheme.getLatestNavDate();
                    if (nav_date != null && start_date != null && nav_date.compareTo(start_date) > 0) {
                        portfolio_day_change_value += scheme.getDay_change_value();
                    }
                    formatSchemeStrings(scheme, format, cost_dcf, sdf2);
                }
                portfolio_day_change_value = Double.parseDouble(cost_dcf.format(portfolio_day_change_value));
                if (investorPortfolioResponse.getTotalCurrentValue() != 0) {
                    portfolio_day_change_percentage = (portfolio_day_change_value / (investorPortfolioResponse.getTotalCurrentValue() - portfolio_day_change_value)) * 100;
                    portfolio_day_change_percentage = Double.parseDouble(cost_dcf.format(portfolio_day_change_percentage));
                }
            }
            investorPortfolioResponse.setPortfolio_day_change_percentage(portfolio_day_change_percentage);
            investorPortfolioResponse.setPortfolio_day_change_value(portfolio_day_change_value);

            investorSchemeWisePortfolioResponseList.sort(Comparator.comparing(InvestorSchemeWisePortfolioResponse::getScheme));

            Double tot_curr_cost = investorPortfolioResponse.getTotalCurrentcost();
            Double tot_curr_value = investorPortfolioResponse.getTotalCurrentValue();
            Double tot_div_reinv = investorPortfolioResponse.getTotalDividendReinvestment();
            Double tot_div_paid = investorPortfolioResponse.getTotalDividendPaid();
            Double tot_unreliased = investorPortfolioResponse.getTotalUnReliasedGain();
            Double tot_reliasedGain = investorPortfolioResponse.getTotalReliasedGain();
            Double tot_reliasedProfit = investorPortfolioResponse.getTotalReliasedGainLoss();

            investorPortfolioResponse.setTotalOriginalInvestedAmount_str(MyMFBoxUtils.formatInRupees(investorPortfolioResponse.getTotalOriginalInvestedAmount().longValue()));
            investorPortfolioResponse.setTotalSwitchInAmount_str(MyMFBoxUtils.formatInRupees(investorPortfolioResponse.getTotalSwitchInAmount().longValue()));
            investorPortfolioResponse.setTotalSwitchOutAmount_str(MyMFBoxUtils.formatInRupees(investorPortfolioResponse.getTotalSwitchOutAmount().longValue()));
            investorPortfolioResponse.setTotalRedemptionAmount_str(MyMFBoxUtils.formatInRupees(investorPortfolioResponse.getTotalRedemptionAmount().longValue()));
            investorPortfolioResponse.setTotalCurrentcost_str(MyMFBoxUtils.formatInRupees(tot_curr_cost.longValue()));
            investorPortfolioResponse.setTotalCurrentValue_str(MyMFBoxUtils.formatInRupees(tot_curr_value.longValue()));
            investorPortfolioResponse.setTotalDividendReinvestment_str(MyMFBoxUtils.formatInRupees(tot_div_reinv.longValue()));
            investorPortfolioResponse.setTotalDividendPaid_str(MyMFBoxUtils.formatInRupees(tot_div_paid.longValue()));
            investorPortfolioResponse.setTotalUnReliasedGain_str(MyMFBoxUtils.formatInRupees(tot_unreliased.longValue()));
            investorPortfolioResponse.setTotalReliasedGain_str(MyMFBoxUtils.formatInRupees(tot_reliasedGain.longValue()));
            investorPortfolioResponse.setTotalReliasedGainLoss_str(MyMFBoxUtils.formatInRupees(tot_reliasedProfit.longValue()));
            investorPortfolioResponse.setTotalDividend_str(MyMFBoxUtils.formatInRupees(tot_div_reinv.longValue() + tot_div_paid.longValue()));

            if (page.equalsIgnoreCase("holding-report")) {
                formatHoldingReportTotals(investorPortfolioResponse, format);
            }

            investorPortfolioResponse.setInvestorSchemeWisePortfolioResponses(investorSchemeWisePortfolioResponseList);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return investorPortfolioResponse;
    }

    // ==========================================================================================
    // Derived distinct scheme lists (computed from already-fetched data)
    // ==========================================================================================

    private List<Object[]> getCamsSchemeList(List<InvestorTransactionCams> allCamsTransactions, boolean sameSchemeMultipleArnAdvisors) {
        if (sameSchemeMultipleArnAdvisors) {
            LinkedHashSet<List<String>> distinct = allCamsTransactions.stream()
                    .map(t -> Arrays.asList(t.getFolio_no(), t.getProdcode(), t.getBrokcode()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return distinct.stream().map(List::toArray).collect(Collectors.toList());
        } else {
            LinkedHashSet<List<String>> distinct = allCamsTransactions.stream()
                    .map(t -> Arrays.asList(t.getFolio_no(), t.getProdcode()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return distinct.stream().map(List::toArray).collect(Collectors.toList());
        }
    }

    private List<Object[]> getKarvySchemeList(List<InvestorTransactionKarvy> allKarvyTransactions, boolean sameSchemeMultipleArnAdvisors) {
        if (sameSchemeMultipleArnAdvisors) {
            LinkedHashSet<List<String>> distinct = allKarvyTransactions.stream()
                    .map(t -> Arrays.asList(t.getFolio_number(), t.getFund(), t.getScheme_code(), t.getAgent_code()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return distinct.stream().map(List::toArray).collect(Collectors.toList());
        } else {
            LinkedHashSet<List<String>> distinct = allKarvyTransactions.stream()
                    .map(t -> Arrays.asList(t.getFolio_number(), t.getFund(), t.getScheme_code()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return distinct.stream().map(List::toArray).collect(Collectors.toList());
        }
    }

    private List<Object[]> getManualSchemeList(List<PortfolioTransactions> allManualTransactions) {
        LinkedHashSet<List<String>> distinct = allManualTransactions.stream()
                .map(t -> Arrays.asList(t.getFolio_no(), t.getScheme_code()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return distinct.stream().map(List::toArray).collect(Collectors.toList());
    }

    // ==========================================================================================
    // CAMS
    // ==========================================================================================

    private InvestorSchemeWisePortfolioResponse processCamsScheme(
            Integer user_id,
            List<InvestorTransactionCams> camsSchemeWiseInvestorTransactions, String folio_no, String code,
            List<InvestorSipCams49> sip_cams, List<InvestorSipCams49> active_sip_cams, List<InvestorSipCams49> closed_sip_cams,
            List<String> camsPositiveTransactionArrayList, List<String> camsNeutralTransactionArrayList,
            List<String> cams_switch_in_list, List<String> cams_switch_out_list, List<String> cams_dividend_list,
            List<String> cams_sip_list, List<String> cams_sip_list2,
            String folio_type, String financial_year, String financial_year_end_date_str,
            Date financial_year_strt_date, Date fourty_days_befor_date, Date today, Date yesterday,
            String client_name, String page,
            DecimalFormat round_dcf, DecimalFormat cost_dcf, DecimalFormat unit_decimal, SimpleDateFormat sdf1, SimpleDateFormat sdf2, Format format,
            List<XirrResponse> equity_cagr_list, List<XirrResponse> debt_cagr_list, List<XirrResponse> hybrid_cagr_list,
            List<XirrResponse> solution_cagr_list, List<XirrResponse> other_cagr_list, List<XirrResponse> final_cagr_list,
            InvestorPortfolioResponse investorPortfolioResponse) throws Exception {

        List<InvestorMasterCams> masterCamsList = loadCamsFolioMasterPort.findByUser_idAndClient_nameNew(user_id, client_name);

        InvestorSchemeWisePortfolioResponse investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
        List<InvestorSchemeWiseTransactionResponse> investorSchemeWiseTransactionResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> freeUnitsResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> divPaidResponseList = new ArrayList<>();
        investorSchemeWisePortfolioResponse.setLoadFreeUnits(0.0);

        List<XirrResponse> xirr_list = new ArrayList<>();
        XirrResponse xirrRes = null;
        List<XirrResponse> positive_list = new ArrayList<>();
        List<XirrResponse> negative_list = new ArrayList<>();
        Date last_tran_date = null;
        Date sip_last_tran_date = null;
        Date stp_last_tran_date = null;
        Date swp_last_tran_date = null;
        Double last_tran_nav = 0.0;
        boolean dividend_sweep_in = false;
        boolean stp_in_amount_flag = false;
        boolean stp_out_amount_flag = false;
        boolean swp_amount_flag = false;
        List<String> multiple_broker_code_list = new ArrayList<>();

        InvestorSchemeWiseTransactionResponse freeUnitsResponse;

        for (int i = 0; i < camsSchemeWiseInvestorTransactions.size(); i++) {
            String trxn_type_ = camsSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
            String trxn_nature = camsSchemeWiseInvestorTransactions.get(i).getTrxn_natur().trim();
            String trxn_suffi = camsSchemeWiseInvestorTransactions.get(i).getTrxn_suffi().trim();
            String scheme = camsSchemeWiseInvestorTransactions.get(i).getScheme().trim();
            String prodcode = camsSchemeWiseInvestorTransactions.get(i).getProdcode().trim();
            Date traddate = camsSchemeWiseInvestorTransactions.get(i).getTraddate();
            Double price = camsSchemeWiseInvestorTransactions.get(i).getPurprice();
            Double units = camsSchemeWiseInvestorTransactions.get(i).getUnits();
            Double amount = camsSchemeWiseInvestorTransactions.get(i).getAmount();
            Double stamp_duty = camsSchemeWiseInvestorTransactions.get(i).getStamp_duty();
            Double total_tax = camsSchemeWiseInvestorTransactions.get(i).getTotal_tax();
            Double sip_trxnno = camsSchemeWiseInvestorTransactions.get(i).getSiptrxnno();
            String amc_code = camsSchemeWiseInvestorTransactions.get(i).getAmc_code();
            String broker_code = camsSchemeWiseInvestorTransactions.get(i).getBrokcode();
            String swflag = camsSchemeWiseInvestorTransactions.get(i).getSwflag();
            if (stamp_duty == null) stamp_duty = 0.0;
            if (total_tax == null) total_tax = 0.0;
            if (broker_code == null) broker_code = "";

            if (!multiple_broker_code_list.contains(broker_code)) {
                multiple_broker_code_list.add(broker_code);
            }

            if (i == 0) {
                investorSchemeWisePortfolioResponse.setScheme(scheme);
                investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme);
                investorSchemeWisePortfolioResponse.setScheme_code(prodcode);
                investorSchemeWisePortfolioResponse.setFoliono(camsSchemeWiseInvestorTransactions.get(i).getFolio_no());
                investorSchemeWisePortfolioResponse.setAmc_code(amc_code);
                investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                investorSchemeWisePortfolioResponse.setInvestmentReStartDate(traddate);
                investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                investorSchemeWisePortfolioResponse.setPurchaseNav(price);

                InvestorMasterCams masterCams = masterCamsList.stream()
                        .filter(x -> x.getFoliochk().equalsIgnoreCase(folio_no) && x.getProduct().equalsIgnoreCase(prodcode))
                        .findAny().orElse(null);
                if (masterCams != null) {
                    String dp_id = masterCams.getDp_id();
                    String demat_folio_flag = masterCams.getDemat();
                    if (dp_id == null) dp_id = "";
                    if (demat_folio_flag == null) demat_folio_flag = "";
                    if (demat_folio_flag.equalsIgnoreCase("Y")) {
                        investorSchemeWisePortfolioResponse.setIsDeamtAccount(true);
                    }
                    if (StringHelper.isNotEmpty(dp_id) && dp_id.length() > 13 && dp_id.length() < 17) {
                        investorSchemeWisePortfolioResponse.setIsDeamtAccount(true);
                    }
                }

                String dividend_flag = camsSchemeWiseInvestorTransactions.get(i).getReinvest_f().trim();
                if (dividend_flag.equalsIgnoreCase("N") || dividend_flag.equalsIgnoreCase("Y")) {
                    investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                    if (dividend_flag.equalsIgnoreCase("Y")) {
                        investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                    }else
                    {
                        investorSchemeWisePortfolioResponse.setIsDividendPayout(true);
                    }
                }

                investorSchemeWisePortfolioResponse.setScheme_registrar("cams");
                investorSchemeWisePortfolioResponse.setBroker_code(broker_code);
                investorSchemeWisePortfolioResponse.setEuin(camsSchemeWiseInvestorTransactions.get(i).getEuin());
            }

            investorSchemeWisePortfolioResponse.setLastTransactionDate(traddate);
            last_tran_date = traddate;
            last_tran_nav = price;

            InvestorSchemeWiseTransactionResponse investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
            investorSchemeWiseTransactionResponse.setSCHEME(scheme);
            investorSchemeWiseTransactionResponse.setTRADDATE(traddate);
            investorSchemeWiseTransactionResponse.setPURPRICE(price);
            investorSchemeWiseTransactionResponse.setUNITS(units);
            investorSchemeWiseTransactionResponse.setTOTAL_TAX(total_tax);
            investorSchemeWiseTransactionResponse.setTRXN_TYPE_(trxn_type_);
            investorSchemeWiseTransactionResponse.setTRXN_SUFFI(trxn_suffi);
            investorSchemeWiseTransactionResponse.setSTAMP_DUTY(stamp_duty);
            if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                investorSchemeWiseTransactionResponse.setAMOUNT(amount);
            } else {
                investorSchemeWiseTransactionResponse.setAMOUNT(-amount);
            }
            investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

            if (i == 0) {
                if (sip_cams != null && sip_cams.size() > 0) {
                    InvestorSipCams49 stpOutObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_no) && x.getProduct().equalsIgnoreCase(prodcode)).findAny().orElse(null);
                    if (stpOutObj != null) {
                        investorSchemeWisePortfolioResponse.setIsStpOutScheme(true);
                        investorSchemeWisePortfolioResponse.setStp_amount(stpOutObj.getAuto_amount());
                        stp_out_amount_flag = true;
                    }
                    InvestorSipCams49 stpInObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_no) && prodcode.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsStpInScheme(true);
                        investorSchemeWisePortfolioResponse.setStp_amount(stpInObj.getAuto_amount());
                        stp_in_amount_flag = true;
                    }
                    InvestorSipCams49 swpObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio_no) && x.getProduct().equalsIgnoreCase(prodcode)).findAny().orElse(null);
                    if (swpObj != null) {
                        investorSchemeWisePortfolioResponse.setIsSwpScheme(true);
                        investorSchemeWisePortfolioResponse.setSwp_amount(swpObj.getAuto_amount());
                        swp_amount_flag = true;
                    }
                }
            }

            if (!stp_out_amount_flag && trxn_type_.equalsIgnoreCase("Partial Switch Out") && trxn_nature.toLowerCase().indexOf("systematic - to") != -1 && (trxn_nature.toLowerCase().indexOf("instalment") != -1 || trxn_nature.indexOf("/") != -1)) {
                investorSchemeWisePortfolioResponse.setIsStpOutScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
            }
            if (!stp_in_amount_flag && trxn_type_.equalsIgnoreCase("Switch In") && swflag.equalsIgnoreCase("NA") && trxn_nature.toLowerCase().indexOf("systematic - from") != -1 && (trxn_nature.toLowerCase().indexOf("instalment") != -1 || trxn_nature.indexOf("/") != -1)) {
                investorSchemeWisePortfolioResponse.setIsStpInScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
            }
            if (!swp_amount_flag && trxn_type_.equalsIgnoreCase("Partial Redemption") && trxn_nature.toLowerCase().indexOf("systematic") != -1) {
                investorSchemeWisePortfolioResponse.setIsSwpScheme(true);
                investorSchemeWisePortfolioResponse.setSwp_amount(amount);
            }

            if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0 && !folio_type.equalsIgnoreCase("All")) {
                    investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                    investorSchemeWisePortfolioResponse.setTotalInflow(0);
                    investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                    investorSchemeWisePortfolioResponse.setTotalUnits(0);
                    investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                    investorSchemeWisePortfolioResponse.setDividendPaid(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                    investorSchemeWisePortfolioResponse.setLastInvestmentDate(traddate);
                    investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                    investorSchemeWisePortfolioResponse.setInvestmentReStartDate(traddate);
                    xirr_list = new ArrayList<>();
                    xirrRes = null;
                }

                Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                added_units = Double.parseDouble(unit_decimal.format(added_units));
                investorSchemeWisePortfolioResponse.setTotalUnits(added_units);

                Double adjust_price = price;
                String segre_percent = MfboxUtils.segregatedSchemes(prodcode);
                if (!segre_percent.isEmpty()) {
                    Date check_date = null;
                    if (amc_code.equalsIgnoreCase("T")) check_date = sdf2.parse("15-06-2019");
                    if (amc_code.equalsIgnoreCase("B")) check_date = sdf2.parse("25-11-2019");
                    if (amc_code.equalsIgnoreCase("FTI")) check_date = sdf2.parse("24-01-2020");

                    if (check_date != null && traddate.compareTo(check_date) <= 0) {
                        Double adjust_percent = (100 - Double.parseDouble(segre_percent)) / 100.0;
                        amount = amount * adjust_percent;
                        amount = Double.parseDouble(unit_decimal.format(amount));
                        adjust_price = price * adjust_percent;
                        adjust_price = Double.parseDouble(unit_decimal.format(adjust_price));
                    }
                }

                if (!cams_dividend_list.contains(trxn_type_)) {
                    if (cams_switch_in_list.contains(trxn_type_)) {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalSwitchInAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalSwitchInAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    } else {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalOriginalInvestedAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    }
                }

                if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInflow(Double.parseDouble(cost_dcf.format(total_inflow_amount)));
                    double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalInvestedAmount() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                }

                if (trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount + stamp_duty);
                    if(!investorSchemeWisePortfolioResponse.getIsDividendScheme()) {
                        investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                        investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                    }
                    if (traddate.compareTo(financial_year_strt_date) >= 0) {
                        investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest_current_fy() + amount + stamp_duty);
                    }
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest() + amount + stamp_duty);
                }

                final Double adjust_price_new = adjust_price;
                final Double stamp_duty_f = stamp_duty;
                int index = IntStream.range(0, positive_list.size()).filter(x -> positive_list.get(x).getTrxn_date().compareTo(traddate) == 0 && positive_list.get(x).getNav().compareTo(adjust_price_new) == 0 && positive_list.get(x).getTrxn_type().equalsIgnoreCase(trxn_type_)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = positive_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setCheck_units(xirrRes.getCheck_units() + units);
                    xirrRes.setStamp_duty(xirrRes.getStamp_duty() + stamp_duty_f);

                    freeUnitsResponse = freeUnitsResponseList.get(index);
                    freeUnitsResponse.setUNITS(freeUnitsResponse.getUNITS() + units);
                    freeUnitsResponse.setPurchase_units(freeUnitsResponse.getPurchase_units() + units);
                    freeUnitsResponse.setTOTAL_TAX(freeUnitsResponse.getTOTAL_TAX() + total_tax);
                    freeUnitsResponse.setSTAMP_DUTY(freeUnitsResponse.getSTAMP_DUTY() + stamp_duty_f);
                    freeUnitsResponse.setAMOUNT(freeUnitsResponse.getAMOUNT() + amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(trxn_type_);
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(adjust_price);
                    xirrRes.setCheck_units(units);
                    xirrRes.setStamp_duty(stamp_duty);
                    positive_list.add(xirrRes);

                    freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                    freeUnitsResponse.setTRADDATE(traddate);
                    freeUnitsResponse.setTRXN_TYPE_(trxn_type_);
                    freeUnitsResponse.setPURPRICE(adjust_price);
                    freeUnitsResponse.setUNITS(units);
                    freeUnitsResponse.setPurchase_units(units);
                    freeUnitsResponse.setTOTAL_TAX(total_tax);
                    freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                    freeUnitsResponse.setAMOUNT(amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    freeUnitsResponseList.add(freeUnitsResponse);
                }

            } else if (camsNeutralTransactionArrayList.contains(trxn_type_)) {
                investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount + total_tax);
                if(!investorSchemeWisePortfolioResponse.getIsDividendScheme()) {
                    investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                    investorSchemeWisePortfolioResponse.setIsDividendPayout(true);
                }
                if (traddate.compareTo(financial_year_strt_date) >= 0) {
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_paid_current_fy() + amount + total_tax);
                }
                investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount + total_tax);

                final Double priceF = price;
                InvestorSchemeWiseTransactionResponse free = divPaidResponseList.stream().filter(x -> x.getTRADDATE().compareTo(traddate) == 0 && x.getPURPRICE().compareTo(priceF) == 0 && x.getTRXN_TYPE_().equalsIgnoreCase(trxn_type_)).findAny().orElse(null);
                if (free != null) {
                    free.setUNITS(free.getUNITS() + units);
                    free.setPurchase_units(free.getPurchase_units() + units);
                    free.setTOTAL_TAX(free.getTOTAL_TAX() + total_tax);
                    free.setSTAMP_DUTY(free.getSTAMP_DUTY() + stamp_duty);
                    free.setAMOUNT(free.getAMOUNT() + amount);
                    free.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                } else {
                    freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                    freeUnitsResponse.setTRADDATE(traddate);
                    freeUnitsResponse.setTRXN_TYPE_(trxn_type_);
                    freeUnitsResponse.setPURPRICE(price);
                    freeUnitsResponse.setUNITS(units);
                    freeUnitsResponse.setPurchase_units(units);
                    freeUnitsResponse.setTOTAL_TAX(total_tax);
                    freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                    freeUnitsResponse.setAMOUNT(amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    divPaidResponseList.add(freeUnitsResponse);
                }
            } else {
                if (cams_switch_out_list.contains(trxn_type_)) {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalSwitchOutAmount() + amount + camsSchemeWiseInvestorTransactions.get(i).getTotal_tax();
                    investorSchemeWisePortfolioResponse.setTotalSwitchOutAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                } else {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalRedemptionAmount() + amount + camsSchemeWiseInvestorTransactions.get(i).getTotal_tax();
                    investorSchemeWisePortfolioResponse.setTotalRedemptionAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                }

                investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount + camsSchemeWiseInvestorTransactions.get(i).getTotal_tax());
                Double added_units_neg = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                added_units_neg = Double.parseDouble(unit_decimal.format(added_units_neg));
                investorSchemeWisePortfolioResponse.setTotalUnits(added_units_neg);

                final Double added_units_final = added_units_neg;
                final Double priceF2 = price;
                int index = IntStream.range(0, negative_list.size()).filter(x -> negative_list.get(x).getTrxn_date().compareTo(traddate) == 0 && negative_list.get(x).getNav().compareTo(priceF2) == 0 && negative_list.get(x).getTrxn_type().equalsIgnoreCase(trxn_type_)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = negative_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setStt(xirrRes.getStt() + camsSchemeWiseInvestorTransactions.get(i).getStt());
                    xirrRes.setTotal_units(added_units_final);
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(trxn_type_);
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(price);
                    xirrRes.setStt(camsSchemeWiseInvestorTransactions.get(i).getStt());
                    xirrRes.setTotal_units(added_units_final);
                    negative_list.add(xirrRes);
                }
            }

            if ((cams_sip_list.contains(trxn_type_) && trxn_nature.toLowerCase().indexOf("systematic") != -1) || (sip_trxnno != 0 && cams_sip_list2.contains(trxn_type_))) {
                investorSchemeWisePortfolioResponse.setIsSipScheme(true);
                investorSchemeWisePortfolioResponse.setSip_amount(amount);
                sip_last_tran_date = traddate;
            } else if (investorSchemeWisePortfolioResponse.getIsSipScheme() && (sip_trxnno != 0 && cams_sip_list2.contains(trxn_type_))) {
                sip_last_tran_date = traddate;
            }

            if (investorSchemeWisePortfolioResponse.getIsSwpScheme() && trxn_type_.equalsIgnoreCase("Partial Redemption") && trxn_nature.toLowerCase().indexOf("systematic") != -1) {
                swp_last_tran_date = traddate;
            }
            if (investorSchemeWisePortfolioResponse.getIsStpInScheme() && trxn_type_.equalsIgnoreCase("Switch In")) {
                stp_last_tran_date = traddate;
            }
            if (investorSchemeWisePortfolioResponse.getIsStpOutScheme() && trxn_type_.equalsIgnoreCase("Partial Switch Out")) {
                stp_last_tran_date = traddate;
            }

            if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setAmount(-(amount + stamp_duty));
                    xirr_list.add(xirrRes);
                } else if (trxn_type_.equalsIgnoreCase("Dividend Reinvest") && investorSchemeWisePortfolioResponse.getTotalInvestedAmount() == 0) {
                    dividend_sweep_in = true;
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setAmount(-(amount + stamp_duty));
                    xirr_list.add(xirrRes);
                }
            } else {
                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(traddate);
                xirrRes.setAmount(amount + total_tax);
                xirr_list.add(xirrRes);
            }
        }

        investorSchemeWisePortfolioResponse.setMultiple_broker_code(String.join(",", multiple_broker_code_list));

        Double realisedAllRedeemedGainLoss = 0.0;
        Double realisedGainLoss = 0.0;
        Double div_reinvest_value = 0.0;
        if (negative_list.size() > 0) {
            Comparator<XirrResponse> compareByValues = Comparator.comparing(XirrResponse::getTrxn_date)
                    .thenComparing(Comparator.comparing(XirrResponse::getTotal_units).reversed());
            List<XirrResponse> sortedNegative_list = negative_list.stream().sorted(compareByValues).collect(Collectors.toList());

            for (XirrResponse xirr : sortedNegative_list) {
                Double purchase_nav;
                Double purchase_units;
                Double total_units = xirr.getTotal_units();
                Double sold_nav = xirr.getNav();
                Double sold_units = xirr.getUnits();
                Double total_tax = xirr.getStt();
                Double reliased_gain = 0.0;
                Double orgi_units;
                Double orgi_amt;
                Double stamp_duty;
                String trxn_type;

                for (int k = 0; k < positive_list.size(); k++) {
                    xirrRes = positive_list.get(k);
                    purchase_units = xirrRes.getCheck_units();
                    trxn_type = xirrRes.getTrxn_type();

                    if (purchase_units > 0) {
                        Double remain_units = purchase_units - sold_units;
                        remain_units = Double.parseDouble(unit_decimal.format(remain_units));

                        if (remain_units >= 0) {
                            xirrRes.setCheck_units(remain_units);
                            purchase_nav = xirrRes.getNav();
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + (((sold_nav - purchase_nav) * sold_units) - total_tax);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            if (remain_units <= 0) {
                                free.setUNITS(0.0);
                                free.setTOTAL_UNITS(0.0);
                            } else {
                                free.setUNITS(remain_units);
                                free.setTOTAL_UNITS(remain_units);
                            }
                            if (trxn_type.equalsIgnoreCase("Dividend Reinvest")) {
                                div_reinvest_value += (purchase_nav * sold_units);
                            }
                            break;
                        } else {
                            purchase_nav = xirrRes.getNav();
                            Double current_units = xirrRes.getCheck_units();
                            sold_units = sold_units - current_units;
                            sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + ((sold_nav - purchase_nav) * current_units);
                            xirrRes.setCheck_units(0.0);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            free.setUNITS(0.0);
                            free.setTOTAL_UNITS(0.0);
                            if (trxn_type.equalsIgnoreCase("Dividend Reinvest")) {
                                div_reinvest_value += (purchase_nav * current_units);
                            }
                        }
                    }
                }
                reliased_gain = Double.parseDouble(cost_dcf.format(reliased_gain));
                realisedAllRedeemedGainLoss = realisedAllRedeemedGainLoss + reliased_gain;
                realisedGainLoss = realisedGainLoss + reliased_gain;

                if (total_units == 0) {
                    realisedGainLoss = 0.0;
                }
            }
        }
        realisedGainLoss = Double.parseDouble(round_dcf.format(realisedGainLoss));
        realisedAllRedeemedGainLoss = Double.parseDouble(round_dcf.format(realisedAllRedeemedGainLoss));
        div_reinvest_value = Double.parseDouble(cost_dcf.format(div_reinvest_value));

        investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
        investorSchemeWisePortfolioResponse.setRealisedProfitLoss(realisedAllRedeemedGainLoss + investorSchemeWisePortfolioResponse.getDividendPaid());
        investorSchemeWisePortfolioResponse.setRealisedGainLoss(realisedGainLoss + investorSchemeWisePortfolioResponse.getDividendPaid());

        if (sip_last_tran_date != null && sip_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
            investorSchemeWisePortfolioResponse.setIsActiveSipScheme(true);
        }
        if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
            String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
            String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
            InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SIP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
            if (sipObj != null) {
                investorSchemeWisePortfolioResponse.setIsActiveSipScheme(false);
            }
        }
        if (active_sip_cams != null && active_sip_cams.size() > 0) {
            String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
            String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
            InvestorSipCams49 sipObj = active_sip_cams.stream().filter(x -> x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
            if (sipObj != null) {
                investorSchemeWisePortfolioResponse.setIsActiveSipScheme(true);
                Double total_sip_amount = active_sip_cams.stream().filter(x -> x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).mapToDouble(InvestorSipCams49::getAuto_amount).sum();
                investorSchemeWisePortfolioResponse.setSip_amount(total_sip_amount);
            }
        }
        if (stp_last_tran_date != null) {
            if (stp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
                investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
            }
            if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
                String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
                String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveStpScheme(false);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                } else {
                    InvestorSipCams49 stpInObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && scheme_code.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsActiveStpScheme(false);
                        investorSchemeWisePortfolioResponse.setSip_frequency(stpInObj.getPeriodicit());
                    }
                }
            }
            if (sip_cams != null && sip_cams.size() > 0) {
                String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
                String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                } else {
                    InvestorSipCams49 stpInObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && scheme_code.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
                        investorSchemeWisePortfolioResponse.setSip_frequency(stpInObj.getPeriodicit());
                    }
                }
            }
        }
        if (swp_last_tran_date != null) {
            if (swp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
                investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(true);
            }
            if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
                String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
                String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(false);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                }
            }
            if (sip_cams != null && sip_cams.size() > 0) {
                String folio_number = investorSchemeWisePortfolioResponse.getFoliono();
                String scheme_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(scheme_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(true);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                }
            }
        }

        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Date traddate = free.getTRADDATE();
            Double units = free.getUNITS();
            Calendar cal = Calendar.getInstance();
            cal.setTime(traddate);
            cal.add(Calendar.YEAR, 3);
            Date trxn_date_after3years = cal.getTime();
            if (trxn_date_after3years.compareTo(today) < 0) {
                Double added_units = investorSchemeWisePortfolioResponse.getLoadFreeUnits() + units;
                added_units = Double.parseDouble(unit_decimal.format(added_units));
                investorSchemeWisePortfolioResponse.setLoadFreeUnits(added_units);
            }
        }

        if (!folio_type.equalsIgnoreCase("All")) {
            Double tot_units = 0.0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                if (free.getUNITS() > 0) {
                    if (tot_units == 0) {
                        Double amount = (free.getUNITS() * free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentReStartDate(free.getTRADDATE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setPurchaseNav(free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                    }
                    tot_units = tot_units + free.getUNITS();
                    free.setTOTAL_UNITS(tot_units);
                }
            }
            freeUnitsResponseList.addAll(divPaidResponseList);
            freeUnitsResponseList.sort(Comparator.comparing(InvestorSchemeWiseTransactionResponse::getTRADDATE));

            xirr_list = new ArrayList<>();
            xirrRes = null;
            Double live_current_cost = 0.0;
            Double div_reinvest_amt = 0.0;
            Double div_paid_amt = 0.0;
            Double scheme_total_units = 0.0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                Date traddate = free.getTRADDATE();
                String trxn_type_ = free.getTRXN_TYPE_();
                Double nav = free.getPURPRICE();
                Double units = free.getUNITS();
                Double purchase_units = free.getPurchase_units();
                Double amount = (units * nav);
                Double tax = free.getTOTAL_TAX();
                Double stamp_duty = free.getSTAMP_DUTY();
                Double pur_amount = free.getAMOUNT();
                if (units > 0 && units < purchase_units) {
                    stamp_duty = units * (stamp_duty / purchase_units);
                }
                if (amount > pur_amount) {
                    amount = pur_amount;
                } else if (amount < pur_amount) {
                    if ((pur_amount - amount) <= 5) {
                        amount = pur_amount;
                    }
                }

                if (units != 0) {
                    if (camsPositiveTransactionArrayList.contains(trxn_type_)) {
                        scheme_total_units = free.getTOTAL_UNITS();
                        if (trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                            div_reinvest_amt = div_reinvest_amt + amount + stamp_duty;
                        }
                        if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                            live_current_cost = live_current_cost + amount + stamp_duty;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(-(amount + stamp_duty));
                            xirr_list.add(xirrRes);
                        } else if (trxn_type_.equalsIgnoreCase("Dividend Reinvest") && dividend_sweep_in) {
                            live_current_cost = live_current_cost + amount + stamp_duty;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(-(amount + stamp_duty));
                            xirr_list.add(xirrRes);
                        } else if (trxn_type_.equalsIgnoreCase("Dividend Reinvest") && live_current_cost <= 0) {
                            dividend_sweep_in = true;
                            live_current_cost = live_current_cost + amount + stamp_duty;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(-(amount + stamp_duty));
                            xirr_list.add(xirrRes);
                        }
                    }
                }

                if (camsNeutralTransactionArrayList.contains(trxn_type_)) {
                    if (live_current_cost > 0) {
                        amount = free.getAMOUNT() + tax;
                        Double div_total_units = free.getTOTAL_UNITS();
                        if (!scheme_total_units.equals(div_total_units)) {
                            Double div_per_unit = 0.0;
                            if (div_total_units > 0) {
                                div_per_unit = (amount / div_total_units);
                            }
                            amount = div_per_unit * scheme_total_units;
                        }
                        div_paid_amt = div_paid_amt + amount;
                        xirrRes = new XirrResponse();
                        xirrRes.setTrxn_date(traddate);
                        xirrRes.setAmount(amount);
                        xirr_list.add(xirrRes);
                    }
                }
            }

            Double current_cost = live_current_cost;
            if (current_cost <= 0) current_cost = 0.0;
            current_cost = Double.parseDouble(round_dcf.format(current_cost));
            div_reinvest_amt = Double.parseDouble(round_dcf.format(div_reinvest_amt));
            div_paid_amt = Double.parseDouble(round_dcf.format(div_paid_amt));
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            investorSchemeWisePortfolioResponse.setTotalInvestedAmount(current_cost);
            investorSchemeWisePortfolioResponse.setDividendReinvestment(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setDividendPaid(div_paid_amt);
        }

        if (last_tran_date == null) {
            last_tran_date = today;
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() > 0) {
            applyAmfiSchemeMapping(investorSchemeWisePortfolioResponse, "CAMS", client_name, financial_year,
                    financial_year_end_date_str, page, sdf1, cost_dcf, last_tran_nav, last_tran_date);

            if (investorSchemeWisePortfolioResponse.getScheme() != null &&
                    investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                xirr_list = new ArrayList<>();
            }

            if (folio_type.equalsIgnoreCase("All")) {
                Double current_cost = (investorSchemeWisePortfolioResponse.getTotalInflow() - investorSchemeWisePortfolioResponse.getTotalOutflow()) + realisedAllRedeemedGainLoss;
                if (current_cost <= 0) current_cost = 0.0;
                current_cost = Double.parseDouble(round_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            }

            Double purchase_nav = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment() / investorSchemeWisePortfolioResponse.getTotalUnits();
            purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
            if (purchase_nav > 0) {
                investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0) {
                Double current_value = investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav();
                current_value = Double.parseDouble(round_dcf.format(current_value));
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(current_value);
            } else {
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0 && investorSchemeWisePortfolioResponse.getPrev_latestNav() > 0) {
                if (investorSchemeWisePortfolioResponse.getLatestNavDate().compareTo(last_tran_date) == 0 && camsSchemeWiseInvestorTransactions.size() == 1) {
                    investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
                } else {
                    Double day_change_value = (investorSchemeWisePortfolioResponse.getLatestNav() - investorSchemeWisePortfolioResponse.getPrev_latestNav()) * investorSchemeWisePortfolioResponse.getTotalUnits();
                    day_change_value = Double.parseDouble(cost_dcf.format(day_change_value));
                    investorSchemeWisePortfolioResponse.setDay_change_value(day_change_value);
                }
            } else {
                investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
            }
        } else {
            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
            investorSchemeWisePortfolioResponse.setPrev_latestNav(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_percentage(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
        }

        if (investorSchemeWisePortfolioResponse.getScheme_class() == null) {
            investorSchemeWisePortfolioResponse.setScheme_class("");
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(0);
            investorSchemeWisePortfolioResponse.setTotalUnits(0);
            investorSchemeWisePortfolioResponse.setTotalCurrentValue(0);
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(0);

            applyAmfiSchemeMappingBasicsOnly(investorSchemeWisePortfolioResponse, "CAMS", client_name);
            if (investorSchemeWisePortfolioResponse.getScheme() != null &&
                    investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                xirr_list = new ArrayList<>();
            }

            if (folio_type.equalsIgnoreCase("All") && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() != 0) {
                addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                        equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
                final_cagr_list.addAll(xirr_list);
            }
            investorSchemeWisePortfolioResponse.setAverage_days(0);
        } else {
            xirrRes = new XirrResponse();
            xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
            xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
            xirr_list.add(xirrRes);

            Double unrealisedProfitLoss = investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            unrealisedProfitLoss = Double.parseDouble(round_dcf.format(unrealisedProfitLoss));
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(unrealisedProfitLoss);

            Double gain;
            Double invested_value;
            if (folio_type.equalsIgnoreCase("All")) {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss();
                invested_value = investorSchemeWisePortfolioResponse.getTotalInvestedAmount();
            } else {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getDividendPaid();
                invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            }
            if (invested_value > 0) {
                Double absolute_return = gain / invested_value;
                absolute_return = absolute_return * 100;
                absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
            } else if (invested_value == 0 && dividend_sweep_in) {
                invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
                if (invested_value > 0) {
                    if (folio_type.equalsIgnoreCase("All")) {
                        gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss();
                    } else {
                        gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getDividendPaid();
                    }
                    Double absolute_return = gain / invested_value;
                    absolute_return = absolute_return * 100;
                    absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                    investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
                } else {
                    investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
                }
            } else {
                investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
            }

            addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                    equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
            final_cagr_list.addAll(xirr_list);

            Integer lt_st_total = 0, lt_st_count = 0, lt_st_avg = 0;
            for (XirrResponse res : positive_list) {
                Double purchase_units = res.getCheck_units();
                if (purchase_units > 0) {
                    int days = getDaysBetweenDates(res.getTrxn_date(),yesterday);
                    lt_st_total += days;
                    lt_st_count += 1;
                }
            }
            if (lt_st_count > 0 && lt_st_total > 0) {
                lt_st_avg = lt_st_total / lt_st_count;
            }
            investorSchemeWisePortfolioResponse.setAverage_days(lt_st_avg);
        }

        String scheme_class = investorSchemeWisePortfolioResponse.getScheme_class();
        if (scheme_class == null || StringHelper.isEmpty(scheme_class)) {
            scheme_class = "Equity Schemes";
        }
        Integer checking_days = (scheme_class.equalsIgnoreCase("Debt Schemes") ||
                "Equity: ELSS".equalsIgnoreCase(investorSchemeWisePortfolioResponse.getScheme_advisorkhoj_category()))
                ? 1095 : 365;

        Double long_term_units = 0.0;
        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Double purchase_units = free.getUNITS();
            if (purchase_units > 0) {
                int days = getDaysBetweenDates(free.getTRADDATE(), yesterday);
                if (days > checking_days) {
                    long_term_units = long_term_units + purchase_units;
                }
            }
        }
        long_term_units = Double.parseDouble(unit_decimal.format(long_term_units));
        investorSchemeWisePortfolioResponse.setLong_term_units(long_term_units);
        Double long_term_amount = long_term_units * investorSchemeWisePortfolioResponse.getLatestNav();
        long_term_amount = Double.parseDouble(cost_dcf.format(long_term_amount));
        investorSchemeWisePortfolioResponse.setLong_term_amount(long_term_amount);

        if ((investorSchemeWisePortfolioResponse.getTotalUnits() <= 0 && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() == 0) || xirr_list.size() == 0) {
            investorSchemeWisePortfolioResponse.setCagr(0);
        } else {
            investorSchemeWisePortfolioResponse.setCagr(computeSchemeXirr(xirr_list, cost_dcf));
        }
        investorSchemeWisePortfolioResponse.setCashflow_list(new ArrayList<>(xirr_list));

        if ((folio_type.equalsIgnoreCase("Live") || folio_type.equalsIgnoreCase("NonSegregated") || folio_type.equalsIgnoreCase("MF Without other ARN"))
                && investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            return null;
        }

        if (page.equalsIgnoreCase("holding-report")) {
            addToHoldingReportTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);
        }

        accumulatePortfolioTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);

        investorSchemeWisePortfolioResponse.setXirr_list(xirr_list);
        if (sip_last_tran_date != null) {
            investorSchemeWisePortfolioResponse.setLastTransactionDate(stp_last_tran_date);
        } else if (stp_last_tran_date == null) {
            investorSchemeWisePortfolioResponse.setLastTransactionDate(stp_last_tran_date);
        } else if (swp_last_tran_date == null) {
            investorSchemeWisePortfolioResponse.setLastTransactionDate(swp_last_tran_date);
        }

        return investorSchemeWisePortfolioResponse;
    }

    // ==========================================================================================
    // KARVY
    // ==========================================================================================

    private InvestorSchemeWisePortfolioResponse processKarvyScheme(
            Integer user_id,
            List<InvestorTransactionKarvy> karvySchemeWiseInvestorTransactions, String folio_number, String fund, String scheme_code,
            List<InvestorSipCams49> sip_cams, List<InvestorSipCams49> active_sip_cams, List<InvestorSipCams49> closed_sip_cams,
            List<String> karvyPositiveTransactionArrayList, List<String> karvyNeutralTransactionArrayList,
            List<String> karvy_dividend_list, List<String> karvy_switch_in_list, List<String> karvy_switch_out_list,
            List<String> karvy_neutral_dividend_transaction_type_list,
            String folio_type, String financial_year, String financial_year_end_date_str,
            Date financial_year_strt_date, Date fourty_days_befor_date, Date today, Date yesterday,
            String client_name, String page,
            DecimalFormat round_dcf, DecimalFormat cost_dcf, DecimalFormat unit_decimal, SimpleDateFormat sdf1, SimpleDateFormat sdf2, Format format,
            List<XirrResponse> equity_cagr_list, List<XirrResponse> debt_cagr_list, List<XirrResponse> hybrid_cagr_list,
            List<XirrResponse> solution_cagr_list, List<XirrResponse> other_cagr_list, List<XirrResponse> final_cagr_list,
            InvestorPortfolioResponse investorPortfolioResponse) throws Exception {

        List<InvestorMasterKarvy> masterKarvyList = loadKarvyFolioMasterPort.findByUser_idAndClient_nameNew(user_id, client_name);

        InvestorSchemeWisePortfolioResponse investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
        List<InvestorSchemeWiseTransactionResponse> investorSchemeWiseTransactionResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> freeUnitsResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> divPaidResponseList = new ArrayList<>();
        investorSchemeWisePortfolioResponse.setLoadFreeUnits(0.0);

        List<XirrResponse> xirr_list = new ArrayList<>();
        XirrResponse xirrRes = null;
        List<XirrResponse> positive_list = new ArrayList<>();
        List<XirrResponse> negative_list = new ArrayList<>();
        Date last_tran_date = null;
        Date sip_last_tran_date = null;
        Date stp_last_tran_date = null;
        Date swp_last_tran_date = null;
        Double last_tran_nav = 0.0;
        boolean transfer_flag = false;
        boolean stp_in_amount_flag = false;
        boolean stp_out_amount_flag = false;
        boolean swp_amount_flag = false;
        List<String> multiple_broker_code_list = new ArrayList<>();
        String fund_name = "";

        InvestorSchemeWiseTransactionResponse freeUnitsResponse;

        for (int i = 0; i < karvySchemeWiseInvestorTransactions.size(); i++) {
            Date transaction_date = karvySchemeWiseInvestorTransactions.get(i).getTransaction_date();
            Double purchase_price = karvySchemeWiseInvestorTransactions.get(i).getPrice() != null ? karvySchemeWiseInvestorTransactions.get(i).getPrice() : 0.0;
            Double units = karvySchemeWiseInvestorTransactions.get(i).getUnits() != null ? karvySchemeWiseInvestorTransactions.get(i).getUnits() : 0.0;
            String transaction_description = karvySchemeWiseInvestorTransactions.get(i).getTransaction_description().trim();
            Double amount = karvySchemeWiseInvestorTransactions.get(i).getAmount();
            Double stamp_duty = karvySchemeWiseInvestorTransactions.get(i).getStampduty();
            String tr_charges = karvySchemeWiseInvestorTransactions.get(i).getTrcharges();
            String remarks = karvySchemeWiseInvestorTransactions.get(i).getRemarks().trim();
            String fund_description = karvySchemeWiseInvestorTransactions.get(i).getFund_description().trim();
            String prodcode = karvySchemeWiseInvestorTransactions.get(i).getProduct_code().trim();
            String broker_code = karvySchemeWiseInvestorTransactions.get(i).getAgent_code();
            Double tds_amount = karvySchemeWiseInvestorTransactions.get(i).getTdsamount() != null ? karvySchemeWiseInvestorTransactions.get(i).getTdsamount() : 0.0;
            if (stamp_duty == null) stamp_duty = 0.0;
            if (tr_charges == null) tr_charges = "";
            if (!tr_charges.isEmpty() && tr_charges.matches("\\d+(\\.\\d+)?") && !tr_charges.equalsIgnoreCase("LPS") && Double.parseDouble(tr_charges) > 0) {
                stamp_duty = stamp_duty + Double.parseDouble(tr_charges);
            }

            if (transaction_description.equalsIgnoreCase("Bonus") || transaction_description.equalsIgnoreCase("Bonus Units") || transaction_description.equalsIgnoreCase("Bonus Rejection") || transaction_description.equalsIgnoreCase("Bonus Rejection Reversal")) {
                if (amount == 0) {
                    purchase_price = 0.0;
                    amount = 0.0;
                    stamp_duty = 0.0;
                }
            }
            if (broker_code == null) broker_code = "";
            if (!multiple_broker_code_list.contains(broker_code)) {
                multiple_broker_code_list.add(broker_code);
            }

            if (i == 0) {
                investorSchemeWisePortfolioResponse.setScheme(fund_description);
                investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(fund_description);
                investorSchemeWisePortfolioResponse.setScheme_code(karvySchemeWiseInvestorTransactions.get(i).getFund() + karvySchemeWiseInvestorTransactions.get(i).getScheme_code());
                investorSchemeWisePortfolioResponse.setFoliono(karvySchemeWiseInvestorTransactions.get(i).getFolio_number().trim());
                investorSchemeWisePortfolioResponse.setAmc_code(karvySchemeWiseInvestorTransactions.get(i).getFund());
                String scheme_dividend = karvySchemeWiseInvestorTransactions.get(i).getDividend_option().trim();
                if (scheme_dividend.equalsIgnoreCase("D") || scheme_dividend.equalsIgnoreCase("R")) {
                    investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                    if (scheme_dividend.equalsIgnoreCase("R")) {
                        investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                    }else{
                        investorSchemeWisePortfolioResponse.setIsDividendPayout(true);
                    }
                }
                String product_code = karvySchemeWiseInvestorTransactions.get(i).getFund() + karvySchemeWiseInvestorTransactions.get(i).getScheme_code();
                InvestorMasterKarvy masterKarvy = masterKarvyList.stream().filter(x -> x.getFolio().equalsIgnoreCase(folio_number) && x.getProduct_code().equalsIgnoreCase(product_code)).findAny().orElse(null);
                if (masterKarvy != null) {
                    fund_name = masterKarvy.getFund_description();
                    String dp_id = masterKarvy.getDp_id();
                    if (fund_name == null) fund_name = "";
                    if (dp_id == null) dp_id = "";
                    if (StringHelper.isNotEmpty(dp_id) && dp_id.length() == 8) {
                        investorSchemeWisePortfolioResponse.setIsDeamtAccount(true);
                    }
                } else {
                    fund_name = fund_description;
                }
                investorSchemeWisePortfolioResponse.setInvestmentStartNav(purchase_price);
                investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                investorSchemeWisePortfolioResponse.setInvestmentReStartDate(transaction_date);
                investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_price);
                investorSchemeWisePortfolioResponse.setScheme_registrar("karvy");
                investorSchemeWisePortfolioResponse.setBroker_code(broker_code);
                investorSchemeWisePortfolioResponse.setEuin(karvySchemeWiseInvestorTransactions.get(i).getEuin());
            }

            investorSchemeWisePortfolioResponse.setLastTransactionDate(transaction_date);
            last_tran_date = transaction_date;
            last_tran_nav = purchase_price;

            InvestorSchemeWiseTransactionResponse investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
            investorSchemeWiseTransactionResponse.setSCHEME(fund_name);
            investorSchemeWiseTransactionResponse.setTRADDATE(transaction_date);
            investorSchemeWiseTransactionResponse.setPURPRICE(purchase_price);
            investorSchemeWiseTransactionResponse.setUNITS(units);
            investorSchemeWiseTransactionResponse.setTOTAL_TAX(tds_amount);
            investorSchemeWiseTransactionResponse.setTRXN_TYPE_(transaction_description);
            investorSchemeWiseTransactionResponse.setTRXN_SUFFI(remarks);
            investorSchemeWiseTransactionResponse.setSTAMP_DUTY(karvySchemeWiseInvestorTransactions.get(i).getStampduty());
            if (karvyPositiveTransactionArrayList.contains(transaction_description)) {
                investorSchemeWiseTransactionResponse.setAMOUNT(amount);
            } else {
                investorSchemeWiseTransactionResponse.setAMOUNT(-amount);
            }
            investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

            if (i == 0) {
                if (sip_cams != null && sip_cams.size() > 0) {
                    InvestorSipCams49 stpOutObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(prodcode)).findAny().orElse(null);
                    if (stpOutObj != null) {
                        investorSchemeWisePortfolioResponse.setIsStpOutScheme(true);
                        investorSchemeWisePortfolioResponse.setStp_amount(stpOutObj.getAuto_amount());
                        stp_out_amount_flag = true;
                    }
                    InvestorSipCams49 stpInObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio_number) && prodcode.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsStpInScheme(true);
                        investorSchemeWisePortfolioResponse.setStp_amount(stpInObj.getAuto_amount());
                        stp_in_amount_flag = true;
                    }
                    InvestorSipCams49 swpObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio_number) && x.getProduct().equalsIgnoreCase(prodcode)).findAny().orElse(null);
                    if (swpObj != null) {
                        investorSchemeWisePortfolioResponse.setIsSwpScheme(true);
                        investorSchemeWisePortfolioResponse.setSwp_amount(swpObj.getAuto_amount());
                        swp_amount_flag = true;
                    }
                }
            }

            if (!stp_out_amount_flag && (transaction_description.equalsIgnoreCase("S T P Out") || transaction_description.equalsIgnoreCase("STRIP Out"))) {
                investorSchemeWisePortfolioResponse.setIsStpOutScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
            }
            if (!stp_in_amount_flag && (transaction_description.equalsIgnoreCase("S T P In") || transaction_description.equalsIgnoreCase("STRIP In"))) {
                investorSchemeWisePortfolioResponse.setIsStpInScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
            }
            if (!swp_amount_flag && transaction_description.equalsIgnoreCase("Systematic Withdrawal")) {
                investorSchemeWisePortfolioResponse.setIsSwpScheme(true);
                investorSchemeWisePortfolioResponse.setSwp_amount(amount);
            }

            if (karvyPositiveTransactionArrayList.contains(transaction_description)) {
                if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0 && !folio_type.equalsIgnoreCase("All")) {
                    investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                    investorSchemeWisePortfolioResponse.setTotalInflow(0);
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                    investorSchemeWisePortfolioResponse.setTotalUnits(0);
                    investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                    investorSchemeWisePortfolioResponse.setDividendPaid(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                    investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                    investorSchemeWisePortfolioResponse.setInvestmentReStartDate(transaction_date);
                    xirr_list = new ArrayList<>();
                    xirrRes = null;
                    freeUnitsResponseList = new ArrayList<>();
                    divPaidResponseList = new ArrayList<>();
                    positive_list = new ArrayList<>();
                    negative_list = new ArrayList<>();
                }

                Double current_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                current_units = Double.parseDouble(unit_decimal.format(current_units));
                investorSchemeWisePortfolioResponse.setTotalUnits(current_units);

                if (!karvy_dividend_list.contains(transaction_description)) {
                    if (karvy_switch_in_list.contains(transaction_description)) {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalSwitchInAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalSwitchInAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    } else {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalOriginalInvestedAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    }
                }

                if (!transaction_description.equalsIgnoreCase("Div. Reinvestment") && !transaction_description.equalsIgnoreCase("ReInvestment Rejection") && !transaction_description.equalsIgnoreCase("Bonus Units")) {
                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInflow(Double.parseDouble(cost_dcf.format(total_inflow_amount)));
                    double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalInvestedAmount() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                }

                if (transaction_description.equalsIgnoreCase("Div. Reinvestment") || transaction_description.equalsIgnoreCase("ReInvestment Rejection") || transaction_description.equalsIgnoreCase("ReInvestment Rejection Reversal") || transaction_description.equalsIgnoreCase("Dividend Sweep In") || transaction_description.equalsIgnoreCase("Dividend Sweep In Rej.")) {
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount + stamp_duty);
                    if(!investorSchemeWisePortfolioResponse.getIsDividendScheme()) {
                    investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                    investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                    }
                    if (transaction_date.compareTo(financial_year_strt_date) >= 0) {
                        investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest_current_fy() + amount + stamp_duty);
                    }
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest() + amount + stamp_duty);
                }

                final Double priceF = purchase_price;
                final List<XirrResponse> positive_list_ref = positive_list;
                int index = IntStream.range(0, positive_list_ref.size()).filter(x -> positive_list_ref.get(x).getTrxn_date().compareTo(transaction_date) == 0 && positive_list_ref.get(x).getNav().compareTo(priceF) == 0 && positive_list_ref.get(x).getTrxn_type().equalsIgnoreCase(transaction_description)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = positive_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setCheck_units(xirrRes.getCheck_units() + units);
                    xirrRes.setStamp_duty(xirrRes.getStamp_duty() + stamp_duty);

                    freeUnitsResponse = freeUnitsResponseList.get(index);
                    freeUnitsResponse.setUNITS(freeUnitsResponse.getUNITS() + units);
                    freeUnitsResponse.setPurchase_units(freeUnitsResponse.getPurchase_units() + units);
                    freeUnitsResponse.setTOTAL_TAX(freeUnitsResponse.getTOTAL_TAX() + tds_amount);
                    freeUnitsResponse.setSTAMP_DUTY(freeUnitsResponse.getSTAMP_DUTY() + stamp_duty);
                    freeUnitsResponse.setAMOUNT(freeUnitsResponse.getAMOUNT() + amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(transaction_description);
                    xirrRes.setTrxn_date(transaction_date);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(purchase_price);
                    xirrRes.setCheck_units(units);
                    xirrRes.setStamp_duty(stamp_duty);
                    positive_list.add(xirrRes);

                    freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                    freeUnitsResponse.setTRADDATE(transaction_date);
                    freeUnitsResponse.setTRXN_TYPE_(transaction_description);
                    freeUnitsResponse.setPURPRICE(purchase_price);
                    freeUnitsResponse.setUNITS(units);
                    freeUnitsResponse.setPurchase_units(units);
                    freeUnitsResponse.setAMOUNT(amount);
                    freeUnitsResponse.setTOTAL_TAX(tds_amount);
                    freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    freeUnitsResponseList.add(freeUnitsResponse);
                }

            } else if (karvyNeutralTransactionArrayList.contains(transaction_description)) {
                if (transaction_description.equalsIgnoreCase("Consolidation Out") || transaction_description.contains("Transmission")) {
                    investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                    investorSchemeWisePortfolioResponse.setTotalInflow(0);
                    investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                    investorSchemeWisePortfolioResponse.setTotalUnits(0);
                    investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                    investorSchemeWisePortfolioResponse.setDividendPaid(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                    investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                    investorSchemeWisePortfolioResponse.setInvestmentReStartDate(transaction_date);
                    xirr_list = new ArrayList<>();
                    xirrRes = null;
                    freeUnitsResponseList = new ArrayList<>();
                    divPaidResponseList = new ArrayList<>();
                    positive_list = new ArrayList<>();
                    negative_list = new ArrayList<>();
                    transfer_flag = true;
                } else if (karvy_neutral_dividend_transaction_type_list.contains(transaction_description)) {
                    investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount);
                    if(!investorSchemeWisePortfolioResponse.getIsDividendScheme()) {
                        investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                        investorSchemeWisePortfolioResponse.setIsDividendPayout(true);
                    }
                    if (transaction_date.compareTo(financial_year_strt_date) >= 0) {
                        investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_paid_current_fy() + amount);
                    }
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount);
                    final Double priceF = purchase_price;
                    InvestorSchemeWiseTransactionResponse free = divPaidResponseList.stream().filter(x -> x.getTRADDATE().compareTo(transaction_date) == 0 && x.getPURPRICE().compareTo(priceF) == 0 && x.getTRXN_TYPE_().equalsIgnoreCase(transaction_description)).findAny().orElse(null);
                    if (free != null) {
                        free.setUNITS(free.getUNITS() + units);
                        free.setPurchase_units(free.getPurchase_units() + units);
                        free.setTOTAL_TAX(free.getTOTAL_TAX() + tds_amount);
                        free.setSTAMP_DUTY(free.getSTAMP_DUTY() + stamp_duty);
                        free.setAMOUNT(free.getAMOUNT() + amount);
                        free.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    } else {
                        freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                        freeUnitsResponse.setTRADDATE(transaction_date);
                        freeUnitsResponse.setTRXN_TYPE_(transaction_description);
                        freeUnitsResponse.setPURPRICE(purchase_price);
                        freeUnitsResponse.setUNITS(units);
                        freeUnitsResponse.setPurchase_units(units);
                        freeUnitsResponse.setAMOUNT(amount);
                        freeUnitsResponse.setTOTAL_TAX(tds_amount);
                        freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                        freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                        divPaidResponseList.add(freeUnitsResponse);
                    }
                }
            } else {
                if (karvy_switch_out_list.contains(transaction_description)) {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalSwitchOutAmount() + amount;
                    investorSchemeWisePortfolioResponse.setTotalSwitchOutAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                } else {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalRedemptionAmount() + amount;
                    investorSchemeWisePortfolioResponse.setTotalRedemptionAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                }

                investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                investorSchemeWisePortfolioResponse.setLastTransactionDate(transaction_date);
                investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount);
                Double current_units = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                current_units = Double.parseDouble(unit_decimal.format(current_units));
                investorSchemeWisePortfolioResponse.setTotalUnits(current_units);

                final Double priceF = purchase_price;
                final Double currentUnitsF = current_units;
                final List<XirrResponse> negative_list_ref = negative_list;
                int index = IntStream.range(0, negative_list_ref.size()).filter(x -> negative_list_ref.get(x).getTrxn_date().compareTo(transaction_date) == 0 && negative_list_ref.get(x).getNav().compareTo(priceF) == 0 && negative_list_ref.get(x).getTrxn_type().equalsIgnoreCase(transaction_description)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = negative_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setStt(xirrRes.getStt() + karvySchemeWiseInvestorTransactions.get(i).getStt());
                    xirrRes.setTotal_units(currentUnitsF);
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(transaction_description);
                    xirrRes.setTrxn_date(transaction_date);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(purchase_price);
                    xirrRes.setStt(karvySchemeWiseInvestorTransactions.get(i).getStt());
                    xirrRes.setTotal_units(currentUnitsF);
                    negative_list.add(xirrRes);
                }
            }

            if (transfer_flag && investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
                investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                investorSchemeWisePortfolioResponse.setTotalInflow(0);
                investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(0);
                investorSchemeWisePortfolioResponse.setTotalInvestedAmount(0);
                investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                investorSchemeWisePortfolioResponse.setTotalUnits(0);
                investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                investorSchemeWisePortfolioResponse.setDividendPaid(0);
                investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(0);
                investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(0);
                investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                investorSchemeWisePortfolioResponse.setInvestmentStartDate(transaction_date);
                investorSchemeWisePortfolioResponse.setInvestmentReStartDate(transaction_date);
                xirr_list = new ArrayList<>();
                xirrRes = null;
                freeUnitsResponseList = new ArrayList<>();
                divPaidResponseList = new ArrayList<>();
                positive_list = new ArrayList<>();
                negative_list = new ArrayList<>();
            }

            if (transaction_description.equalsIgnoreCase("Systematic Investment") || remarks.equalsIgnoreCase("New Purchase With SIP") || remarks.equalsIgnoreCase("Existing Folio with SIP") || remarks.equalsIgnoreCase("SIP Insure")) {
                investorSchemeWisePortfolioResponse.setIsSipScheme(true);
                investorSchemeWisePortfolioResponse.setSip_amount(amount);
                sip_last_tran_date = transaction_date;
            }

            if (investorSchemeWisePortfolioResponse.getIsStpOutScheme() && ((transaction_description.equalsIgnoreCase("S T P Out") || transaction_description.equalsIgnoreCase("STRIP Out") || transaction_description.equalsIgnoreCase("Lateral Shift Out")) && !karvySchemeWiseInvestorTransactions.get(i).getPurchase_transaction_no().equalsIgnoreCase("0"))) {
                stp_last_tran_date = transaction_date;
            }
            if (investorSchemeWisePortfolioResponse.getIsStpInScheme() && ((transaction_description.equalsIgnoreCase("S T P In") || transaction_description.equalsIgnoreCase("STRIP In") || transaction_description.equalsIgnoreCase("Lateral Shift In")) && !karvySchemeWiseInvestorTransactions.get(i).getPurchase_transaction_no().equalsIgnoreCase("0"))) {
                stp_last_tran_date = transaction_date;
            }
            if (investorSchemeWisePortfolioResponse.getIsSwpScheme() && transaction_description.equalsIgnoreCase("Systematic Withdrawal")) {
                swp_last_tran_date = transaction_date;
            }

            if (karvyPositiveTransactionArrayList.contains(transaction_description)) {
                if (!transaction_description.equalsIgnoreCase("Div. Reinvestment") && !transaction_description.equalsIgnoreCase("ReInvestment Rejection") && !transaction_description.equalsIgnoreCase("Bonus Units")) {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(transaction_date);
                    xirrRes.setAmount(-(amount + stamp_duty));
                    xirr_list.add(xirrRes);
                }
            } else if (karvyNeutralTransactionArrayList.contains(transaction_description)) {
                if (transaction_description.equalsIgnoreCase("Consolidation Out") || transaction_description.contains("Transmission")) {
                    xirr_list = new ArrayList<>();
                    xirrRes = null;
                    transfer_flag = true;
                } else if (karvy_neutral_dividend_transaction_type_list.contains(transaction_description)) {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(transaction_date);
                    xirrRes.setAmount(amount);
                    xirr_list.add(xirrRes);
                }
            } else {
                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(transaction_date);
                xirrRes.setAmount(amount);
                xirr_list.add(xirrRes);
            }
            if (transfer_flag) {
                xirr_list = new ArrayList<>();
                xirrRes = null;
            }
        }

        if (investorSchemeWisePortfolioResponse.getScheme_code().equalsIgnoreCase("RMF01GP")) {
            investorSchemeWisePortfolioResponse.setTotalUnits(0.0);
        }

        investorSchemeWisePortfolioResponse.setMultiple_broker_code(String.join(",", multiple_broker_code_list));

        Double realisedAllRedeemedGainLoss = 0.0;
        Double realisedGainLoss = 0.0;
        Double div_reinvest_value = 0.0;
        if (negative_list.size() > 0) {
            Comparator<XirrResponse> compareByValues = Comparator.comparing(XirrResponse::getTrxn_date)
                    .thenComparing(Comparator.comparing(XirrResponse::getTotal_units).reversed());
            List<XirrResponse> sortedNegative_list = negative_list.stream().sorted(compareByValues).collect(Collectors.toList());

            for (XirrResponse xirr : sortedNegative_list) {
                Double purchase_nav;
                Double purchase_units;
                Double total_units = xirr.getTotal_units();
                Double sold_nav = xirr.getNav();
                Double sold_units = xirr.getUnits();
                Double total_tax = 0.0;
                Double reliased_gain = 0.0;
                Double orgi_units;
                Double orgi_amt;
                Double stamp_duty;
                String trxn_type;

                for (int k = 0; k < positive_list.size(); k++) {
                    xirrRes = positive_list.get(k);
                    purchase_units = xirrRes.getCheck_units();
                    trxn_type = xirrRes.getTrxn_type();

                    if (purchase_units > 0) {
                        Double remain_units = purchase_units - sold_units;
                        remain_units = Double.parseDouble(unit_decimal.format(remain_units));

                        if (remain_units >= 0) {
                            xirrRes.setCheck_units(remain_units);
                            purchase_nav = xirrRes.getNav();
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + (((sold_nav - purchase_nav) * sold_units) - total_tax);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            if (remain_units <= 0) {
                                free.setUNITS(0.0);
                                free.setTOTAL_UNITS(0.0);
                            } else {
                                free.setUNITS(remain_units);
                                free.setTOTAL_UNITS(remain_units);
                            }
                            if (trxn_type.equalsIgnoreCase("Div. Reinvestment") || trxn_type.equalsIgnoreCase("ReInvestment Rejection") || trxn_type.equalsIgnoreCase("Bonus Units")) {
                                div_reinvest_value += (purchase_nav * sold_units);
                            }
                            break;
                        } else {
                            purchase_nav = xirrRes.getNav();
                            Double current_units = xirrRes.getCheck_units();
                            sold_units = sold_units - current_units;
                            sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + (sold_nav - purchase_nav) * current_units;
                            xirrRes.setCheck_units(0.0);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            free.setUNITS(0.0);
                            free.setTOTAL_UNITS(0.0);
                            if (trxn_type.equalsIgnoreCase("Div. Reinvestment") || trxn_type.equalsIgnoreCase("ReInvestment Rejection") || trxn_type.equalsIgnoreCase("Bonus Units")) {
                                div_reinvest_value += (purchase_nav * current_units);
                            }
                        }
                    }
                }
                reliased_gain = Double.parseDouble(cost_dcf.format(reliased_gain));
                realisedAllRedeemedGainLoss = realisedAllRedeemedGainLoss + reliased_gain;
                realisedGainLoss = realisedGainLoss + reliased_gain;

                if (total_units == 0) {
                    realisedGainLoss = 0.0;
                }
            }
        }
        realisedGainLoss = Double.parseDouble(round_dcf.format(realisedGainLoss));
        realisedAllRedeemedGainLoss = Double.parseDouble(round_dcf.format(realisedAllRedeemedGainLoss));
        div_reinvest_value = Double.parseDouble(cost_dcf.format(div_reinvest_value));

        investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
        investorSchemeWisePortfolioResponse.setRealisedProfitLoss(realisedAllRedeemedGainLoss + investorSchemeWisePortfolioResponse.getDividendPaid());
        investorSchemeWisePortfolioResponse.setRealisedGainLoss(realisedGainLoss + investorSchemeWisePortfolioResponse.getDividendPaid());

        if (sip_last_tran_date != null && sip_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
            investorSchemeWisePortfolioResponse.setIsActiveSipScheme(true);
        }
        if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
            String folio = investorSchemeWisePortfolioResponse.getFoliono();
            String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
            InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
            if (sipObj != null) {
                investorSchemeWisePortfolioResponse.setIsActiveSipScheme(false);
            }
        }
        if (active_sip_cams != null && active_sip_cams.size() > 0) {
            String folio = investorSchemeWisePortfolioResponse.getFoliono();
            String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
            InvestorSipCams49 sipObj = active_sip_cams.stream().filter(x -> x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
            if (sipObj != null) {
                investorSchemeWisePortfolioResponse.setIsActiveSipScheme(true);
                Double total_sip_amount = active_sip_cams.stream().filter(x -> x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).mapToDouble(InvestorSipCams49::getAuto_amount).sum();
                investorSchemeWisePortfolioResponse.setSip_amount(total_sip_amount);
            }
        }
        if (stp_last_tran_date != null) {
            if (stp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
                investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
            }
            if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
                String folio = investorSchemeWisePortfolioResponse.getFoliono();
                String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveStpScheme(false);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                } else {
                    InvestorSipCams49 stpInObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio) && sch_code.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsActiveStpScheme(false);
                        investorSchemeWisePortfolioResponse.setSip_frequency(stpInObj.getPeriodicit());
                    }
                }
            }
            if (sip_cams != null && sip_cams.size() > 0) {
                String folio = investorSchemeWisePortfolioResponse.getFoliono();
                String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                } else {
                    InvestorSipCams49 stpInObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("STP") && x.getFolio_no().equalsIgnoreCase(folio) && sch_code.equalsIgnoreCase(x.getTarget_scheme_code())).findAny().orElse(null);
                    if (stpInObj != null) {
                        investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
                        investorSchemeWisePortfolioResponse.setSip_frequency(stpInObj.getPeriodicit());
                    }
                }
            }
        }
        if (swp_last_tran_date != null) {
            if (swp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
                investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(true);
            }
            if (closed_sip_cams != null && closed_sip_cams.size() > 0) {
                String folio = investorSchemeWisePortfolioResponse.getFoliono();
                String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = closed_sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(false);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                }
            }
            if (sip_cams != null && sip_cams.size() > 0) {
                String folio = investorSchemeWisePortfolioResponse.getFoliono();
                String sch_code = investorSchemeWisePortfolioResponse.getScheme_code();
                InvestorSipCams49 sipObj = sip_cams.stream().filter(x -> x.getAut_trntyp().equalsIgnoreCase("SWP") && x.getFolio_no().equalsIgnoreCase(folio) && x.getProduct().equalsIgnoreCase(sch_code)).findAny().orElse(null);
                if (sipObj != null) {
                    investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(true);
                    investorSchemeWisePortfolioResponse.setSip_frequency(sipObj.getPeriodicit());
                }
            }
        }

        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Date traddate = free.getTRADDATE();
            Double units = free.getUNITS();
            Calendar cal = Calendar.getInstance();
            cal.setTime(traddate);
            cal.add(Calendar.YEAR, 3);
            Date trxn_date_after3years = cal.getTime();
            if (trxn_date_after3years.compareTo(today) < 0) {
                Double added_units = investorSchemeWisePortfolioResponse.getLoadFreeUnits() + units;
                added_units = Double.parseDouble(unit_decimal.format(added_units));
                investorSchemeWisePortfolioResponse.setLoadFreeUnits(added_units);
            }
        }

        if (!folio_type.equalsIgnoreCase("All")) {
            Double tot_units = 0.0;
            int cnt = 0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                if (free.getUNITS() > 0) {
                    if (tot_units == 0) {
                        Double amount = (free.getUNITS() * free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentReStartDate(free.getTRADDATE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                        investorSchemeWisePortfolioResponse.setPurchaseNav(free.getPURPRICE());
                    }
                }
                tot_units = tot_units + free.getUNITS();
                tot_units = Double.parseDouble(unit_decimal.format(tot_units));
                free.setTOTAL_UNITS(tot_units);
                if (tot_units == 0) {
                    for (int k = 0; k <= cnt; k++) {
                        freeUnitsResponseList.get(k).setUNITS(0.0);
                        freeUnitsResponseList.get(k).setTOTAL_UNITS(0.0);
                    }
                }
                cnt++;
            }
            freeUnitsResponseList.addAll(divPaidResponseList);
            freeUnitsResponseList.sort(Comparator.comparing(InvestorSchemeWiseTransactionResponse::getTRADDATE));

            xirr_list = new ArrayList<>();
            xirrRes = null;
            Double live_current_cost = 0.0;
            Double div_reinvest_amt = 0.0;
            Double div_paid_amt = 0.0;
            Double scheme_total_units = 0.0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                Date traddate = free.getTRADDATE();
                String trxn_desc = free.getTRXN_TYPE_();
                Double nav = free.getPURPRICE();
                Double units = free.getUNITS();
                Double purchase_units = free.getPurchase_units();
                Double amount;
                if (!investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                    amount = (units * nav);
                } else {
                    amount = free.getAMOUNT();
                }
                Double tax = free.getTOTAL_TAX();
                Double stamp_duty = free.getSTAMP_DUTY();
                Double pur_amount = free.getAMOUNT();

                if (units > 0 && units < purchase_units) {
                    stamp_duty = units * (stamp_duty / purchase_units);
                }
                if (amount > pur_amount) {
                    amount = pur_amount;
                } else if (amount < pur_amount) {
                    if ((pur_amount - amount) <= 5) {
                        amount = pur_amount;
                    }
                }

                if (units != 0) {
                    if (karvyPositiveTransactionArrayList.contains(trxn_desc)) {
                        scheme_total_units = free.getTOTAL_UNITS();
                        if (trxn_desc.equalsIgnoreCase("Div. Reinvestment") || trxn_desc.equalsIgnoreCase("ReInvestment Rejection") || trxn_desc.equalsIgnoreCase("ReInvestment Rejection Reversal") || trxn_desc.equalsIgnoreCase("Dividend Sweep In") || trxn_desc.equalsIgnoreCase("Dividend Sweep In Rej.")) {
                            div_reinvest_amt = div_reinvest_amt + amount;
                        }
                        if (!trxn_desc.equalsIgnoreCase("Div. Reinvestment") && !trxn_desc.equalsIgnoreCase("ReInvestment Rejection") && !trxn_desc.equalsIgnoreCase("ReInvestment Rejection Reversal")) {
                            live_current_cost = live_current_cost + amount + stamp_duty;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(-(amount + stamp_duty));
                            xirr_list.add(xirrRes);
                        }
                    }
                }

                if (karvyNeutralTransactionArrayList.contains(trxn_desc)) {
                    if (live_current_cost > 0) {
                        if (trxn_desc.equalsIgnoreCase("Consolidation Out") || trxn_desc.contains("Transmission")) {
                            xirr_list = new ArrayList<>();
                            xirrRes = null;
                        } else if (karvy_neutral_dividend_transaction_type_list.contains(trxn_desc)) {
                            amount = free.getAMOUNT() + tax;
                            Double div_total_units = free.getTOTAL_UNITS();
                            if (!scheme_total_units.equals(div_total_units)) {
                                Double div_per_unit = 0.0;
                                if (div_total_units > 0) {
                                    div_per_unit = (amount / div_total_units);
                                }
                                amount = div_per_unit * scheme_total_units;
                            }
                            div_paid_amt = div_paid_amt + amount;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(amount);
                            xirr_list.add(xirrRes);
                        }
                    }
                }
            }

            Double current_cost = live_current_cost;
            if (current_cost <= 0) current_cost = 0.0;
            current_cost = Double.parseDouble(round_dcf.format(current_cost));
            div_reinvest_amt = Double.parseDouble(round_dcf.format(div_reinvest_amt));
            div_paid_amt = Double.parseDouble(round_dcf.format(div_paid_amt));
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            investorSchemeWisePortfolioResponse.setTotalInvestedAmount(current_cost);
            investorSchemeWisePortfolioResponse.setDividendReinvestment(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setDividendPaid(div_paid_amt);
        }

        if (last_tran_date == null) {
            last_tran_date = today;
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() > 0) {
            applyAmfiSchemeMapping(investorSchemeWisePortfolioResponse, "KARVY", client_name, financial_year,
                    financial_year_end_date_str, page, sdf1, cost_dcf, last_tran_nav, last_tran_date);

            if (investorSchemeWisePortfolioResponse.getScheme() != null &&
                    investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                xirr_list = new ArrayList<>();
            }

            if (investorSchemeWisePortfolioResponse.getTotalInflow() <= 0) {
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(0.0);
            } else if (folio_type.equalsIgnoreCase("All")) {
                Double current_cost = (investorSchemeWisePortfolioResponse.getTotalInflow() - investorSchemeWisePortfolioResponse.getTotalOutflow()) + realisedAllRedeemedGainLoss;
                if (current_cost <= 0) current_cost = 0.0;
                current_cost = Double.parseDouble(round_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            }

            Double purchase_nav = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment() / investorSchemeWisePortfolioResponse.getTotalUnits();
            purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
            if (purchase_nav > 0) {
                investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0) {
                Double current_value = investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav();
                current_value = Double.parseDouble(round_dcf.format(current_value));
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(current_value);
            } else {
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0 && investorSchemeWisePortfolioResponse.getPrev_latestNav() > 0) {
                if (investorSchemeWisePortfolioResponse.getLatestNavDate().compareTo(last_tran_date) == 0 && karvySchemeWiseInvestorTransactions.size() == 1) {
                    investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
                } else {
                    Double day_change_value = (investorSchemeWisePortfolioResponse.getLatestNav() - investorSchemeWisePortfolioResponse.getPrev_latestNav()) * investorSchemeWisePortfolioResponse.getTotalUnits();
                    day_change_value = Double.parseDouble(cost_dcf.format(day_change_value));
                    investorSchemeWisePortfolioResponse.setDay_change_value(day_change_value);
                }
            } else {
                investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
            }
        } else {
            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
            investorSchemeWisePortfolioResponse.setPrev_latestNav(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_percentage(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
        }

        if (investorSchemeWisePortfolioResponse.getScheme_class() == null) {
            investorSchemeWisePortfolioResponse.setScheme_class("");
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(0);
            investorSchemeWisePortfolioResponse.setTotalUnits(0);
            investorSchemeWisePortfolioResponse.setTotalCurrentValue(0);
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(0);

            applyAmfiSchemeMappingBasicsOnly(investorSchemeWisePortfolioResponse, "KARVY", client_name);
            if (investorSchemeWisePortfolioResponse.getScheme() != null &&
                    investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                xirr_list = new ArrayList<>();
            }

            if (folio_type.equalsIgnoreCase("All") && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() != 0) {
                addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                        equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
                final_cagr_list.addAll(xirr_list);
            }
            investorSchemeWisePortfolioResponse.setAverage_days(0);
        } else {
            xirrRes = new XirrResponse();
            xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
            xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
            xirr_list.add(xirrRes);

            Double unrealisedProfitLoss = investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            unrealisedProfitLoss = Double.parseDouble(round_dcf.format(unrealisedProfitLoss));
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(unrealisedProfitLoss);

            Double gain;
            Double invested_value;
            if (folio_type.equalsIgnoreCase("All")) {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss();
                invested_value = investorSchemeWisePortfolioResponse.getTotalInvestedAmount();
            } else {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getDividendPaid();
                invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            }
            if (invested_value > 0) {
                Double absolute_return = gain / invested_value;
                absolute_return = absolute_return * 100;
                absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
            } else {
                investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
            }

            addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                    equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
            final_cagr_list.addAll(xirr_list);

            Integer lt_st_total = 0, lt_st_count = 0, lt_st_avg = 0;
            for (XirrResponse res : positive_list) {
                Double purchase_units = res.getCheck_units();
                if (purchase_units > 0) {
                    int days = getDaysBetweenDates(res.getTrxn_date(), yesterday);
                    lt_st_total += days;
                    lt_st_count += 1;
                }
            }
            if (lt_st_count > 0 && lt_st_total > 0) {
                lt_st_avg = lt_st_total / lt_st_count;
            }
            investorSchemeWisePortfolioResponse.setAverage_days(lt_st_avg);
        }

        String scheme_class = investorSchemeWisePortfolioResponse.getScheme_class();
        if (scheme_class == null || StringHelper.isEmpty(scheme_class)) {
            scheme_class = "Equity Schemes";
        }
        Integer checking_days = (scheme_class.equalsIgnoreCase("Debt Schemes") ||
                "Equity: ELSS".equalsIgnoreCase(investorSchemeWisePortfolioResponse.getScheme_advisorkhoj_category()))
                ? 1095 : 365;

        Double long_term_units = 0.0;
        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Double purchase_units = free.getUNITS();
            if (purchase_units > 0) {
                int days = getDaysBetweenDates(free.getTRADDATE(), yesterday);
                if (days > checking_days) {
                    long_term_units = long_term_units + purchase_units;
                }
            }
        }
        long_term_units = Double.parseDouble(unit_decimal.format(long_term_units));
        investorSchemeWisePortfolioResponse.setLong_term_units(long_term_units);
        Double long_term_amount = long_term_units * investorSchemeWisePortfolioResponse.getLatestNav();
        long_term_amount = Double.parseDouble(cost_dcf.format(long_term_amount));
        investorSchemeWisePortfolioResponse.setLong_term_amount(long_term_amount);

        if ((investorSchemeWisePortfolioResponse.getTotalUnits() <= 0 && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() == 0) || xirr_list.size() == 0) {
            investorSchemeWisePortfolioResponse.setCagr(0);
        } else {
            investorSchemeWisePortfolioResponse.setCagr(computeSchemeXirr(xirr_list, cost_dcf));
        }
        investorSchemeWisePortfolioResponse.setCashflow_list(new ArrayList<>(xirr_list));

        if ((folio_type.equalsIgnoreCase("Live") || folio_type.equalsIgnoreCase("NonSegregated") || folio_type.equalsIgnoreCase("MF Without other ARN"))
                && investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            return null;
        }

        if (page.equalsIgnoreCase("holding-report")) {
            addToHoldingReportTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);
        }

        accumulatePortfolioTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);

        investorSchemeWisePortfolioResponse.setXirr_list(xirr_list);
        investorSchemeWisePortfolioResponse.setLastTransactionDate(last_tran_date);

        return investorSchemeWisePortfolioResponse;
    }

    // ==========================================================================================
    // MANUAL / PORTFOLIO TRANSACTIONS
    // ==========================================================================================

    private InvestorSchemeWisePortfolioResponse processManualScheme(
            List<PortfolioTransactions> portfolioSchemeWiseInvestorTransactions,
            List<String> mf_manualPositiveTransactionArrayList, List<String> mf_manualNeutralTransactionArrayList,
            List<String> manual_switch_in_list, List<String> manual_switch_out_list,
            String folio_type, String financial_year, String financial_year_end_date_str,
            Date financial_year_strt_date, Date fourty_days_befor_date, Date today, Date yesterday,
            String client_name, String page,
            DecimalFormat round_dcf, DecimalFormat cost_dcf, DecimalFormat unit_decimal, SimpleDateFormat sdf1, SimpleDateFormat sdf2, Format format,
            List<XirrResponse> equity_cagr_list, List<XirrResponse> debt_cagr_list, List<XirrResponse> hybrid_cagr_list,
            List<XirrResponse> solution_cagr_list, List<XirrResponse> other_cagr_list, List<XirrResponse> final_cagr_list,
            InvestorPortfolioResponse investorPortfolioResponse) throws Exception {

        InvestorSchemeWisePortfolioResponse investorSchemeWisePortfolioResponse = new InvestorSchemeWisePortfolioResponse();
        List<InvestorSchemeWiseTransactionResponse> investorSchemeWiseTransactionResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> freeUnitsResponseList = new ArrayList<>();
        List<InvestorSchemeWiseTransactionResponse> divPaidResponseList = new ArrayList<>();
        investorSchemeWisePortfolioResponse.setLoadFreeUnits(0.0);

        List<XirrResponse> xirr_list = new ArrayList<>();
        XirrResponse xirrRes = null;
        List<XirrResponse> positive_list = new ArrayList<>();
        List<XirrResponse> negative_list = new ArrayList<>();
        Date last_tran_date = null;
        Date sip_last_tran_date = null;
        Date stp_last_tran_date = null;
        Date swp_last_tran_date = null;
        Double last_tran_nav = 0.0;

        InvestorSchemeWiseTransactionResponse freeUnitsResponse;

        for (int i = 0; i < portfolioSchemeWiseInvestorTransactions.size(); i++) {
            String trxn_type_ = portfolioSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
            Date traddate = portfolioSchemeWiseInvestorTransactions.get(i).getTraddate();
            Double price = portfolioSchemeWiseInvestorTransactions.get(i).getPurprice();
            Double units = portfolioSchemeWiseInvestorTransactions.get(i).getUnits();
            Double amount = portfolioSchemeWiseInvestorTransactions.get(i).getAmount();
            Double stamp_duty = portfolioSchemeWiseInvestorTransactions.get(i).getStamp_duty();
            Double tax = portfolioSchemeWiseInvestorTransactions.get(i).getTds();
            investorSchemeWisePortfolioResponse.setLastTransactionDate(traddate);
            last_tran_date = traddate;
            last_tran_nav = price;
            if (stamp_duty == null) stamp_duty = 0.0;
            if (tax == null) tax = 0.0;

            InvestorSchemeWiseTransactionResponse investorSchemeWiseTransactionResponse = new InvestorSchemeWiseTransactionResponse();
            investorSchemeWiseTransactionResponse.setSCHEME(portfolioSchemeWiseInvestorTransactions.get(i).getScheme());
            investorSchemeWiseTransactionResponse.setTRADDATE(traddate);
            investorSchemeWiseTransactionResponse.setPURPRICE(price);
            investorSchemeWiseTransactionResponse.setUNITS(units);
            investorSchemeWiseTransactionResponse.setTOTAL_TAX(tax);
            investorSchemeWiseTransactionResponse.setTRXN_TYPE_(trxn_type_);
            investorSchemeWiseTransactionResponse.setTRXN_SUFFI(portfolioSchemeWiseInvestorTransactions.get(i).getTrxn_suffi());
            investorSchemeWiseTransactionResponse.setSTAMP_DUTY(stamp_duty);
            if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                investorSchemeWiseTransactionResponse.setAMOUNT(amount);
            } else {
                investorSchemeWiseTransactionResponse.setAMOUNT(-amount);
            }
            investorSchemeWiseTransactionResponseList.add(investorSchemeWiseTransactionResponse);

            if (i == 0) {
                String scheme_name = portfolioSchemeWiseInvestorTransactions.get(i).getScheme().trim();
                investorSchemeWisePortfolioResponse.setScheme(scheme_name);
                investorSchemeWisePortfolioResponse.setScheme_amfi_short_name(scheme_name);
                investorSchemeWisePortfolioResponse.setScheme_amfi_code(portfolioSchemeWiseInvestorTransactions.get(i).getScheme_code());
                investorSchemeWisePortfolioResponse.setScheme_code(portfolioSchemeWiseInvestorTransactions.get(i).getScheme_code());
                investorSchemeWisePortfolioResponse.setScheme_rta_code(portfolioSchemeWiseInvestorTransactions.get(i).getRta_scheme_code());
                investorSchemeWisePortfolioResponse.setFoliono(portfolioSchemeWiseInvestorTransactions.get(i).getFolio_no());
                boolean dividend_flag = portfolioSchemeWiseInvestorTransactions.get(i).isDividend_trxn();
                if (dividend_flag) {
                    investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                }
                investorSchemeWisePortfolioResponse.setInvestmentStartNav(price);
                investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                investorSchemeWisePortfolioResponse.setInvestmentReStartDate(traddate);
                investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                investorSchemeWisePortfolioResponse.setPurchaseNav(price);
                investorSchemeWisePortfolioResponse.setScheme_registrar("mfmanual");
                investorSchemeWisePortfolioResponse.setIsManualEntry(true);
                String upload_type = portfolioSchemeWiseInvestorTransactions.get(i).getUpload_type();
                if (upload_type == null) upload_type = "";
                if (upload_type.equalsIgnoreCase("CAS")) {
                    investorSchemeWisePortfolioResponse.setScheme_pan("CAS");
                } else {
                    investorSchemeWisePortfolioResponse.setScheme_pan("Manual");
                }
                investorSchemeWisePortfolioResponse.setBroker_code(portfolioSchemeWiseInvestorTransactions.get(i).getBroker_code());
                investorSchemeWisePortfolioResponse.setEuin("");
                investorSchemeWisePortfolioResponse.setTax_status("");
            }

            if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                if (investorSchemeWisePortfolioResponse.getTotalUnits() == 0 && !folio_type.equalsIgnoreCase("All")) {
                    investorSchemeWisePortfolioResponse.setIsNegativeTransaction(false);
                    investorSchemeWisePortfolioResponse.setTotalInflow(0);
                    investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(0);
                    investorSchemeWisePortfolioResponse.setTotalOutflow(0);
                    investorSchemeWisePortfolioResponse.setTotalUnits(0);
                    investorSchemeWisePortfolioResponse.setPurchaseNav(0);
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(0);
                    investorSchemeWisePortfolioResponse.setDividendPaid(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(0);
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid(0);
                    investorSchemeWisePortfolioResponse.setInvestmentStartDate(traddate);
                    investorSchemeWisePortfolioResponse.setInvestmentReStartDate(traddate);
                    xirr_list = new ArrayList<>();
                    xirrRes = null;
                }

                Double added_units = investorSchemeWisePortfolioResponse.getTotalUnits() + units;
                added_units = Double.parseDouble(unit_decimal.format(added_units));
                investorSchemeWisePortfolioResponse.setTotalUnits(added_units);

                if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    if (manual_switch_in_list.contains(trxn_type_)) {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalSwitchInAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalSwitchInAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    } else {
                        double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalOriginalInvestedAmount() + amount + stamp_duty;
                        investorSchemeWisePortfolioResponse.setTotalOriginalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                    }
                    double total_inflow_amount = investorSchemeWisePortfolioResponse.getTotalInflow() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInflow(Double.parseDouble(cost_dcf.format(total_inflow_amount)));
                    double total_invested_amount = investorSchemeWisePortfolioResponse.getTotalInvestedAmount() + amount + stamp_duty;
                    investorSchemeWisePortfolioResponse.setTotalInvestedAmount(Double.parseDouble(cost_dcf.format(total_invested_amount)));
                }

                if (trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    investorSchemeWisePortfolioResponse.setDividendReinvestment(investorSchemeWisePortfolioResponse.getDividendReinvestment() + amount + stamp_duty);
                    investorSchemeWisePortfolioResponse.setIsDividendReinvest(true);
                    investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                    investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                    if (traddate.compareTo(financial_year_strt_date) >= 0) {
                        investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest_current_fy() + amount + stamp_duty);
                    }
                    investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(investorSchemeWisePortfolioResponse.getTotal_dividend_reinvest() + amount + stamp_duty);
                }

                final Double priceF = price;
                final List<XirrResponse> positive_list_ref = positive_list;
                int index = IntStream.range(0, positive_list_ref.size()).filter(x -> positive_list_ref.get(x).getTrxn_date().compareTo(traddate) == 0 && positive_list_ref.get(x).getNav().compareTo(priceF) == 0 && positive_list_ref.get(x).getTrxn_type().equalsIgnoreCase(trxn_type_)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = positive_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setCheck_units(xirrRes.getCheck_units() + units);
                    xirrRes.setStamp_duty(xirrRes.getStamp_duty() + stamp_duty);

                    freeUnitsResponse = freeUnitsResponseList.get(index);
                    freeUnitsResponse.setUNITS(freeUnitsResponse.getUNITS() + units);
                    freeUnitsResponse.setPurchase_units(freeUnitsResponse.getPurchase_units() + units);
                    freeUnitsResponse.setTOTAL_TAX(freeUnitsResponse.getTOTAL_TAX() + tax);
                    freeUnitsResponse.setSTAMP_DUTY(freeUnitsResponse.getSTAMP_DUTY() + stamp_duty);
                    freeUnitsResponse.setAMOUNT(freeUnitsResponse.getAMOUNT() + amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(trxn_type_);
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(price);
                    xirrRes.setCheck_units(units);
                    xirrRes.setStamp_duty(stamp_duty);
                    positive_list.add(xirrRes);

                    freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                    freeUnitsResponse.setTRADDATE(traddate);
                    freeUnitsResponse.setTRXN_TYPE_(trxn_type_);
                    freeUnitsResponse.setPURPRICE(price);
                    freeUnitsResponse.setUNITS(units);
                    freeUnitsResponse.setPurchase_units(units);
                    freeUnitsResponse.setTOTAL_TAX(tax);
                    freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                    freeUnitsResponse.setAMOUNT(amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    freeUnitsResponseList.add(freeUnitsResponse);
                }

            } else if (mf_manualNeutralTransactionArrayList.contains(trxn_type_)) {
                investorSchemeWisePortfolioResponse.setDividendPaid(investorSchemeWisePortfolioResponse.getDividendPaid() + amount + tax);
                investorSchemeWisePortfolioResponse.setIsDividendDeclared(true);
                investorSchemeWisePortfolioResponse.setIsDividendPayout(true);
                investorSchemeWisePortfolioResponse.setIsDividendScheme(true);
                if (traddate.compareTo(financial_year_strt_date) >= 0) {
                    investorSchemeWisePortfolioResponse.setTotal_dividend_paid_current_fy(investorSchemeWisePortfolioResponse.getTotal_dividend_paid_current_fy() + amount + tax);
                }
                investorSchemeWisePortfolioResponse.setTotal_dividend_paid(investorSchemeWisePortfolioResponse.getTotal_dividend_paid() + amount + tax);

                final Double priceF = price;
                InvestorSchemeWiseTransactionResponse free = divPaidResponseList.stream().filter(x -> x.getTRADDATE().compareTo(traddate) == 0 && x.getPURPRICE().compareTo(priceF) == 0 && x.getTRXN_TYPE_().equalsIgnoreCase(trxn_type_)).findAny().orElse(null);
                if (free != null) {
                    free.setUNITS(free.getUNITS() + units);
                    free.setPurchase_units(free.getPurchase_units() + units);
                    free.setTOTAL_TAX(free.getTOTAL_TAX() + tax);
                    free.setSTAMP_DUTY(free.getSTAMP_DUTY() + stamp_duty);
                    free.setAMOUNT(free.getAMOUNT() + amount);
                    free.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                } else {
                    freeUnitsResponse = new InvestorSchemeWiseTransactionResponse();
                    freeUnitsResponse.setTRADDATE(traddate);
                    freeUnitsResponse.setTRXN_TYPE_(trxn_type_);
                    freeUnitsResponse.setPURPRICE(price);
                    freeUnitsResponse.setUNITS(units);
                    freeUnitsResponse.setPurchase_units(units);
                    freeUnitsResponse.setTOTAL_TAX(tax);
                    freeUnitsResponse.setSTAMP_DUTY(stamp_duty);
                    freeUnitsResponse.setAMOUNT(amount);
                    freeUnitsResponse.setTOTAL_UNITS(investorSchemeWisePortfolioResponse.getTotalUnits());
                    divPaidResponseList.add(freeUnitsResponse);
                }
            } else {
                if (manual_switch_out_list.contains(trxn_type_)) {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalSwitchOutAmount() + amount + tax;
                    investorSchemeWisePortfolioResponse.setTotalSwitchOutAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                } else {
                    double total_out_amount = investorSchemeWisePortfolioResponse.getTotalRedemptionAmount() + amount + tax;
                    investorSchemeWisePortfolioResponse.setTotalRedemptionAmount(Double.parseDouble(cost_dcf.format(total_out_amount)));
                }
                investorSchemeWisePortfolioResponse.setIsNegativeTransaction(true);
                investorSchemeWisePortfolioResponse.setLastTransactionDate(traddate);
                investorSchemeWisePortfolioResponse.setTotalOutflow(investorSchemeWisePortfolioResponse.getTotalOutflow() + amount + tax);
                Double added_units_neg = investorSchemeWisePortfolioResponse.getTotalUnits() - units;
                added_units_neg = Double.parseDouble(unit_decimal.format(added_units_neg));
                investorSchemeWisePortfolioResponse.setTotalUnits(added_units_neg);

                final Double priceF2 = price;
                final Double addedUnitsF = added_units_neg;
                final List<XirrResponse> negative_list_ref = negative_list;
                int index = IntStream.range(0, negative_list_ref.size()).filter(x -> negative_list_ref.get(x).getTrxn_date().compareTo(traddate) == 0 && negative_list_ref.get(x).getNav().compareTo(priceF2) == 0 && negative_list_ref.get(x).getTrxn_type().equalsIgnoreCase(trxn_type_)).findFirst().orElse(-1);
                if (index != -1) {
                    xirrRes = negative_list.get(index);
                    xirrRes.setUnits(xirrRes.getUnits() + units);
                    xirrRes.setTotal_units(addedUnitsF);
                } else {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_type(trxn_type_);
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setUnits(units);
                    xirrRes.setNav(price);
                    xirrRes.setStt(0.0);
                    xirrRes.setTotal_units(addedUnitsF);
                    negative_list.add(xirrRes);
                }
            }

            if (trxn_type_.equalsIgnoreCase("SIP")) {
                investorSchemeWisePortfolioResponse.setIsSipScheme(true);
                investorSchemeWisePortfolioResponse.setSip_amount(amount);
                sip_last_tran_date = traddate;
            }
            if (trxn_type_.equalsIgnoreCase("SWP")) {
                investorSchemeWisePortfolioResponse.setIsSwpScheme(true);
                investorSchemeWisePortfolioResponse.setSwp_amount(amount);
                swp_last_tran_date = traddate;
            }
            if (trxn_type_.equalsIgnoreCase("STP In")) {
                investorSchemeWisePortfolioResponse.setIsStpInScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
                stp_last_tran_date = traddate;
            }
            if (trxn_type_.equalsIgnoreCase("STP Out")) {
                investorSchemeWisePortfolioResponse.setIsStpOutScheme(true);
                investorSchemeWisePortfolioResponse.setStp_amount(amount);
                stp_last_tran_date = traddate;
            }

            if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                    xirrRes = new XirrResponse();
                    xirrRes.setTrxn_date(traddate);
                    xirrRes.setAmount(-(amount + stamp_duty));
                    xirr_list.add(xirrRes);
                }
            } else {
                xirrRes = new XirrResponse();
                xirrRes.setTrxn_date(traddate);
                xirrRes.setAmount(amount + tax);
                xirr_list.add(xirrRes);
            }
        }

        Double realisedAllRedeemedGainLoss = 0.0;
        Double realisedGainLoss = 0.0;
        Double div_reinvest_value = 0.0;
        if (negative_list.size() > 0) {
            Comparator<XirrResponse> compareByValues = Comparator.comparing(XirrResponse::getTrxn_date)
                    .thenComparing(Comparator.comparing(XirrResponse::getTotal_units).reversed());
            List<XirrResponse> sortedNegative_list = negative_list.stream().sorted(compareByValues).collect(Collectors.toList());

            for (XirrResponse xirr : sortedNegative_list) {
                Double purchase_nav;
                Double purchase_units;
                Double total_units = xirr.getTotal_units();
                Double sold_nav = xirr.getNav();
                Double sold_units = xirr.getUnits();
                Double total_tax = xirr.getStt();
                Double reliased_gain = 0.0;
                Double orgi_units;
                Double orgi_amt;
                Double stamp_duty;
                String trxn_type;

                for (int k = 0; k < positive_list.size(); k++) {
                    xirrRes = positive_list.get(k);
                    purchase_units = xirrRes.getCheck_units();
                    trxn_type = xirrRes.getTrxn_type();

                    if (purchase_units > 0) {
                        Double remain_units = purchase_units - sold_units;
                        remain_units = Double.parseDouble(unit_decimal.format(remain_units));

                        if (remain_units >= 0) {
                            xirrRes.setCheck_units(remain_units);
                            purchase_nav = xirrRes.getNav();
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + (((sold_nav - purchase_nav) * sold_units) - total_tax);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            if (remain_units <= 0) {
                                free.setUNITS(0.0);
                                free.setTOTAL_UNITS(0.0);
                            } else {
                                free.setUNITS(remain_units);
                                free.setTOTAL_UNITS(remain_units);
                            }
                            if (trxn_type.equalsIgnoreCase("Dividend Reinvest")) {
                                div_reinvest_value += (purchase_nav * sold_units);
                            }
                            break;
                        } else {
                            purchase_nav = xirrRes.getNav();
                            Double current_units = xirrRes.getCheck_units();
                            sold_units = sold_units - current_units;
                            sold_units = Double.parseDouble(unit_decimal.format(sold_units));
                            orgi_units = xirrRes.getUnits();
                            stamp_duty = xirrRes.getStamp_duty();
                            orgi_amt = (orgi_units * purchase_nav) + stamp_duty;
                            purchase_nav = (orgi_amt / orgi_units);
                            reliased_gain = reliased_gain + (sold_nav - purchase_nav) * current_units;
                            xirrRes.setCheck_units(0.0);
                            InvestorSchemeWiseTransactionResponse free = freeUnitsResponseList.get(k);
                            free.setUNITS(0.0);
                            free.setTOTAL_UNITS(0.0);
                            if (trxn_type.equalsIgnoreCase("Dividend Reinvest")) {
                                div_reinvest_value += (purchase_nav * current_units);
                            }
                        }
                    }
                }
                reliased_gain = Double.parseDouble(cost_dcf.format(reliased_gain));
                realisedAllRedeemedGainLoss = realisedAllRedeemedGainLoss + reliased_gain;
                realisedGainLoss = realisedGainLoss + reliased_gain;

                if (total_units == 0) {
                    realisedGainLoss = 0.0;
                }
            }
        }
        realisedGainLoss = Double.parseDouble(round_dcf.format(realisedGainLoss));
        realisedAllRedeemedGainLoss = Double.parseDouble(round_dcf.format(realisedAllRedeemedGainLoss));
        div_reinvest_value = Double.parseDouble(cost_dcf.format(div_reinvest_value));

        investorSchemeWisePortfolioResponse.setInvestorSchemeWiseTransactionResponses(investorSchemeWiseTransactionResponseList);
        investorSchemeWisePortfolioResponse.setRealisedProfitLoss(realisedAllRedeemedGainLoss + investorSchemeWisePortfolioResponse.getTotal_dividend_paid());
        investorSchemeWisePortfolioResponse.setRealisedGainLoss(realisedGainLoss + investorSchemeWisePortfolioResponse.getTotal_dividend_paid());

        if (sip_last_tran_date != null && sip_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
            investorSchemeWisePortfolioResponse.setIsActiveSipScheme(true);
        }
        if (stp_last_tran_date != null && stp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
            investorSchemeWisePortfolioResponse.setIsActiveStpScheme(true);
        }
        if (swp_last_tran_date != null && swp_last_tran_date.compareTo(fourty_days_befor_date) >= 0) {
            investorSchemeWisePortfolioResponse.setIsActiveSwpScheme(true);
        }

        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Date traddate = free.getTRADDATE();
            Double units = free.getUNITS();
            Calendar cal = Calendar.getInstance();
            cal.setTime(traddate);
            cal.add(Calendar.YEAR, 3);
            Date trxn_date_after3years = cal.getTime();
            if (trxn_date_after3years.compareTo(today) < 0) {
                Double added_units = investorSchemeWisePortfolioResponse.getLoadFreeUnits() + units;
                added_units = Double.parseDouble(unit_decimal.format(added_units));
                investorSchemeWisePortfolioResponse.setLoadFreeUnits(added_units);
            }
        }

        if (!folio_type.equalsIgnoreCase("All")) {
            Double tot_units = 0.0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                if (free.getUNITS() > 0) {
                    if (tot_units == 0) {
                        Double amount = (free.getUNITS() * free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentReStartDate(free.getTRADDATE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartNav(free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setPurchaseNav(free.getPURPRICE());
                        investorSchemeWisePortfolioResponse.setInvestmentStartValue(amount);
                    }
                    tot_units = tot_units + free.getUNITS();
                    free.setTOTAL_UNITS(tot_units);
                }
            }
            freeUnitsResponseList.addAll(divPaidResponseList);
            freeUnitsResponseList.sort(Comparator.comparing(InvestorSchemeWiseTransactionResponse::getTRADDATE));

            xirr_list = new ArrayList<>();
            xirrRes = null;
            Double live_current_cost = 0.0;
            Double div_reinvest_amt = 0.0;
            Double div_paid_amt = 0.0;
            Double scheme_total_units = 0.0;
            for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
                Date traddate = free.getTRADDATE();
                Double nav = free.getPURPRICE();
                Double units = free.getUNITS();
                Double purchase_units = free.getPurchase_units();
                Double amount;
                if (investorSchemeWisePortfolioResponse.getScheme().toLowerCase().contains("segregated")) {
                    amount = free.getAMOUNT();
                } else {
                    amount = (units * nav);
                }
                Double tax = free.getTOTAL_TAX();
                Double stamp_duty = free.getSTAMP_DUTY();
                String trxn_type_ = free.getTRXN_TYPE_();
                Double pur_amount = free.getAMOUNT();

                if (units > 0 && units < purchase_units) {
                    stamp_duty = units * (stamp_duty / purchase_units);
                }
                if (amount > pur_amount) {
                    amount = pur_amount;
                } else if (amount < pur_amount) {
                    if ((pur_amount - amount) <= 5) {
                        amount = pur_amount;
                    }
                }

                if (units != 0) {
                    if (mf_manualPositiveTransactionArrayList.contains(trxn_type_)) {
                        scheme_total_units = free.getTOTAL_UNITS();
                        if (trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                            div_reinvest_amt = div_reinvest_amt + amount;
                        }
                        if (!trxn_type_.equalsIgnoreCase("Dividend Reinvest")) {
                            live_current_cost = live_current_cost + amount + stamp_duty;
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setAmount(-(amount + stamp_duty));
                            xirr_list.add(xirrRes);
                        }
                    }
                }

                if (mf_manualNeutralTransactionArrayList.contains(trxn_type_)) {
                    if (live_current_cost > 0) {
                        amount = free.getAMOUNT() + tax;
                        Double div_total_units = free.getTOTAL_UNITS();
                        if (!scheme_total_units.equals(div_total_units)) {
                            Double div_per_unit = 0.0;
                            if (div_total_units > 0) {
                                div_per_unit = (amount / div_total_units);
                            }
                            amount = div_per_unit * scheme_total_units;
                        }
                        div_paid_amt = div_paid_amt + amount;
                        xirrRes = new XirrResponse();
                        xirrRes.setTrxn_date(traddate);
                        xirrRes.setAmount(amount);
                        xirr_list.add(xirrRes);
                    }
                }
            }

            Double current_cost = live_current_cost;
            if (current_cost <= 0) current_cost = 0.0;
            current_cost = Double.parseDouble(round_dcf.format(current_cost));
            div_reinvest_amt = Double.parseDouble(round_dcf.format(div_reinvest_amt));
            div_paid_amt = Double.parseDouble(round_dcf.format(div_paid_amt));
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            investorSchemeWisePortfolioResponse.setTotalInvestedAmount(current_cost);
            investorSchemeWisePortfolioResponse.setDividendReinvestment(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setTotal_dividend_reinvest(div_reinvest_amt);
            investorSchemeWisePortfolioResponse.setDividendPaid(div_paid_amt);
        }

        if (last_tran_date == null) {
            last_tran_date = today;
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() > 0) {
            if (investorSchemeWisePortfolioResponse.getScheme_code() != null && !investorSchemeWisePortfolioResponse.getScheme_code().isEmpty()) {
                applyAmfiSchemeMapping(investorSchemeWisePortfolioResponse, null, client_name, financial_year,
                        financial_year_end_date_str, page, sdf1, cost_dcf, last_tran_nav, last_tran_date);
            } else {
                investorSchemeWisePortfolioResponse.setScheme_company("");
                investorSchemeWisePortfolioResponse.setScheme_advisorkhoj_category("");
                investorSchemeWisePortfolioResponse.setScheme_class("");
                investorSchemeWisePortfolioResponse.setIsin_no("");
                investorSchemeWisePortfolioResponse.setScheme_amfi_common("");
                investorSchemeWisePortfolioResponse.setScheme_risk_profile("");
                investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
                investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
                investorSchemeWisePortfolioResponse.setPrev_latestNav(0.0);
                investorSchemeWisePortfolioResponse.setDay_change_percentage(0.0);
                investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
            }

            if (folio_type.equalsIgnoreCase("All")) {
                Double current_cost = (investorSchemeWisePortfolioResponse.getTotalInflow() - investorSchemeWisePortfolioResponse.getTotalOutflow()) + realisedAllRedeemedGainLoss;
                if (current_cost <= 0) current_cost = 0.0;
                current_cost = Double.parseDouble(round_dcf.format(current_cost));
                investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(current_cost);
            }

            Double purchase_nav = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment() / investorSchemeWisePortfolioResponse.getTotalUnits();
            purchase_nav = Double.parseDouble(unit_decimal.format(purchase_nav));
            if (purchase_nav > 0) {
                investorSchemeWisePortfolioResponse.setPurchaseNav(purchase_nav);
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0) {
                Double current_value = investorSchemeWisePortfolioResponse.getTotalUnits() * investorSchemeWisePortfolioResponse.getLatestNav();
                current_value = Double.parseDouble(round_dcf.format(current_value));
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(current_value);
            } else {
                investorSchemeWisePortfolioResponse.setTotalCurrentValue(investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment());
            }

            if (investorSchemeWisePortfolioResponse.getLatestNav() > 0 && investorSchemeWisePortfolioResponse.getPrev_latestNav() > 0) {
                if (investorSchemeWisePortfolioResponse.getLatestNavDate().compareTo(last_tran_date) == 0 && portfolioSchemeWiseInvestorTransactions.size() == 1) {
                    investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
                } else {
                    Double day_change_value = (investorSchemeWisePortfolioResponse.getLatestNav() - investorSchemeWisePortfolioResponse.getPrev_latestNav()) * investorSchemeWisePortfolioResponse.getTotalUnits();
                    day_change_value = Double.parseDouble(cost_dcf.format(day_change_value));
                    investorSchemeWisePortfolioResponse.setDay_change_value(day_change_value);
                }
            } else {
                investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
            }
        } else {
            investorSchemeWisePortfolioResponse.setLatestNav(last_tran_nav);
            investorSchemeWisePortfolioResponse.setLatestNavDate(last_tran_date);
            investorSchemeWisePortfolioResponse.setPrev_latestNav(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_percentage(0.0);
            investorSchemeWisePortfolioResponse.setDay_change_value(0.0);
        }

        if (investorSchemeWisePortfolioResponse.getScheme_class() == null) {
            investorSchemeWisePortfolioResponse.setScheme_class("");
        }

        if (investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            investorSchemeWisePortfolioResponse.setPurchaseNav(0);
            investorSchemeWisePortfolioResponse.setCurrentCostOfInvestment(0);
            investorSchemeWisePortfolioResponse.setTotalUnits(0);
            investorSchemeWisePortfolioResponse.setTotalCurrentValue(0);
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(0);

            if (investorSchemeWisePortfolioResponse.getScheme_code() != null && !investorSchemeWisePortfolioResponse.getScheme_code().isEmpty()) {
                applyAmfiSchemeMappingBasicsOnly(investorSchemeWisePortfolioResponse, null, client_name);
            } else {
                investorSchemeWisePortfolioResponse.setScheme_company("");
                investorSchemeWisePortfolioResponse.setScheme_advisorkhoj_category("");
                investorSchemeWisePortfolioResponse.setScheme_class("");
                investorSchemeWisePortfolioResponse.setIsin_no("");
                investorSchemeWisePortfolioResponse.setScheme_amfi_common("");
            }

            if (folio_type.equalsIgnoreCase("All") && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() != 0) {
                addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                        equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
                final_cagr_list.addAll(xirr_list);
            }
            investorSchemeWisePortfolioResponse.setAverage_days(0);
        } else {
            xirrRes = new XirrResponse();
            xirrRes.setTrxn_date(investorSchemeWisePortfolioResponse.getLatestNavDate());
            xirrRes.setAmount(investorSchemeWisePortfolioResponse.getTotalCurrentValue());
            xirr_list.add(xirrRes);

            Double unrealisedProfitLoss = investorSchemeWisePortfolioResponse.getTotalCurrentValue() - investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            unrealisedProfitLoss = Double.parseDouble(round_dcf.format(unrealisedProfitLoss));
            investorSchemeWisePortfolioResponse.setUnrealisedProfitLoss(unrealisedProfitLoss);

            Double gain;
            Double invested_value;
            if (folio_type.equalsIgnoreCase("All")) {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getRealisedProfitLoss();
                invested_value = investorSchemeWisePortfolioResponse.getTotalInvestedAmount();
            } else {
                gain = investorSchemeWisePortfolioResponse.getUnrealisedProfitLoss() + investorSchemeWisePortfolioResponse.getDividendPaid();
                invested_value = investorSchemeWisePortfolioResponse.getCurrentCostOfInvestment();
            }
            if (invested_value > 0) {
                Double absolute_return = gain / invested_value;
                absolute_return = absolute_return * 100;
                absolute_return = Double.parseDouble(cost_dcf.format(absolute_return));
                investorSchemeWisePortfolioResponse.setAbsolute_return(absolute_return);
            } else {
                investorSchemeWisePortfolioResponse.setAbsolute_return(0.0);
            }

            addToCagrBuckets(page, investorSchemeWisePortfolioResponse, xirr_list,
                    equity_cagr_list, debt_cagr_list, hybrid_cagr_list, solution_cagr_list, other_cagr_list);
            final_cagr_list.addAll(xirr_list);

            Integer lt_st_total = 0, lt_st_count = 0, lt_st_avg = 0;
            for (XirrResponse res : positive_list) {
                Double purchase_units = res.getCheck_units();
                if (purchase_units > 0) {
                    int days = getDaysBetweenDates(res.getTrxn_date(), yesterday);
                    lt_st_total += days;
                    lt_st_count += 1;
                }
            }
            if (lt_st_count > 0 && lt_st_total > 0) {
                lt_st_avg = lt_st_total / lt_st_count;
            }
            investorSchemeWisePortfolioResponse.setAverage_days(lt_st_avg);
        }

        String scheme_class = investorSchemeWisePortfolioResponse.getScheme_class();
        if (scheme_class == null || StringHelper.isEmpty(scheme_class)) {
            scheme_class = "Equity Schemes";
        }
        Integer checking_days = (scheme_class.equalsIgnoreCase("Debt Schemes") ||
                "Equity: ELSS".equalsIgnoreCase(investorSchemeWisePortfolioResponse.getScheme_advisorkhoj_category()))
                ? 1095 : 365;

        Double long_term_units = 0.0;
        for (InvestorSchemeWiseTransactionResponse free : freeUnitsResponseList) {
            Double purchase_units = free.getUNITS();
            if (purchase_units > 0) {
                int days = getDaysBetweenDates(free.getTRADDATE(), yesterday);
                if (days > checking_days) {
                    long_term_units = long_term_units + purchase_units;
                }
            }
        }
        long_term_units = Double.parseDouble(unit_decimal.format(long_term_units));
        investorSchemeWisePortfolioResponse.setLong_term_units(long_term_units);
        Double long_term_amount = long_term_units * investorSchemeWisePortfolioResponse.getLatestNav();
        long_term_amount = Double.parseDouble(cost_dcf.format(long_term_amount));
        investorSchemeWisePortfolioResponse.setLong_term_amount(long_term_amount);

        if ((investorSchemeWisePortfolioResponse.getTotalUnits() <= 0 && investorSchemeWisePortfolioResponse.getRealisedProfitLoss() == 0) || xirr_list.size() == 0) {
            investorSchemeWisePortfolioResponse.setCagr(0);
        } else {
            investorSchemeWisePortfolioResponse.setCagr(computeSchemeXirr(xirr_list, cost_dcf));
        }
        investorSchemeWisePortfolioResponse.setCashflow_list(new ArrayList<>(xirr_list));

        // NOTE: legacy manual-transactions block used "MF bought from others" here (not
        // "MF Without other ARN" like Cams/Karvy) -- preserved exactly.
        if ((folio_type.equalsIgnoreCase("Live") || folio_type.equalsIgnoreCase("NonSegregated") || folio_type.equalsIgnoreCase("MF bought from others"))
                && investorSchemeWisePortfolioResponse.getTotalUnits() <= 0) {
            return null;
        }

        if (page.equalsIgnoreCase("holding-report")) {
            addToHoldingReportTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);
        }

        accumulatePortfolioTotals(investorPortfolioResponse, investorSchemeWisePortfolioResponse);

        investorSchemeWisePortfolioResponse.setXirr_list(xirr_list);
        investorSchemeWisePortfolioResponse.setLastTransactionDate(last_tran_date);

        return investorSchemeWisePortfolioResponse;
    }

    // ==========================================================================================
    // Shared helpers
    // ==========================================================================================

    private void accumulatePortfolioTotals(InvestorPortfolioResponse investorPortfolioResponse, InvestorSchemeWisePortfolioResponse s) {
        investorPortfolioResponse.setTotalOriginalInvestedAmount(investorPortfolioResponse.getTotalOriginalInvestedAmount() + s.getTotalOriginalInvestedAmount());
        investorPortfolioResponse.setTotalSwitchInAmount(investorPortfolioResponse.getTotalSwitchInAmount() + s.getTotalSwitchInAmount());
        investorPortfolioResponse.setTotalSwitchOutAmount(investorPortfolioResponse.getTotalSwitchOutAmount() + s.getTotalSwitchOutAmount());
        investorPortfolioResponse.setTotalRedemptionAmount(investorPortfolioResponse.getTotalRedemptionAmount() + s.getTotalRedemptionAmount());
        investorPortfolioResponse.setTotalInvestedAmount(investorPortfolioResponse.getTotalInvestedAmount() + s.getTotalInvestedAmount());
        investorPortfolioResponse.setTotalInflow(investorPortfolioResponse.getTotalInflow() + s.getTotalInflow());
        investorPortfolioResponse.setTotalOutflow(investorPortfolioResponse.getTotalOutflow() + s.getTotalOutflow());
        investorPortfolioResponse.setTotalUnReliasedGain(investorPortfolioResponse.getTotalUnReliasedGain() + s.getUnrealisedProfitLoss());
        investorPortfolioResponse.setTotalReliasedGain(investorPortfolioResponse.getTotalReliasedGain() + s.getRealisedProfitLoss());
        investorPortfolioResponse.setTotalReliasedGainLoss(investorPortfolioResponse.getTotalReliasedGainLoss() + s.getRealisedGainLoss());
        investorPortfolioResponse.setTotalDividendPaid(investorPortfolioResponse.getTotalDividendPaid() + s.getTotal_dividend_paid());
        investorPortfolioResponse.setTotalDividendReinvestment(investorPortfolioResponse.getTotalDividendReinvestment() + s.getTotal_dividend_reinvest());
        investorPortfolioResponse.setTotalDividendPaidCurrentFY(investorPortfolioResponse.getTotalDividendPaidCurrentFY() + s.getTotal_dividend_paid_current_fy());
        investorPortfolioResponse.setTotalDividendReinvestCurrentFY(investorPortfolioResponse.getTotalDividendReinvestCurrentFY() + s.getTotal_dividend_reinvest_current_fy());
        investorPortfolioResponse.setTotalCurrentValue(investorPortfolioResponse.getTotalCurrentValue() + s.getTotalCurrentValue());
        investorPortfolioResponse.setTotalCurrentcost(investorPortfolioResponse.getTotalCurrentcost() + s.getCurrentCostOfInvestment());
        investorPortfolioResponse.setTotalCAGR(investorPortfolioResponse.getTotalCAGR() + s.getCagr());
    }

    private void addToCagrBuckets(String page, InvestorSchemeWisePortfolioResponse resp, List<XirrResponse> xirr_list,
                                  List<XirrResponse> equity, List<XirrResponse> debt, List<XirrResponse> hybrid,
                                  List<XirrResponse> solution, List<XirrResponse> other) {
        if (!page.equalsIgnoreCase("holding-report")) return;
        String cls = resp.getScheme_class();
        if (cls == null) return;
        if (cls.equalsIgnoreCase("Equity Schemes")) equity.addAll(xirr_list);
        if (cls.equalsIgnoreCase("Debt Schemes")) debt.addAll(xirr_list);
        if (cls.equalsIgnoreCase("Hybrid Schemes")) hybrid.addAll(xirr_list);
        if (cls.equalsIgnoreCase("Solution Oriented Schemes")) solution.addAll(xirr_list);
        if (cls.equalsIgnoreCase("Other Schemes")) other.addAll(xirr_list);
    }

    private void addToHoldingReportTotals(InvestorPortfolioResponse total, InvestorSchemeWisePortfolioResponse s) {
        String cls = s.getScheme_class();
        if (cls == null) return;
        if (cls.equalsIgnoreCase("Equity Schemes")) {
            total.setEquity_total_current_cost(total.getEquity_total_current_cost() + s.getCurrentCostOfInvestment());
            total.setEquity_total_current_value(total.getEquity_total_current_value() + s.getTotalCurrentValue());
            total.setEquity_total_unReliasedGain(total.getEquity_total_unReliasedGain() + s.getUnrealisedProfitLoss());
            total.setEquity_total_ReliasedGain(total.getEquity_total_ReliasedGain() + s.getRealisedGainLoss());
            total.setEquity_total_dividendPaid(total.getEquity_total_dividendPaid() + s.getTotal_dividend_paid());
            total.setEquity_total_dividendReinvestment(total.getEquity_total_dividendReinvestment() + s.getTotal_dividend_reinvest());
            total.setEquity_total_inflow(total.getEquity_total_inflow() + s.getTotalInflow());
            total.setEquity_total_invested_amount(total.getEquity_total_invested_amount() + s.getTotalInvestedAmount());
        }
        if (cls.equalsIgnoreCase("Debt Schemes")) {
            total.setDebt_total_current_cost(total.getDebt_total_current_cost() + s.getCurrentCostOfInvestment());
            total.setDebt_total_current_value(total.getDebt_total_current_value() + s.getTotalCurrentValue());
            total.setDebt_total_unReliasedGain(total.getDebt_total_unReliasedGain() + s.getUnrealisedProfitLoss());
            total.setDebt_total_ReliasedGain(total.getDebt_total_ReliasedGain() + s.getRealisedGainLoss());
            total.setDebt_total_dividendPaid(total.getDebt_total_dividendPaid() + s.getTotal_dividend_paid());
            total.setDebt_total_dividendReinvestment(total.getDebt_total_dividendReinvestment() + s.getTotal_dividend_reinvest());
            total.setDebt_total_inflow(total.getDebt_total_inflow() + s.getTotalInflow());
            total.setDebt_total_invested_amount(total.getDebt_total_invested_amount() + s.getTotalInvestedAmount());
        }
        if (cls.equalsIgnoreCase("Hybrid Schemes")) {
            total.setHybrid_total_current_cost(total.getHybrid_total_current_cost() + s.getCurrentCostOfInvestment());
            total.setHybrid_total_current_value(total.getHybrid_total_current_value() + s.getTotalCurrentValue());
            total.setHybrid_total_unReliasedGain(total.getHybrid_total_unReliasedGain() + s.getUnrealisedProfitLoss());
            total.setHybrid_total_ReliasedGain(total.getHybrid_total_ReliasedGain() + s.getRealisedGainLoss());
            total.setHybrid_total_dividendPaid(total.getHybrid_total_dividendPaid() + s.getTotal_dividend_paid());
            total.setHybrid_total_dividendReinvestment(total.getHybrid_total_dividendReinvestment() + s.getTotal_dividend_reinvest());
            total.setHybrid_total_inflow(total.getHybrid_total_inflow() + s.getTotalInflow());
            total.setHybrid_total_invested_amount(total.getHybrid_total_invested_amount() + s.getTotalInvestedAmount());
        }
        if (cls.equalsIgnoreCase("Solution Oriented Schemes")) {
            total.setSolution_total_current_cost(total.getSolution_total_current_cost() + s.getCurrentCostOfInvestment());
            total.setSolution_total_current_value(total.getSolution_total_current_value() + s.getTotalCurrentValue());
            total.setSolution_total_unReliasedGain(total.getSolution_total_unReliasedGain() + s.getUnrealisedProfitLoss());
            total.setSolution_total_ReliasedGain(total.getSolution_total_ReliasedGain() + s.getRealisedGainLoss());
            total.setSolution_total_dividendPaid(total.getSolution_total_dividendPaid() + s.getTotal_dividend_paid());
            total.setSolution_total_dividendReinvestment(total.getSolution_total_dividendReinvestment() + s.getTotal_dividend_reinvest());
            total.setSolution_total_inflow(total.getSolution_total_inflow() + s.getTotalInflow());
            total.setSolution_total_invested_amount(total.getSolution_total_invested_amount() + s.getTotalInvestedAmount());
        }
        if (cls.equalsIgnoreCase("Other Schemes")) {
            total.setOther_total_current_cost(total.getOther_total_current_cost() + s.getCurrentCostOfInvestment());
            total.setOther_total_current_value(total.getOther_total_current_value() + s.getTotalCurrentValue());
            total.setOther_total_unReliasedGain(total.getOther_total_unReliasedGain() + s.getUnrealisedProfitLoss());
            total.setOther_total_ReliasedGain(total.getOther_total_ReliasedGain() + s.getRealisedGainLoss());
            total.setOther_total_dividendPaid(total.getOther_total_dividendPaid() + s.getTotal_dividend_paid());
            total.setOther_total_dividendReinvestment(total.getOther_total_dividendReinvestment() + s.getTotal_dividend_reinvest());
            total.setOther_total_inflow(total.getOther_total_inflow() + s.getTotalInflow());
            total.setOther_total_invested_amount(total.getOther_total_invested_amount() + s.getTotalInvestedAmount());
        }
    }

    private Double computeSchemeXirr(List<XirrResponse> xirr_list, DecimalFormat cost_dcf) {
        int array_size = xirr_list.size();
        double[] values = new double[array_size];
        double[] dates = new double[array_size];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        int i = 0;
        for (XirrResponse xir : xirr_list) {
            values[i] = xir.getAmount();
            Calendar cal = Calendar.getInstance();
            cal.setTime(xir.getTrxn_date());
            dates[i] = (double) XIRR.getDateDiff(cal, calendar);
            i++;
        }
        double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
        if (xirrValue == 0) {
            xirrValue = XIRR.Bisection_method(values, dates, 0.1);
        }
        if (array_size == 1 && xirr_list.get(0).getAmount() == 0) xirrValue = 0;
        if (array_size == 2 && (xirr_list.get(0).getAmount() + xirr_list.get(1).getAmount() == 0)) xirrValue = 0;
        xirrValue = xirrValue * 100;
        return Double.parseDouble(cost_dcf.format(xirrValue));
    }

    private int getDaysBetweenDates(Date fromDate, Date toDate) {
        long diffMillis = toDate.getTime() - fromDate.getTime();
        return (int) (diffMillis / (24L * 60 * 60 * 1000));
    }
    private String rowStr(Object[] row, int idx) {
        return (row.length > idx && row[idx] != null) ? row[idx].toString().trim() : "";
    }

    private Double rowDouble(Object[] row, int idx) {
        return (row.length > idx && row[idx] != null) ? Double.parseDouble(row[idx].toString()) : 0.0;
    }

    /**
     * Resolve the AMC logo URL for a holding. The brand map keys on the first word of a name and
     * some AMFI scheme names do not lead with the AMC brand, so prefer the canonical AMC name
     * {@code scheme_company} and only fall back to the scheme names. Returns a full URL (path +
     * file), defaulting to the {@code empty.png} placeholder when no name resolves to a brand logo.
     */
    private String resolveAmcLogo(String... names) {
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.trim().isEmpty()) {
                    String file = MyMFBoxUtils.getLogoByAmcNameOrSchemeName(name);
                    if (!"empty.png".equalsIgnoreCase(file)) {
                        return amcLogoPath + file;
                    }
                }
            }
        }
        return amcLogoPath + "empty.png";
    }
    /** src = "CAMS" / "KARVY" / null (manual, keyed only by scheme_amfi_code). */
    private void applyAmfiSchemeMapping(InvestorSchemeWisePortfolioResponse resp, String src, String client_name,
                                        String financial_year, String financial_year_end_date_str, String page,
                                        SimpleDateFormat sdf1, DecimalFormat cost_dcf, Double last_tran_nav, Date last_tran_date) throws Exception {

        List<Object[]> mapping = (src != null)
                ? loadSchemeMasterPort.findSchemeMappingNew1(src, resp.getScheme_code())
                : loadSchemeMasterPort.findByAmfiCodeNew1(resp.getScheme_code());

        if (mapping != null && !mapping.isEmpty()) {
            Object[] row = mapping.get(0);
            String scheme_company = row[0].toString();
            String scheme_advisorkhoj_category = row[1].toString();
            String scheme_broad_category = row[2] != null ? row[2].toString() : "";
            String scheme_amfi_code = row[3].toString();
            String scheme_amfi = row[4].toString();
            String isin_no = row[5] != null ? row[5].toString().trim() : "";
            String scheme_amfi_common = row[6] != null ? row[6].toString().trim() : "";
            String scheme_amfi_short_name = row[7] != null ? row[7].toString().trim() : scheme_amfi;
            String riskometer = rowStr(row, 8);

            resp.setScheme_company(scheme_company);
            resp.setScheme_advisorkhoj_category(scheme_advisorkhoj_category);
            resp.setScheme_class(scheme_broad_category);
            resp.setScheme_amfi_code(scheme_amfi_code);
            if (src != null) {
                resp.setScheme(scheme_amfi);
            }
            resp.setScheme_amfi_short_name(scheme_amfi_short_name);
            resp.setIsin_no(isin_no);
            resp.setScheme_amfi_common(scheme_amfi_common);
            resp.setScheme_risk_profile(riskometer);
            resp.setAmc_logo(resolveAmcLogo(scheme_company, scheme_amfi_short_name, scheme_amfi));

            if (client_name.equalsIgnoreCase("justinvest")) {
                FundScoreInfo fundScore = loadFundScorePort.findLatestScoreNew(scheme_amfi_code);
                if (fundScore != null) {
                    resp.setConsistency_score_current_month(fundScore.getConsistencyScore());
                    resp.setPerformance_bucket_current_month(fundScore.getPerformanceBucket());
                } else {
                    resp.setConsistency_score_current_month(0.0);
                    resp.setPerformance_bucket_current_month("");
                }
            }

            Double nav_value = 0.0, prev_nav_value = 0.0;
            Date nav_date = null;
            Double day_change_percentage = 0.0;

            if (financial_year.equalsIgnoreCase("All")) {
                List<Object[]> latest = loadLatestNavPort.findLatestNavNews(scheme_amfi_code);
                if (latest != null && !latest.isEmpty()) {
                    Object[] r = latest.get(0);
                    resp.setLatestNav(rowDouble(r, 0));
                    resp.setLatestNavDate(sdf1.parse(r[1].toString()));
                    resp.setPrev_latestNav(rowDouble(r, 2));       // ← guarded, always 0.0 now
                    resp.setDay_change_percentage(rowDouble(r, 3)); // ← guarded, always 0.0 now
                    finishRatingLookup(resp, page, scheme_amfi_code, scheme_amfi);
                    return;   // ← and it returns here, so it NEVER reaches the amfi_mf_nav fallback below
                }
            }else {
                List<Object[]> latest = loadLatestNavPort.findLatestNavNews(scheme_amfi_code);
                if (latest != null && !latest.isEmpty()) {
                    Object[] r = latest.get(0);
                    resp.setCurrentNav(Double.parseDouble(r[0].toString()));
                    resp.setCurrentNavDate(sdf1.parse(r[1].toString()));
                }
            }

            List<Object[]> navRows = loadNavHistoryPort.findLastTwoNavsOnOrBeforeNew(scheme_amfi_code, financial_year_end_date_str);
            if (navRows != null && !navRows.isEmpty()) {
                int cnt = 0;
                for (Object[] r : navRows) {
                    if (cnt == 0) {
                        nav_date = sdf1.parse(String.valueOf(r[0]));
                        nav_value = Double.parseDouble(String.valueOf(r[1]));
                    }
                    if (cnt == 1) {
                        prev_nav_value = Double.parseDouble(String.valueOf(r[1]));
                    }
                    cnt++;
                }
            }
            if (prev_nav_value > 0) {
                day_change_percentage = ((nav_value - prev_nav_value) / prev_nav_value) * 100;
                day_change_percentage = Double.parseDouble(cost_dcf.format(day_change_percentage));
            }
            if (nav_date != null) {
                resp.setLatestNav(nav_value);
                resp.setLatestNavDate(nav_date);
                resp.setPrev_latestNav(prev_nav_value);
                resp.setDay_change_percentage(day_change_percentage);
            } else {
                resp.setLatestNav(last_tran_nav);
                resp.setLatestNavDate(last_tran_date);
                resp.setPrev_latestNav(prev_nav_value);
                resp.setDay_change_percentage(day_change_percentage);
            }

            finishRatingLookup(resp, page, scheme_amfi_code, scheme_amfi);
        } else {
            applyAmfiSchemeMappingBasicsOnly(resp, src, client_name);
            resp.setLatestNav(last_tran_nav);
            resp.setLatestNavDate(last_tran_date);
            resp.setPrev_latestNav(0.0);
            resp.setDay_change_percentage(0.0);
            resp.setDay_change_value(0.0);
        }
    }

    private void finishRatingLookup(InvestorSchemeWisePortfolioResponse resp, String page, String scheme_amfi_code, String scheme_amfi) {
        if (!page.equalsIgnoreCase("Portfolio-Score")) return;
        List<Integer> ratingList = loadHealthCheckupPort.findRatingNew(scheme_amfi_code);
        String scheme_review = "Not Rated";
        Integer rating = 0;
        if (ratingList != null && !ratingList.isEmpty()) {
            rating = ratingList.get(0) == null ? 0 : ratingList.get(0);
        }
        if (scheme_amfi.toLowerCase().contains("segregated")) {
            resp.setScheme_rating(0.0);
            resp.setScheme_review("Not Rated");
        } else {
            resp.setScheme_rating(Double.parseDouble(String.valueOf(rating)));
            if (rating == 5) scheme_review = "High Performer";
            else if (rating == 4) scheme_review = "Above Average Performer";
            else if (rating == 3) scheme_review = "Average Performer";
            else if (rating == 2) scheme_review = "Below Average Performer";
            else if (rating == 1) scheme_review = "Low Performer";
            resp.setScheme_review(scheme_review);
        }
    }

    private void applyAmfiSchemeMappingBasicsOnly(InvestorSchemeWisePortfolioResponse resp, String src, String client_name) {
        List<Object[]> mapping = (src != null)
                ? loadSchemeMasterPort.findSchemeMappingNew1(src, resp.getScheme_code())
                : loadSchemeMasterPort.findByAmfiCodeNew1(resp.getScheme_code());
        if (mapping != null && !mapping.isEmpty()) {
            Object[] row = mapping.get(0);
            resp.setScheme_company(row[0].toString());
            resp.setScheme_advisorkhoj_category(row[1].toString());
            resp.setScheme_class(row[2] != null ? row[2].toString() : "");
            resp.setScheme_amfi_code(row[3].toString());
            if (src != null) {
                resp.setScheme(row[4].toString());
            }
            resp.setScheme_amfi_short_name(row[7] != null ? row[7].toString().trim() : row[4].toString());
            resp.setIsin_no(row[5] != null ? row[5].toString().trim() : "");
            resp.setScheme_amfi_common(row[6] != null ? row[6].toString().trim() : "");
            resp.setScheme_risk_profile(rowStr(row, 8));
            resp.setAmc_logo(resolveAmcLogo(resp.getScheme_company(), resp.getScheme_amfi_short_name(), resp.getScheme()));
            if (client_name.equalsIgnoreCase("justinvest")) {
                resp.setConsistency_score_current_month(0.0);
                resp.setPerformance_bucket_current_month("");
            }
        } else {
            resp.setScheme_company("");
            resp.setScheme_advisorkhoj_category("");
            resp.setScheme_class("");
            resp.setScheme_amfi_code("");
            resp.setIsin_no("");
            resp.setScheme_amfi_common("");
            resp.setScheme_risk_profile("");
            resp.setAmc_logo(resolveAmcLogo(resp.getScheme_company(), resp.getScheme_amfi_short_name(), resp.getScheme()));
            if (client_name.equalsIgnoreCase("justinvest")) {
                resp.setConsistency_score_current_month(0.0);
                resp.setPerformance_bucket_current_month("");
            }
        }
    }

    // ==========================================================================================
    // Existing (already implemented) totals / formatting / XIRR helpers
    // ==========================================================================================

    private void computeHoldingReportCategoryTotals(InvestorPortfolioResponse investorPortfolioResponse, DecimalFormat cost_dcf) {
        Double equity_gain = investorPortfolioResponse.getEquity_total_unReliasedGain() + investorPortfolioResponse.getEquity_total_dividendPaid();
        Double equity_cost = investorPortfolioResponse.getEquity_total_current_cost();
        investorPortfolioResponse.setEquity_total_AbsoluteReturn(equity_cost > 0 ? Double.parseDouble(cost_dcf.format((equity_gain / equity_cost) * 100)) : 0.0);

        Double debt_gain = investorPortfolioResponse.getDebt_total_unReliasedGain() + investorPortfolioResponse.getDebt_total_dividendPaid();
        Double debt_cost = investorPortfolioResponse.getDebt_total_current_cost();
        investorPortfolioResponse.setDebt_total_AbsoluteReturn(debt_cost > 0 ? Double.parseDouble(cost_dcf.format((debt_gain / debt_cost) * 100)) : 0.0);

        Double hybrid_gain = investorPortfolioResponse.getHybrid_total_unReliasedGain() + investorPortfolioResponse.getHybrid_total_dividendPaid();
        Double hybrid_cost = investorPortfolioResponse.getHybrid_total_current_cost();
        investorPortfolioResponse.setHybrid_total_AbsoluteReturn(hybrid_cost > 0 ? Double.parseDouble(cost_dcf.format((hybrid_gain / hybrid_cost) * 100)) : 0.0);

        Double solution_gain = investorPortfolioResponse.getSolution_total_unReliasedGain() + investorPortfolioResponse.getSolution_total_dividendPaid();
        Double solution_cost = investorPortfolioResponse.getSolution_total_current_cost();
        investorPortfolioResponse.setSolution_total_AbsoluteReturn(solution_cost > 0 ? Double.parseDouble(cost_dcf.format((solution_gain / solution_cost) * 100)) : 0.0);

        Double other_gain = investorPortfolioResponse.getOther_total_unReliasedGain() + investorPortfolioResponse.getOther_total_dividendPaid();
        Double other_cost = investorPortfolioResponse.getOther_total_current_cost();
        investorPortfolioResponse.setOther_total_AbsoluteReturn(other_cost > 0 ? Double.parseDouble(cost_dcf.format((other_gain / other_cost) * 100)) : 0.0);
    }

    private Double computeXirr(List<XirrResponse> cagrList, DecimalFormat cost_dcf) {
        if (cagrList.size() > 0) {
            cagrList.sort(Comparator.comparing(XirrResponse::getTrxn_date));

            int array_size = cagrList.size();
            double[] values = new double[array_size];
            double[] dates = new double[array_size];

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(0);

            int i = 0;
            for (XirrResponse xir : cagrList) {
                values[i] = xir.getAmount();
                Calendar cal = Calendar.getInstance();
                cal.setTime(xir.getTrxn_date());
                dates[i] = (double) XIRR.getDateDiff(cal, calendar);
                i++;
            }

            double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
            if (xirrValue == 0) {
                xirrValue = XIRR.Bisection_method(values, dates, 0.1);
            }
            xirrValue = xirrValue * 100;
            return Double.parseDouble(cost_dcf.format(xirrValue));
        } else {
            return 0.0;
        }
    }

    private double computeXirrRaw(List<XirrResponse> cagrList) {
        if (cagrList.isEmpty()) {
            return 0.0;
        }

        int array_size = cagrList.size();
        double[] values = new double[array_size];
        double[] dates = new double[array_size];

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);

        int i = 0;
        for (XirrResponse xir : cagrList) {
            values[i] = xir.getAmount();
            Calendar cal = Calendar.getInstance();
            cal.setTime(xir.getTrxn_date());
            dates[i] = (double) XIRR.getDateDiff(cal, calendar);
            i++;
        }

        double xirrValue = XIRR.Newtons_method(values, dates, 0.1);
        if (xirrValue == 0) {
            xirrValue = XIRR.Bisection_method(values, dates, 0.1);
        }
        if (xirrValue == 0.1) {
            xirrValue = 0.0;
        }
        return xirrValue;
    }

    private void formatSchemeStrings(InvestorSchemeWisePortfolioResponse scheme, Format format, DecimalFormat cost_dcf, SimpleDateFormat sdf2) {
        Double curr_cost = scheme.getCurrentCostOfInvestment();
        Double curr_value = scheme.getTotalCurrentValue();
        Double div_reinv = scheme.getTotal_dividend_reinvest();
        Double div_paid = scheme.getTotal_dividend_paid();
        Double unreliased = scheme.getUnrealisedProfitLoss();
        Double reliasedGain = scheme.getRealisedGainLoss();
        Double reliasedProfit = scheme.getRealisedProfitLoss();
        Double ori_amt = scheme.getTotalOriginalInvestedAmount();
        Double switch_in_amt = scheme.getTotalSwitchInAmount();
        Double switch_out_amt = scheme.getTotalSwitchOutAmount();
        Double reem_amt = scheme.getTotalRedemptionAmount();
        Double long_term_units = scheme.getLong_term_units();
        Double long_term_amount = scheme.getLong_term_amount();

        scheme.setInvestmentStartDate_str(sdf2.format(scheme.getInvestmentStartDate()));
        scheme.setInvestmentReStartDate_str(sdf2.format(scheme.getInvestmentReStartDate()));
        scheme.setLatestNavDate_str(sdf2.format(scheme.getLatestNavDate()));
        scheme.setTotalUnits_str(format.format(scheme.getTotalUnits()));
        scheme.setPurchaseNav_str(format.format(scheme.getPurchaseNav()));
        scheme.setCurrentCostOfInvestment_str(format.format(curr_cost.longValue()));
        scheme.setTotal_dividend_reinvest_str(format.format(div_reinv.longValue()));
        scheme.setTotal_dividend_paid_str(format.format(div_paid.longValue()));
        scheme.setTotal_dividend_str(format.format(div_paid.longValue() + div_reinv.longValue()));

        scheme.setLatestNav_str(format.format(scheme.getLatestNav()));
        scheme.setTotalCurrentValue_str(format.format(curr_value.longValue()));
        scheme.setUnrealisedProfitLoss_str(format.format(unreliased.longValue()));
        scheme.setRealisedGainLoss_str(format.format(reliasedGain.longValue()));
        scheme.setRealisedProfitLoss_str(format.format(reliasedProfit.longValue()));
        scheme.setTotalOriginalInvestedAmount_str(format.format(ori_amt.longValue()));
        scheme.setTotalSwitchInAmount_str(format.format(switch_in_amt.longValue()));
        scheme.setTotalSwitchOutAmount_str(format.format(switch_out_amt.longValue()));
        scheme.setTotalRedemptionAmount_str(format.format(reem_amt.longValue()));
        scheme.setLong_term_units_str(format.format(long_term_units.longValue()));
        scheme.setLong_term_amount_str(format.format(long_term_amount.longValue()));
    }

    private void formatHoldingReportTotals(InvestorPortfolioResponse investorPortfolioResponse, Format format) {
        investorPortfolioResponse.setEquity_total_current_cost_str(format.format(investorPortfolioResponse.getEquity_total_current_cost().longValue()));
        investorPortfolioResponse.setEquity_total_current_value_str(format.format(investorPortfolioResponse.getEquity_total_current_value().longValue()));
        investorPortfolioResponse.setEquity_total_unReliasedGain_str(format.format(investorPortfolioResponse.getEquity_total_unReliasedGain().longValue()));
        investorPortfolioResponse.setEquity_total_ReliasedGain_str(format.format(investorPortfolioResponse.getEquity_total_ReliasedGain().longValue()));
        investorPortfolioResponse.setEquity_total_dividendPaid_str(format.format(investorPortfolioResponse.getEquity_total_dividendPaid().longValue()));
        investorPortfolioResponse.setEquity_total_dividendReinvestment_str(format.format(investorPortfolioResponse.getEquity_total_dividendReinvestment().longValue()));

        investorPortfolioResponse.setDebt_total_current_cost_str(format.format(investorPortfolioResponse.getDebt_total_current_cost().longValue()));
        investorPortfolioResponse.setDebt_total_current_value_str(format.format(investorPortfolioResponse.getDebt_total_current_value().longValue()));
        investorPortfolioResponse.setDebt_total_unReliasedGain_str(format.format(investorPortfolioResponse.getDebt_total_unReliasedGain().longValue()));
        investorPortfolioResponse.setDebt_total_ReliasedGain_str(format.format(investorPortfolioResponse.getDebt_total_ReliasedGain().longValue()));
        investorPortfolioResponse.setDebt_total_dividendPaid_str(format.format(investorPortfolioResponse.getDebt_total_dividendPaid().longValue()));
        investorPortfolioResponse.setDebt_total_dividendReinvestment_str(format.format(investorPortfolioResponse.getDebt_total_dividendReinvestment().longValue()));

        investorPortfolioResponse.setHybrid_total_current_cost_str(format.format(investorPortfolioResponse.getHybrid_total_current_cost().longValue()));
        investorPortfolioResponse.setHybrid_total_current_value_str(format.format(investorPortfolioResponse.getHybrid_total_current_value().longValue()));
        investorPortfolioResponse.setHybrid_total_unReliasedGain_str(format.format(investorPortfolioResponse.getHybrid_total_unReliasedGain().longValue()));
        investorPortfolioResponse.setHybrid_total_ReliasedGain_str(format.format(investorPortfolioResponse.getHybrid_total_ReliasedGain().longValue()));
        investorPortfolioResponse.setHybrid_total_dividendPaid_str(format.format(investorPortfolioResponse.getHybrid_total_dividendPaid().longValue()));
        investorPortfolioResponse.setHybrid_total_dividendReinvestment_str(format.format(investorPortfolioResponse.getHybrid_total_dividendReinvestment().longValue()));

        investorPortfolioResponse.setSolution_total_current_cost_str(format.format(investorPortfolioResponse.getSolution_total_current_cost().longValue()));
        investorPortfolioResponse.setSolution_total_current_value_str(format.format(investorPortfolioResponse.getSolution_total_current_value().longValue()));
        investorPortfolioResponse.setSolution_total_unReliasedGain_str(format.format(investorPortfolioResponse.getSolution_total_unReliasedGain().longValue()));
        investorPortfolioResponse.setSolution_total_ReliasedGain_str(format.format(investorPortfolioResponse.getSolution_total_ReliasedGain().longValue()));
        investorPortfolioResponse.setSolution_total_dividendPaid_str(format.format(investorPortfolioResponse.getSolution_total_dividendPaid().longValue()));
        investorPortfolioResponse.setSolution_total_dividendReinvestment_str(format.format(investorPortfolioResponse.getSolution_total_dividendReinvestment().longValue()));

        investorPortfolioResponse.setOther_total_current_cost_str(format.format(investorPortfolioResponse.getOther_total_current_cost().longValue()));
        investorPortfolioResponse.setOther_total_current_value_str(format.format(investorPortfolioResponse.getOther_total_current_value().longValue()));
        investorPortfolioResponse.setOther_total_unReliasedGain_str(format.format(investorPortfolioResponse.getOther_total_unReliasedGain().longValue()));
        investorPortfolioResponse.setOther_total_ReliasedGain_str(format.format(investorPortfolioResponse.getOther_total_ReliasedGain().longValue()));
        investorPortfolioResponse.setOther_total_dividendPaid_str(format.format(investorPortfolioResponse.getOther_total_dividendPaid().longValue()));
        investorPortfolioResponse.setOther_total_dividendReinvestment_str(format.format(investorPortfolioResponse.getOther_total_dividendReinvestment().longValue()));
    }
}
