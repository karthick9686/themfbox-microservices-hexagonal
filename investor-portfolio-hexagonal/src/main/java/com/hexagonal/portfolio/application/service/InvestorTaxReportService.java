package com.hexagonal.portfolio.application.service;


import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.internal.util.StringHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hexagonal.portfolio.application.port.in.GetInvestorTaxReportUseCase;
import com.hexagonal.portfolio.application.port.out.LoadCamsTaxTransactionPort;
import com.hexagonal.portfolio.application.port.out.LoadInflationIndexPort;
import com.hexagonal.portfolio.application.port.out.LoadKarvyTaxTransactionPort;
import com.hexagonal.portfolio.application.port.out.LoadNavHistoryPort;
import com.hexagonal.portfolio.application.port.out.LoadSchemeMasterPort;
import com.hexagonal.portfolio.application.port.out.LoadTransactionTypePort;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;
import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;
import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;
import com.hexagonal.portfolio.domain.model.TransactionType;
import com.hexagonal.portfolio.domain.model.XirrResponse;
import com.hexagonal.portfolio.domain.service.TrackerUtils;
import com.hexagonal.portfolio.domain.service.TransactionDataUtils;

@Service
class InvestorTaxReportService implements GetInvestorTaxReportUseCase {

    private final LoadTransactionTypePort loadTransactionTypePort;
    private final LoadCamsTaxTransactionPort loadCamsTaxTransactionPort;
    private final LoadKarvyTaxTransactionPort loadKarvyTaxTransactionPort;
    private final LoadSchemeMasterPort loadSchemeMasterPort;
    private final LoadNavHistoryPort loadNavHistoryPort;
    private final LoadInflationIndexPort loadInflationIndexPort;

    public InvestorTaxReportService(LoadTransactionTypePort loadTransactionTypePort,
                                    LoadCamsTaxTransactionPort loadCamsTaxTransactionPort,
                                    LoadKarvyTaxTransactionPort loadKarvyTaxTransactionPort,
                                    LoadSchemeMasterPort loadSchemeMasterPort,
                                    LoadNavHistoryPort loadNavHistoryPort,
                                    LoadInflationIndexPort loadInflationIndexPort) {
        this.loadTransactionTypePort = loadTransactionTypePort;
        this.loadCamsTaxTransactionPort = loadCamsTaxTransactionPort;
        this.loadKarvyTaxTransactionPort = loadKarvyTaxTransactionPort;
        this.loadSchemeMasterPort = loadSchemeMasterPort;
        this.loadNavHistoryPort = loadNavHistoryPort;
        this.loadInflationIndexPort = loadInflationIndexPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestorSchemeWiseTransactionTaxReport> getInvestorTaxReportNew(
            Integer user_id, String client_name, String option, String financial_year,
            String start_date, String end_date) {

        DecimalFormat decimal = new DecimalFormat("#.##");
        DecimalFormat four_decimal = new DecimalFormat("#.####");
        DecimalFormat unit_decimal = new DecimalFormat("0.0000");
        DecimalFormat two_decimal = new DecimalFormat("0.00");

        InvestorSchemeWiseTransactionTaxReport investorSchemeWiseTransactionTaxReport;
        List<InvestorSchemeWiseTransactionTaxReport> investorSchemeWiseTransactionTaxReportList = new ArrayList<>();

        List<InvestorTransactionCams> camsSchemeWiseInvestorTransactions;
        List<InvestorTransactionKarvy> karvySchemeWiseInvestorTransactions;

        String financial_year_end_date_str = "";
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");

        List<String> camsPositiveTransactionArrayList = new ArrayList<>();
        List<String> camsNegativeTransactionArrayList = new ArrayList<>();
        List<String> camsNegativeTransactionArrayList2 = new ArrayList<>();
        List<String> camsNeutralTransactionArrayList = new ArrayList<>();

        List<String> karvyPositiveTransactionArrayList = new ArrayList<>();
        List<String> karvyNegativeTransactionArrayList = new ArrayList<>();
        List<String> karvyNegativeTransactionArrayList2 = new ArrayList<>();
        List<String> karvyNeutralTransactionArrayList = new ArrayList<>();

        List<String> removeTransactionArrayList = new ArrayList<>();

        try {
            Date financial_year_start_date = null;

            List<TransactionType> transaction_type_list = loadTransactionTypePort.findAll();
            if (transaction_type_list != null && transaction_type_list.size() > 0) {
                for (TransactionType transactionType : transaction_type_list) {
                    String registrar = transactionType.getRegistrar();
                    String positive_transaction = transactionType.getPositive_transaction();
                    String negative_transaction = transactionType.getNegative_transaction();
                    String netural_transaction = transactionType.getNeutral_transaction();

                    if (registrar.equalsIgnoreCase("cams")) {
                        camsPositiveTransactionArrayList = new ArrayList<>(Arrays.asList(positive_transaction.split("[\\,]+")));
                        camsNegativeTransactionArrayList = new ArrayList<>(Arrays.asList(negative_transaction.split("[\\,]+")));
                        camsNeutralTransactionArrayList = new ArrayList<>(Arrays.asList(netural_transaction.split("[\\,]+")));
                    }
                    if (registrar.equalsIgnoreCase("karvy")) {
                        karvyPositiveTransactionArrayList = new ArrayList<>(Arrays.asList(positive_transaction.split("[\\,]+")));
                        karvyNegativeTransactionArrayList = new ArrayList<>(Arrays.asList(negative_transaction.split("[\\,]+")));
                        karvyNeutralTransactionArrayList = new ArrayList<>(Arrays.asList(netural_transaction.split("[\\,]+")));
                    }
                }

                camsNegativeTransactionArrayList2.addAll(camsNegativeTransactionArrayList);
                camsNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("TOCOB"));
                camsNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("TOX"));
                camsNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("TOXT"));
                camsNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Transfer Out"));

                removeTransactionArrayList.add("TOCOB");
                removeTransactionArrayList.add("TOX");
                removeTransactionArrayList.add("TOXT");
                removeTransactionArrayList.add("Transfer Out");

                karvyNegativeTransactionArrayList2.addAll(karvyNegativeTransactionArrayList);
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Transfer Out"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Transmission Out"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Transmission Out Rejection"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Demat Transfer Out"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Demat Transfer Out Rej"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Demat Transfer Out Rej Reversal"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Asset Allocation Out"));
                karvyNegativeTransactionArrayList2.removeIf(x -> x.trim().equalsIgnoreCase("Asset Allocation Out Rejection"));

                removeTransactionArrayList.add("Transfer Out");
                removeTransactionArrayList.add("Transmission Out");
                removeTransactionArrayList.add("Transmission Out Rejection");
                removeTransactionArrayList.add("Demat Transfer Out");
                removeTransactionArrayList.add("Demat Transfer Out Rej");
                removeTransactionArrayList.add("Demat Transfer Out Rej Reversal");
                removeTransactionArrayList.add("Asset Allocation Out");
                removeTransactionArrayList.add("Asset Allocation Out Rejection");
            }

            if (option.equalsIgnoreCase("range")) {
                if (start_date.isEmpty() || end_date.isEmpty()) {
                    Calendar cal = Calendar.getInstance();
                    Date financial_year_end_date = cal.getTime();
                    financial_year_end_date_str = sdf1.format(financial_year_end_date);

                    int CurrentYear = Calendar.getInstance().get(Calendar.YEAR);
                    int CurrentMonth = (Calendar.getInstance().get(Calendar.MONTH) + 1);
                    String financiyalYearFrom;
                    if (CurrentMonth < 4) {
                        financiyalYearFrom = "01-04-" + (CurrentYear - 1);
                    } else {
                        financiyalYearFrom = "01-04-" + (CurrentYear);
                    }
                    financial_year_start_date = sdf2.parse(financiyalYearFrom);
                } else {
                    financial_year_start_date = sdf2.parse(start_date);
                    financial_year_end_date_str = sdf1.format(sdf2.parse(end_date));
                }
            } else {
                if (!financial_year.equalsIgnoreCase("All")) {
                    String[] financial_year_arr = financial_year.split("-");
                    if (financial_year_arr.length > 1) {
                        String financial_start_year = financial_year_arr[0];
                        if (StringHelper.isNotEmpty(financial_start_year)) {
                            int start_year = Integer.parseInt(financial_start_year);
                            Calendar cal = Calendar.getInstance();
                            cal.set(start_year, 3, 1);
                            financial_year_start_date = cal.getTime();
                        }
                        String financial_end_year = financial_year_arr[1];
                        if (StringHelper.isNotEmpty(financial_end_year)) {
                            int end_year = Integer.parseInt(financial_end_year);
                            Calendar cal = Calendar.getInstance();
                            cal.set(end_year, 2, 31);
                            Date financial_year_end_date = cal.getTime();
                            financial_year_end_date_str = sdf1.format(financial_year_end_date);
                        } else {
                            Calendar cal = Calendar.getInstance();
                            Date financial_year_end_date = cal.getTime();
                            financial_year_end_date_str = sdf1.format(financial_year_end_date);
                        }
                    } else {
                        Calendar cal = Calendar.getInstance();
                        Date financial_year_end_date = cal.getTime();
                        financial_year_end_date_str = sdf1.format(financial_year_end_date);
                    }
                } else {
                    Calendar cal = Calendar.getInstance();
                    Date financial_year_end_date = cal.getTime();
                    financial_year_end_date_str = sdf1.format(financial_year_end_date);

                    int CurrentYear = Calendar.getInstance().get(Calendar.YEAR);
                    int CurrentMonth = (Calendar.getInstance().get(Calendar.MONTH) + 1);
                    String financiyalYearFrom;
                    if (CurrentMonth < 4) {
                        financiyalYearFrom = "01-04-" + (CurrentYear - 1);
                    } else {
                        financiyalYearFrom = "01-04-" + (CurrentYear);
                    }
                    financial_year_start_date = sdf2.parse(financiyalYearFrom);
                }
                financial_year_start_date = sdf1.parse(sdf1.format(financial_year_start_date));
            }

            Calendar cal = Calendar.getInstance();
            cal.set(2018, 0, 31);
            Date grandfathered_date = cal.getTime();
            String grandfathered_date_str = sdf1.format(grandfathered_date);
            grandfathered_date = sdf1.parse(grandfathered_date_str);

            cal = Calendar.getInstance();
            cal.set(2024, 6, 23);
            Date new_tax_slap_date = cal.getTime();
            String new_tax_slap_date_str = sdf1.format(new_tax_slap_date);
            new_tax_slap_date = sdf1.parse(new_tax_slap_date_str);

            Date debt_fund_checking_date = sdf1.parse("2023-04-01");
            Date debt_fund_index_stop_date = sdf1.parse("2024-07-23");

            // financial_year_end_date_str is used repeatedly to bound the tracker-DB queries.
            Date financial_year_end_date_bound = sdf1.parse(financial_year_end_date_str);

            List<String> fofCategoryList = new ArrayList<>();
            fofCategoryList.add("Fund of Funds-Domestic-Debt");
            fofCategoryList.add("Fund of Funds-Domestic-Equity");
            fofCategoryList.add("Fund of Funds-Domestic-Gold");
            fofCategoryList.add("Fund of Funds-Domestic-Gold and Silver");
            fofCategoryList.add("Fund of Funds-Domestic-Hybrid");
            fofCategoryList.add("Fund of Funds-Domestic-Silver");
            fofCategoryList.add("Fund of Funds-Income Plus Arbitrage");
            fofCategoryList.add("Fund of Funds-Overseas");

            // ===================== CAMS =====================
            List<Object[]> cams_scheme_list = loadCamsTaxTransactionPort.findDistinctSchemes(
                    user_id, camsNegativeTransactionArrayList2, financial_year_end_date_bound, client_name);

            for (Object[] row : cams_scheme_list) {
                String category = "";
                String scheme_amfi_code = "";
                String isin_no = "";
                String isin_divreinvst_no = "";
                String folio_no = String.valueOf(row[0]);
                String prodcode = String.valueOf(row[1]);
                String scheme = String.valueOf(row[2]);
                String scheme_amfi_short_name = scheme;
                String scheme_advisorkhoj_category = "";

                Object[] amfiRow = getSchemeMasterRow("CAMS", prodcode);
                if (amfiRow != null) {
                    category = String.valueOf(amfiRow[0]);
                    scheme = String.valueOf(amfiRow[1]);
                    scheme_amfi_code = String.valueOf(amfiRow[2]);
                    isin_no = String.valueOf(amfiRow[3]);
                    isin_divreinvst_no = String.valueOf(amfiRow[4]);
                    scheme_amfi_short_name = String.valueOf(amfiRow[5]);
                    scheme_advisorkhoj_category = String.valueOf(amfiRow[6]);
                }

                if (isin_no == null) isin_no = "";
                if (isin_divreinvst_no == null) isin_divreinvst_no = "";

                boolean hybrid_taxation = false;
                if (category.equalsIgnoreCase("HYBRID")) {
                    hybrid_taxation = true;
                    category = "Debt";
                }

                if (folio_no.equalsIgnoreCase("8389711/39") && prodcode.equalsIgnoreCase("P2059")) {
                    camsSchemeWiseInvestorTransactions = loadCamsTaxTransactionPort.findSchemeTransactionsPriceAsc(
                            user_id, folio_no, prodcode, financial_year_end_date_bound, client_name);
                } else {
                    camsSchemeWiseInvestorTransactions = loadCamsTaxTransactionPort.findSchemeTransactionsPriceDesc(
                            user_id, folio_no, prodcode, financial_year_end_date_bound, client_name);
                }

                if (camsSchemeWiseInvestorTransactions.size() > 0) {
                    camsSchemeWiseInvestorTransactions = TransactionDataUtils.removeCamsMinusTransaction(camsSchemeWiseInvestorTransactions);
                }

                if (camsSchemeWiseInvestorTransactions.size() == 0) {
                    continue;
                }

                InvestorTransactionCams ExcludedTransaction = camsSchemeWiseInvestorTransactions.stream()
                        .filter(x -> removeTransactionArrayList.stream().anyMatch(val -> val.equalsIgnoreCase(x.getTrxn_type_())))
                        .findAny().orElse(null);
                if (ExcludedTransaction != null) {
                    camsSchemeWiseInvestorTransactions.remove(ExcludedTransaction);
                }

                List<XirrResponse> positive_list = new ArrayList<>();
                XirrResponse xirrRes;
                Double grandfathered_amount = 0.0;
                Double grandfathered_units = 0.0;
                Double grandfathered_nav = 0.0;
                boolean grandfathered_flag = category.equalsIgnoreCase("Equity");
                boolean segregated_flag = false;

                for (int i = 0; i < camsSchemeWiseInvestorTransactions.size(); i++) {
                    Date traddate = camsSchemeWiseInvestorTransactions.get(i).getTraddate();
                    Double units = camsSchemeWiseInvestorTransactions.get(i).getUnits();
                    Double amount = camsSchemeWiseInvestorTransactions.get(i).getAmount();
                    Double price = camsSchemeWiseInvestorTransactions.get(i).getPurprice();
                    String trxn_type = camsSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim();
                    String reinvest_flag = camsSchemeWiseInvestorTransactions.get(i).getReinvest_f();
                    String amc_code = camsSchemeWiseInvestorTransactions.get(i).getAmc_code();
                    Double stamp_duty = camsSchemeWiseInvestorTransactions.get(i).getStamp_duty();
                    if (stamp_duty == null) stamp_duty = 0.0;

                    if (reinvest_flag.equalsIgnoreCase("Y")) {
                        isin_no = isin_divreinvst_no;
                    }

                    if (camsPositiveTransactionArrayList.contains(trxn_type)) {
                        String segre_percent = TrackerUtils.segregatedSchemes(prodcode);
                        if (!segre_percent.isEmpty()) {
                            Date check_date = null;
                            if (amc_code.equalsIgnoreCase("T")) check_date = sdf2.parse("15-06-2019");
                            if (amc_code.equalsIgnoreCase("B")) check_date = sdf2.parse("25-11-2019");
                            if (amc_code.equalsIgnoreCase("FTI")) check_date = sdf2.parse("24-01-2020");

                            if (check_date != null && traddate.compareTo(check_date) <= 0) {
                                Double percdnt = (100 - Double.parseDouble(segre_percent)) / 100.0;
                                price = price * percdnt;
                                price = Double.parseDouble(unit_decimal.format(price));
                                segregated_flag = true;
                            }
                        }

                        if (trxn_type.equalsIgnoreCase("Bonus")) {
                            price = 0.0;
                            amount = 0.0;
                            stamp_duty = 0.0;
                        }

                        Double price1 = price;
                        int index = IntStream.range(0, positive_list.size())
                                .filter(x -> positive_list.get(x).getTrxn_date().compareTo(traddate) == 0
                                        && positive_list.get(x).getNav().compareTo(price1) == 0
                                        && positive_list.get(x).getTrxn_type().equalsIgnoreCase(trxn_type))
                                .findFirst().orElse(-1);
                        if (index != -1) {
                            xirrRes = positive_list.get(index);
                            xirrRes.setUnits(xirrRes.getUnits() + units);
                            xirrRes.setCheck_units(xirrRes.getCheck_units() + units);
                            xirrRes.setAmount(xirrRes.getAmount() + amount);
                            xirrRes.setStamp_duty(xirrRes.getStamp_duty() + stamp_duty);
                        } else {
                            xirrRes = new XirrResponse();
                            xirrRes.setTrxn_type(trxn_type);
                            xirrRes.setTrxn_date(traddate);
                            xirrRes.setUnits(units);
                            xirrRes.setAmount(amount);
                            xirrRes.setNav(price);
                            xirrRes.setCheck_units(units);
                            xirrRes.setStamp_duty(stamp_duty);
                            positive_list.add(xirrRes);
                        }

                        if (traddate.compareTo(grandfathered_date) < 0) {
                            grandfathered_units = grandfathered_units + units;
                            grandfathered_units = Double.parseDouble(unit_decimal.format(grandfathered_units));
                        }

                    } else if (camsNeutralTransactionArrayList.contains(camsSchemeWiseInvestorTransactions.get(i).getTrxn_type_().trim())) {
                        // no-op, mirrors original
                    } else {
                        boolean flag = true;
                        Date purchase_date = null;
                        Double purchase_units;
                        Double purchase_amount;
                        Double purchase_nav;
                        Double reliased_gain;
                        Double stamp_duty_value;
                        int no_of_days;
                        Double stt = camsSchemeWiseInvestorTransactions.get(i).getStt();
                        Double sold_tax = camsSchemeWiseInvestorTransactions.get(i).getTotal_tax();
                        Double sold_amount = amount + sold_tax + stt;
                        Double sold_units = camsSchemeWiseInvestorTransactions.get(i).getUnits();
                        String purchase_transaction_type = "";
                        boolean grand_father_cost_apply = false;

                        if (traddate.compareTo(grandfathered_date) < 0) {
                            grandfathered_units = grandfathered_units - units;
                            grandfathered_units = Double.parseDouble(unit_decimal.format(grandfathered_units));
                        }

                        if (traddate.compareTo(grandfathered_date) > 0 && grandfathered_flag) {
                            HashMap<String, Double> merger_map = buildMergerMap();
                            if (merger_map.containsKey(prodcode)) {
                                grandfathered_nav = merger_map.get(prodcode);
                            } else {
                                grandfathered_nav = getLatestNav(scheme_amfi_code, grandfathered_date_str);
                            }
                            grandfathered_flag = false;
                        }

                        for (int k = 0; k < positive_list.size(); k++) {
                            xirrRes = positive_list.get(k);
                            purchase_units = xirrRes.getCheck_units();

                            if (purchase_units > 0) {
                                Double remain_units = purchase_units - sold_units;
                                remain_units = Double.parseDouble(four_decimal.format(remain_units));

                                if (remain_units >= 0) {
                                    xirrRes.setCheck_units(remain_units);
                                    purchase_date = xirrRes.getTrxn_date();
                                    purchase_units = xirrRes.getUnits();
                                    purchase_amount = xirrRes.getAmount();
                                    purchase_nav = xirrRes.getNav();
                                    purchase_transaction_type = xirrRes.getTrxn_type();

                                    stamp_duty_value = xirrRes.getStamp_duty() != null ? xirrRes.getStamp_duty() : 0.0;
                                    Double purchase_original_nav = purchase_nav;
                                    Double pur_amt = purchase_amount + stamp_duty_value;
                                    Double purchase_new_nav = purchase_units > 0 ? (pur_amt / purchase_units) : 0.0;
                                    if (!segregated_flag && purchase_new_nav > purchase_nav) {
                                        purchase_nav = purchase_new_nav;
                                    }
                                    no_of_days = TrackerUtils.getDaysBetweenDates(purchase_date, traddate);
                                    Double sold_nav = camsSchemeWiseInvestorTransactions.get(i).getPurprice();

                                    if (fofCategoryList.contains(scheme_advisorkhoj_category)
                                            && purchase_date.compareTo(debt_fund_checking_date) < 0
                                            && traddate.compareTo(debt_fund_index_stop_date) < 0) {
                                        category = "Debt";
                                    }

                                    if (category.equalsIgnoreCase("Equity") && grandfathered_nav != 0) {
                                        if (no_of_days > 365 && purchase_date.compareTo(grandfathered_date) < 0) {
                                            if (sold_nav > grandfathered_nav && purchase_nav < grandfathered_nav) {
                                                grand_father_cost_apply = true;
                                                reliased_gain = Double.parseDouble(decimal.format((sold_nav - grandfathered_nav) * sold_units));
                                            } else if (sold_nav > purchase_nav && purchase_nav > grandfathered_nav) {
                                                reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * sold_units));
                                            } else {
                                                reliased_gain = sold_nav > purchase_nav ? 0.0
                                                        : Double.parseDouble(decimal.format((sold_nav - purchase_nav) * sold_units));
                                            }
                                        } else {
                                            reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * sold_units));
                                        }
                                    } else {
                                        reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * sold_units));
                                    }

                                    double actual_amount = purchase_units > 0
                                            ? (sold_units * purchase_original_nav) + ((stamp_duty_value / purchase_units) * sold_units)
                                            : 0.0;
                                    actual_amount = Double.parseDouble(decimal.format(actual_amount));

                                    if (traddate.compareTo(financial_year_start_date) >= 0) {
                                        investorSchemeWiseTransactionTaxReport = buildBaseReport(
                                                scheme, hybrid_taxation, prodcode, scheme_amfi_short_name, folio_no,
                                                isin_no, category);

                                        if (flag) {
                                            populateSoldFields(investorSchemeWiseTransactionTaxReport, traddate, units,
                                                    sold_amount, price, trxn_type, sold_tax, stt);
                                            flag = false;
                                        } else {
                                            populateEmptySoldFields(investorSchemeWiseTransactionTaxReport, traddate, true);
                                        }

                                        investorSchemeWiseTransactionTaxReport.setPurchase_date(purchase_date);
                                        investorSchemeWiseTransactionTaxReport.setPurchase_units(sold_units);
                                        investorSchemeWiseTransactionTaxReport.setPurchase_amount(Double.parseDouble(two_decimal.format(sold_units * purchase_nav)));
                                        investorSchemeWiseTransactionTaxReport.setPurchase_nav(Double.parseDouble(four_decimal.format(purchase_original_nav)));
                                        investorSchemeWiseTransactionTaxReport.setPurchase_transaction_type(purchase_transaction_type);
                                        investorSchemeWiseTransactionTaxReport.setRedeemed_units(sold_units);
                                        investorSchemeWiseTransactionTaxReport.setRedeemed_nav(price);
                                        investorSchemeWiseTransactionTaxReport.setActual_amount(actual_amount);
                                        investorSchemeWiseTransactionTaxReport.setGrandfathered_cost_apply(grand_father_cost_apply);

                                        applyGrandfathering(investorSchemeWiseTransactionTaxReport, category, purchase_date,
                                                grandfathered_date, sold_units, grandfathered_units, grandfathered_nav);

                                        investorSchemeWiseTransactionTaxReport.setNo_of_days(no_of_days);

                                        applyTaxSlab(investorSchemeWiseTransactionTaxReport, category, hybrid_taxation,
                                                no_of_days, purchase_date, traddate, reliased_gain, new_tax_slap_date,
                                                debt_fund_checking_date, debt_fund_index_stop_date, purchase_nav,
                                                sold_nav, sold_units, sold_nav /*placeholder unused*/);

                                        investorSchemeWiseTransactionTaxReportList.add(investorSchemeWiseTransactionTaxReport);
                                    }
                                    break;
                                } else {
                                    purchase_date = xirrRes.getTrxn_date();
                                    purchase_units = xirrRes.getUnits();
                                    purchase_amount = xirrRes.getAmount();
                                    purchase_nav = xirrRes.getNav();
                                    purchase_transaction_type = xirrRes.getTrxn_type();
                                    stamp_duty_value = xirrRes.getStamp_duty() != null ? xirrRes.getStamp_duty() : 0.0;
                                    Double purchase_original_nav = purchase_nav;
                                    Double pur_amt = purchase_amount + stamp_duty_value;
                                    Double purchase_new_nav = pur_amt / purchase_units;
                                    if (!segregated_flag && purchase_new_nav > purchase_nav) {
                                        purchase_nav = purchase_new_nav;
                                    }
                                    no_of_days = TrackerUtils.getDaysBetweenDates(purchase_date, traddate);
                                    Double sold_nav = camsSchemeWiseInvestorTransactions.get(i).getPurprice();
                                    Double current_units = xirrRes.getCheck_units();
                                    sold_units = Double.parseDouble(four_decimal.format(sold_units - current_units));

                                    if (category.equalsIgnoreCase("Equity") && grandfathered_nav != 0) {
                                        if (no_of_days > 365 && purchase_date.compareTo(grandfathered_date) < 0) {
                                            if (sold_nav > grandfathered_nav && purchase_nav < grandfathered_nav) {
                                                grand_father_cost_apply = true;
                                                reliased_gain = Double.parseDouble(decimal.format((sold_nav - grandfathered_nav) * current_units));
                                            } else if (sold_nav > purchase_nav && purchase_nav > grandfathered_nav) {
                                                reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * current_units));
                                            } else {
                                                reliased_gain = sold_nav > purchase_nav ? 0.0
                                                        : Double.parseDouble(decimal.format((sold_nav - purchase_nav) * current_units));
                                            }
                                        } else {
                                            reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * current_units));
                                        }
                                    } else {
                                        reliased_gain = Double.parseDouble(decimal.format((sold_nav - purchase_nav) * current_units));
                                    }
                                    xirrRes.setCheck_units(0.0);

                                    Double actual_amount = (current_units * purchase_nav) + ((stamp_duty_value / purchase_units) * current_units);
                                    actual_amount = Double.parseDouble(decimal.format(actual_amount));

                                    if (traddate.compareTo(financial_year_start_date) >= 0) {
                                        investorSchemeWiseTransactionTaxReport = buildBaseReport(
                                                scheme, hybrid_taxation, prodcode, scheme_amfi_short_name, folio_no,
                                                isin_no, category);

                                        if (flag) {
                                            populateSoldFields(investorSchemeWiseTransactionTaxReport, traddate, units,
                                                    sold_amount, price, trxn_type, sold_tax, stt);
                                            flag = false;
                                        } else {
                                            populateEmptySoldFields(investorSchemeWiseTransactionTaxReport, traddate, false);
                                        }

                                        investorSchemeWiseTransactionTaxReport.setPurchase_date(purchase_date);
                                        investorSchemeWiseTransactionTaxReport.setPurchase_units(current_units);
                                        investorSchemeWiseTransactionTaxReport.setPurchase_amount(Double.parseDouble(two_decimal.format(current_units * purchase_nav)));
                                        investorSchemeWiseTransactionTaxReport.setPurchase_nav(Double.parseDouble(four_decimal.format(purchase_original_nav)));
                                        investorSchemeWiseTransactionTaxReport.setPurchase_transaction_type(purchase_transaction_type);
                                        investorSchemeWiseTransactionTaxReport.setRedeemed_units(current_units);
                                        investorSchemeWiseTransactionTaxReport.setRedeemed_nav(price);
                                        investorSchemeWiseTransactionTaxReport.setActual_amount(actual_amount);
                                        investorSchemeWiseTransactionTaxReport.setGrandfathered_cost_apply(grand_father_cost_apply);

                                        applyGrandfathering(investorSchemeWiseTransactionTaxReport, category, purchase_date,
                                                grandfathered_date, current_units, grandfathered_units, grandfathered_nav);

                                        investorSchemeWiseTransactionTaxReport.setNo_of_days(no_of_days);

                                        applyTaxSlab(investorSchemeWiseTransactionTaxReport, category, hybrid_taxation,
                                                no_of_days, purchase_date, traddate, reliased_gain, new_tax_slap_date,
                                                debt_fund_checking_date, debt_fund_index_stop_date, purchase_nav,
                                                sold_nav, current_units, sold_nav /*placeholder unused*/);

                                        investorSchemeWiseTransactionTaxReportList.add(investorSchemeWiseTransactionTaxReport);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===================== KARVY =====================
            // NOTE: the Karvy block mirrors the CAMS block above (segregation maps, tr_charges,
            // "New code start/end" fallback branch for zero-price closures, etc). Due to its size
            // and near-duplicate structure with CAMS it is broken out into processKarvyTransactions()
            // for readability -- port the equivalent body from the legacy method into that method
            // using karvyRepository / helper methods the same way the CAMS block above does.
            processKarvyTransactions(user_id, client_name, financial_year_end_date_bound, financial_year_start_date,
                    grandfathered_date, grandfathered_date_str, new_tax_slap_date, debt_fund_checking_date,
                    debt_fund_index_stop_date, fofCategoryList, karvyNegativeTransactionArrayList2,
                    karvyPositiveTransactionArrayList, karvyNeutralTransactionArrayList, removeTransactionArrayList,
                    decimal, four_decimal, unit_decimal, two_decimal, sdf1, sdf2,
                    investorSchemeWiseTransactionTaxReportList);

        } catch (ParseException pe) {
            throw new RuntimeException("Unable to parse date while generating tax report", pe);
        }

        return investorSchemeWiseTransactionTaxReportList;
    }

    // ------------------------------------------------------------------
    // Helper: AMFI scheme master lookup (replaces amfiSession.createSQLQuery on scheme master)
    // ------------------------------------------------------------------
    private Object[] getSchemeMasterRow(String src, String productCode) {
        List<Object[]> rows = loadSchemeMasterPort.findSchemeMasterByProductCodeNew(src, productCode);
        return (rows != null && !rows.isEmpty()) ? rows.get(0) : null;
    }

    // ------------------------------------------------------------------
    // Helper: latest NAV on/before a given date (replaces amfi_mf_nav lookup)
    // ------------------------------------------------------------------
    private Double getLatestNav(String schemeAmfiCode, String dateStr) {
        List<Double> navList = loadNavHistoryPort.findLatestNav(schemeAmfiCode, dateStr);
        return (navList != null && !navList.isEmpty()) ? navList.get(0) : 0.0;
    }

    // ------------------------------------------------------------------
    // Helper: cost inflation index on/before a given date (replaces InflationIndex lookup)
    // ------------------------------------------------------------------
    private Double getInflationIndex(String dateStr) {
        List<Double> list = loadInflationIndexPort.findInflationIndex(dateStr);
        return (list != null && !list.isEmpty()) ? list.get(0) : 0.0;
    }

    private HashMap<String, Double> buildMergerMap() {
        HashMap<String, Double> merger_map = new HashMap<>();
        merger_map.put("HGFG", 197.5876);
        merger_map.put("HGFD", 34.2983);
        merger_map.put("HPREG", 53.4656);
        merger_map.put("HPRED", 13.6063);
        merger_map.put("OLMEG", 11.3147);
        merger_map.put("OLEBFP", 23.8010);
        merger_map.put("OFIVFG", 39.2980);
        merger_map.put("OLEBFG", 28.2070);
        merger_map.put("OMED", 34.2260);
        merger_map.put("O17", 85.8220);
        merger_map.put("OEOID", 33.3475);
        merger_map.put("OEOIG", 85.8220);
        merger_map.put("OEFG", 204.9980);
        merger_map.put("OLMED", 16.9061);
        merger_map.put("OFTAFD", 26.5370);
        merger_map.put("OFTAFG", 58.3370);
        merger_map.put("OLBCFG", 17.5910);
        merger_map.put("OFCPMG", 23.0090);
        merger_map.put("OFCPEG", 26.8330);
        merger_map.put("178MCRG", 36.1810);
        return merger_map;
    }

    private InvestorSchemeWiseTransactionTaxReport buildBaseReport(String scheme, boolean hybrid_taxation,
                                                                   String scheme_code, String scheme_amfi_short_name, String folio, String isin_no, String category) {
        InvestorSchemeWiseTransactionTaxReport report = new InvestorSchemeWiseTransactionTaxReport();
        report.setScheme(scheme);
        report.setHybrid_taxation(hybrid_taxation);
        report.setScheme_code(scheme_code);
        report.setScheme_amfi_short_name(scheme_amfi_short_name);
        report.setFolio(folio);
        report.setIsin_no(isin_no);
        report.setCategory(category);
        return report;
    }

    private void populateSoldFields(InvestorSchemeWiseTransactionTaxReport report, Date traddate, Double units,
                                    Double sold_amount, Double price, String trxn_type, Double sold_tax, Double stt) {
        report.setSold_date(traddate);
        report.setSold_repeated_date(traddate);
        report.setSold_units(units);
        report.setSold_amount(sold_amount);
        report.setSold_nav(price);
        report.setSold_transaction_type(trxn_type);
        report.setTotal_tax(sold_tax);
        report.setStt(stt);
    }

    private void populateEmptySoldFields(InvestorSchemeWiseTransactionTaxReport report, Date traddate, boolean resetGrandfathered) {
        report.setSold_date(null);
        report.setSold_repeated_date(traddate);
        report.setSold_units(0.0);
        report.setSold_amount(0.0);
        report.setSold_nav(0.0);
        report.setSold_transaction_type("");
        report.setTotal_tax(0.0);
        report.setStt(0.0);
        if (resetGrandfathered) {
            report.setGrandfathered_amount(0.0);
            report.setGrandfathered_nav(0.0);
            report.setGrandfathered_units(0.0);
        }
    }

    private void applyGrandfathering(InvestorSchemeWiseTransactionTaxReport report, String category,
                                     Date purchase_date, Date grandfathered_date, Double units, Double grandfathered_units, Double grandfathered_nav) {
        if (category.equalsIgnoreCase("Equity") && purchase_date.compareTo(grandfathered_date) < 0) {
            if (units <= grandfathered_units) {
                report.setGrandfathered_amount(units * grandfathered_nav);
                report.setGrandfathered_nav(grandfathered_nav);
                report.setGrandfathered_units(units);
            } else {
                report.setGrandfathered_amount(grandfathered_units * grandfathered_nav);
                report.setGrandfathered_nav(grandfathered_nav);
                report.setGrandfathered_units(grandfathered_units);
            }
        } else {
            report.setGrandfathered_amount(0.0);
            report.setGrandfathered_nav(0.0);
            report.setGrandfathered_units(0.0);
        }
    }

    /**
     * Applies the LTCG / STCG / indexed-gain tax-slab rules. Mirrors the nested if/else chain
     * repeated four times in the legacy method (Equity vs Debt/Hybrid, pre/post 23-Jul-2024 slab
     * change, indexation cutover on 2023-04-01 / 2024-07-23).
     */
    private void applyTaxSlab(InvestorSchemeWiseTransactionTaxReport report, String category, boolean hybrid_taxation,
                              int no_of_days, Date purchase_date, Date traddate, Double reliased_gain, Date new_tax_slap_date,
                              Date debt_fund_checking_date, Date debt_fund_index_stop_date, Double purchase_nav, Double sold_nav,
                              Double units, Double unused) throws ParseException {

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");

        if (category.equalsIgnoreCase("Equity")) {
            if (no_of_days >= 365) {
                report.setIndexed_nav(0.0);
                report.setStg_gain(0.0);
                report.setLtg_gain(reliased_gain);
                report.setIndexed_gain(0.0);
            } else {
                report.setIndexed_nav(0.0);
                report.setStg_gain(reliased_gain);
                report.setLtg_gain(0.0);
                report.setIndexed_gain(0.0);
            }
            return;
        }

        if (hybrid_taxation && no_of_days >= 730 && purchase_date.compareTo(new_tax_slap_date) >= 0) {
            report.setIndexed_nav(0.0);
            report.setStg_gain(reliased_gain);
            report.setLtg_gain(0.0);
            report.setIndexed_gain(0.0);
        } else if (no_of_days >= 1095) {
            if (purchase_date.compareTo(debt_fund_checking_date) >= 0 && !hybrid_taxation) {
                report.setIndexed_nav(0.0);
                report.setStg_gain(reliased_gain);
                report.setLtg_gain(0.0);
                report.setIndexed_gain(0.0);
            } else if (traddate.compareTo(debt_fund_index_stop_date) >= 0) {
                report.setIndexed_nav(0.0);
                report.setStg_gain(0.0);
                report.setLtg_gain(reliased_gain);
                report.setIndexed_gain(0.0);
            } else {
                String purchase_date_str = sdf1.format(purchase_date);
                String sold_date_str = sdf1.format(traddate);
                Double purchase_inflation_cost = getInflationIndex(purchase_date_str);
                Double sold_inflation_cost = getInflationIndex(sold_date_str);

                Double indexedNAV = purchase_nav * (sold_inflation_cost / purchase_inflation_cost);
                Double reliased_gain1 = Double.parseDouble(new java.text.DecimalFormat("#.##")
                        .format((sold_nav - indexedNAV) * units));

                report.setIndexed_nav(indexedNAV);
                report.setStg_gain(0.0);
                report.setLtg_gain(0.0);
                report.setIndexed_gain(reliased_gain1);
            }
        } else if (no_of_days >= 730 && purchase_date.compareTo(debt_fund_checking_date) < 0) {
            report.setIndexed_nav(0.0);
            report.setStg_gain(0.0);
            report.setLtg_gain(reliased_gain);
            report.setIndexed_gain(0.0);
        } else if (no_of_days >= 730 && traddate.compareTo(debt_fund_index_stop_date) >= 0) {
            report.setIndexed_nav(0.0);
            report.setStg_gain(reliased_gain);
            report.setLtg_gain(0.0);
            report.setIndexed_gain(0.0);
        } else {
            report.setIndexed_nav(0.0);
            report.setStg_gain(reliased_gain);
            report.setLtg_gain(0.0);
            report.setIndexed_gain(0.0);
        }
    }

    /**
     * Karvy leg of the report. Structurally identical to the CAMS block (segregation % maps keyed
     * by AMC code, transfer-charge amortisation, transaction-number de-duplication, and the
     * zero-price/zero-unit "sold_amount" fallback branch for switches). Port the corresponding
     * body from the legacy monolith into here, replacing:
     *  - session.createQuery(distinct schemes)      -> loadKarvyTaxTransactionPort.findDistinctSchemes(...)
     *  - session.createQuery(scheme transactions)   -> loadKarvyTaxTransactionPort.findSchemeTransactions(...)
     *  - session.createQuery(segregated list)       -> loadKarvyTaxTransactionPort.findSegregatedTransactionsNew(...)
     *  - amfiSession scheme-master / nav / inflation lookups -> getSchemeMasterRow / getLatestNav / getInflationIndex
     * using the same buildBaseReport / populateSoldFields / applyGrandfathering / applyTaxSlab helpers.
     */
    private void processKarvyTransactions(Integer user_id, String client_name, Date financial_year_end_date_bound,
                                          Date financial_year_start_date, Date grandfathered_date, String grandfathered_date_str,
                                          Date new_tax_slap_date, Date debt_fund_checking_date, Date debt_fund_index_stop_date,
                                          List<String> fofCategoryList, List<String> karvyNegativeTransactionArrayList2,
                                          List<String> karvyPositiveTransactionArrayList, List<String> karvyNeutralTransactionArrayList,
                                          List<String> removeTransactionArrayList, DecimalFormat decimal, DecimalFormat four_decimal,
                                          DecimalFormat unit_decimal, DecimalFormat two_decimal, SimpleDateFormat sdf1, SimpleDateFormat sdf2,
                                          List<InvestorSchemeWiseTransactionTaxReport> investorSchemeWiseTransactionTaxReportList) throws ParseException {

        List<Object[]> karvy_scheme_list = loadKarvyTaxTransactionPort.findDistinctSchemes(
                user_id, karvyNegativeTransactionArrayList2, financial_year_end_date_bound, client_name);

        for (Object[] row : karvy_scheme_list) {
            String folio_number = String.valueOf(row[0]);
            String fund = String.valueOf(row[1]);
            String schemecode = String.valueOf(row[2]);
            String scheme = String.valueOf(row[3]);
            String product_code = fund + schemecode;
            String category = "";
            String scheme_amfi_code = "";
            String isin_no = "";
            String isin_divreinvst_no = "";
            String scheme_amfi_short_name = scheme;
            String scheme_advisorkhoj_category = "";

            Object[] amfiRow = getSchemeMasterRow("KARVY", product_code);
            if (amfiRow != null) {
                category = String.valueOf(amfiRow[0]);
                scheme = String.valueOf(amfiRow[1]);
                scheme_amfi_code = String.valueOf(amfiRow[2]);
                isin_no = String.valueOf(amfiRow[3]);
                isin_divreinvst_no = String.valueOf(amfiRow[4]);
                scheme_amfi_short_name = String.valueOf(amfiRow[5]);
                scheme_advisorkhoj_category = String.valueOf(amfiRow[6]);
            }
            if (isin_no == null) isin_no = "";
            if (isin_divreinvst_no == null) isin_divreinvst_no = "";

            boolean hybrid_taxation = false;
            if (category.equalsIgnoreCase("HYBRID")) {
                hybrid_taxation = true;
                category = "Debt";
            }

            List<InvestorTransactionKarvy> karvySchemeWiseInvestorTransactions = loadKarvyTaxTransactionPort.findSchemeTransactions(
                    user_id, folio_number, product_code, financial_year_end_date_bound, client_name);

            if (karvySchemeWiseInvestorTransactions.size() > 0) {
                karvySchemeWiseInvestorTransactions = TransactionDataUtils.removeKarvyMinusTransaction(karvySchemeWiseInvestorTransactions);
            }
            if (karvySchemeWiseInvestorTransactions.size() == 0) {
                continue;
            }

            InvestorTransactionKarvy excluded = karvySchemeWiseInvestorTransactions.stream()
                    .filter(x -> removeTransactionArrayList.stream().anyMatch(val -> val.equalsIgnoreCase(x.getTransaction_description())))
                    .findAny().orElse(null);
            if (excluded != null) {
                karvySchemeWiseInvestorTransactions.remove(excluded);
            }

            HashMap<String, String> segre_map = new HashMap<String, String>();
            segre_map.put("RMF05GP", "RMFMIGP");
            segre_map.put("RMF05MP", "RMFMIMP");
            segre_map.put("RMF05QP", "RMFMIQP");
            segre_map.put("RMF01GP", "RMFCPGP");
            segre_map.put("10804GP", "108DBGP");
            segre_map.put("10802GP", "108COGP");

            List<InvestorTransactionKarvy> segregattedList = null;
            if (segre_map.containsKey(product_code)) {
                String segre_code = segre_map.get(product_code);
                segregattedList = loadKarvyTaxTransactionPort.findSegregatedTransactionsNew(
                        user_id, folio_number, segre_code, financial_year_end_date_bound, client_name);
            }

            List<XirrResponse> positive_list = new ArrayList<XirrResponse>();
            XirrResponse xirrRes = null;
            Double grandfathered_amount = 0.0;
            Double grandfathered_units = 0.0;
            Double grandfathered_nav = 0.0;
            boolean grandfathered_flag = false;
            if (category.equalsIgnoreCase("Equity")) {
                grandfathered_flag = true;
            }
            boolean segregated_flag = false;
            String old_transaction_number = "";

            for (int i = 0; i < karvySchemeWiseInvestorTransactions.size(); i++) {
                Date traddate = karvySchemeWiseInvestorTransactions.get(i).getTransaction_date();
                Double units = karvySchemeWiseInvestorTransactions.get(i).getUnits();
                Double amount = karvySchemeWiseInvestorTransactions.get(i).getAmount();
                Double price = karvySchemeWiseInvestorTransactions.get(i).getPrice();
                String trxn_type = karvySchemeWiseInvestorTransactions.get(i).getTransaction_description();
                String isin = karvySchemeWiseInvestorTransactions.get(i).getIsin();
                String reinvest_flag = karvySchemeWiseInvestorTransactions.get(i).getDividend_option();
                String tr_charges = karvySchemeWiseInvestorTransactions.get(i).getTrcharges();
                String amc_code = karvySchemeWiseInvestorTransactions.get(i).getFund();
                Double stamp_duty = karvySchemeWiseInvestorTransactions.get(i).getStampduty();
                String transaction_number = karvySchemeWiseInvestorTransactions.get(i).getTransaction_number();
                if (tr_charges == null || tr_charges.isEmpty()) {
                    tr_charges = "0";
                }

                String obj = removeTransactionArrayList.stream().filter(x -> x.equalsIgnoreCase(trxn_type)).findAny().orElse(null);
                if (obj != null) {
                    continue;
                }

                if (!transaction_number.isEmpty() && !transaction_number.equalsIgnoreCase("0") && transaction_number.equalsIgnoreCase(old_transaction_number)) {
                    old_transaction_number = transaction_number;
                } else {
                    old_transaction_number = transaction_number;
                }

                if (segre_map.containsKey(product_code) && price == 0) {
                    InvestorTransactionKarvy portfolio = segregattedList.stream()
                            .filter(x -> x.getTransaction_date().compareTo(traddate) == 0)
                            .findAny().orElse(null);
                    if (portfolio != null) {
                        price = portfolio.getPrice();
                        if (product_code.equalsIgnoreCase("RMF01GP")) {
                            price = price * 0.0145;
                        } else if (product_code.equalsIgnoreCase("RMF05GP") || product_code.equalsIgnoreCase("RMF05MP") || product_code.equalsIgnoreCase("RMF05QP")) {
                            price = price * 0.0333;
                        } else if (product_code.equalsIgnoreCase("10804GP")) {
                            price = price * 0.01355;
                        } else if (product_code.equalsIgnoreCase("10802GP")) {
                            price = price * 0.03447;
                        }
                        price = Double.parseDouble(unit_decimal.format(price));
                        segregated_flag = true;
                    }
                }

                if (!isin.isEmpty()) {
                    isin_no = isin;
                } else {
                    if (reinvest_flag.equalsIgnoreCase("R")) {
                        isin_no = isin_divreinvst_no;
                    }
                }

                if (karvyPositiveTransactionArrayList.contains(trxn_type)) {
                    String segre_percent = TrackerUtils.segregatedSchemes(product_code);
                    if (!segre_percent.isEmpty()) {
                        if (amc_code.equalsIgnoreCase("108")) {
                            segregated_flag = true;
                            Date check_date = null;
                            List<String> sch_code_list1 = new ArrayList<String>();
                            sch_code_list1.add("108COGP");
                            if (sch_code_list1.contains(product_code)) {
                                check_date = sdf2.parse("17-02-2020");
                            }
                            if (check_date != null && traddate.compareTo(check_date) <= 0) {
                                Double percdnt = Double.parseDouble(segre_percent) / 100.0;
                                price = price * percdnt;
                                price = Double.parseDouble(unit_decimal.format(price));
                            }
                        } else {
                            Date check_date = null;
                            if (amc_code.equalsIgnoreCase("RMF")) {
                                List<String> sch_code_list1 = new ArrayList<String>();
                                sch_code_list1.add("RMFCPGP");
                                sch_code_list1.add("RMFCPDP");
                                sch_code_list1.add("RMFCPQP");
                                sch_code_list1.add("RMFCPMP");
                                sch_code_list1.add("RMFCPDD");
                                sch_code_list1.add("RMFSHGP");
                                sch_code_list1.add("RMFSHDP");
                                sch_code_list1.add("RMFSHMP");
                                sch_code_list1.add("RMFSHQP");
                                sch_code_list1.add("RMFSHDD");
                                sch_code_list1.add("RMFESGP");
                                sch_code_list1.add("RMFESDP");
                                sch_code_list1.add("RMFESMP");
                                sch_code_list1.add("RMFESQP");
                                sch_code_list1.add("RMFESDD");

                                List<String> sch_code_list2 = new ArrayList<String>();
                                sch_code_list2.add("RMFMIGP");
                                sch_code_list2.add("RMFMIDP");
                                sch_code_list2.add("RMFMIMP");
                                sch_code_list2.add("RMFMIQP");
                                sch_code_list2.add("RMFMIDD");
                                sch_code_list2.add("RMFCBGP");
                                sch_code_list2.add("RMFCBDP");
                                sch_code_list2.add("RMFCBMP");
                                sch_code_list2.add("RMFCBQP");
                                sch_code_list2.add("RMFCBDD");
                                sch_code_list2.add("RMFSDGP");
                                sch_code_list2.add("RMFSDDP");
                                sch_code_list2.add("RMFSDDR");
                                sch_code_list2.add("RMFSDMP");
                                sch_code_list2.add("RMFSDQP");
                                sch_code_list2.add("RMFSDDD");

                                if (sch_code_list1.contains(product_code)) {
                                    check_date = sdf2.parse("25-09-2019");
                                }
                                if (sch_code_list2.contains(product_code)) {
                                    check_date = sdf2.parse("17-02-2020");
                                }
                            }
                            if (traddate.compareTo(check_date) <= 0) {
                                Double percdnt = (100 - Double.parseDouble(segre_percent)) / 100.0;
                                price = price * percdnt;
                                price = Double.parseDouble(unit_decimal.format(price));
                                segregated_flag = true;
                            }
                        }
                    }

                    if (product_code.equalsIgnoreCase("RMF02GP") || product_code.equalsIgnoreCase("RMF02DP")) {
                        Double percdnt = 0.001;
                        price = price * percdnt;
                        price = Double.parseDouble(unit_decimal.format(price));
                    }

                    if (trxn_type.equalsIgnoreCase("Bonus") || trxn_type.equalsIgnoreCase("Bonus Units") ||
                            trxn_type.equalsIgnoreCase("Bonus Rejection") || trxn_type.equalsIgnoreCase("Bonus Rejection Reversal")) {
                        price = 0.0;
                        amount = 0.0;
                        stamp_duty = 0.0;
                    }

                    Double price1 = price;
                    int index = IntStream.range(0, positive_list.size())
                            .filter(x -> positive_list.get(x).getTrxn_date().compareTo(traddate) == 0
                                    && positive_list.get(x).getNav().compareTo(price1) == 0
                                    && positive_list.get(x).getTrxn_type().equalsIgnoreCase(trxn_type))
                            .findFirst().orElse(-1);
                    if (index != -1) {
                        xirrRes = positive_list.get(index);
                        xirrRes.setUnits(xirrRes.getUnits() + units);
                        xirrRes.setCheck_units(xirrRes.getCheck_units() + units);
                        xirrRes.setAmount(xirrRes.getAmount() + amount);
                        xirrRes.setStamp_duty(xirrRes.getStamp_duty() + stamp_duty);
                        xirrRes.setTr_charges(xirrRes.getTr_charges() + Double.parseDouble(tr_charges));
                    } else {
                        xirrRes = new XirrResponse();
                        xirrRes.setTrxn_type(trxn_type);
                        xirrRes.setTrxn_date(traddate);
                        xirrRes.setUnits(units);
                        xirrRes.setAmount(amount);
                        xirrRes.setNav(price);
                        xirrRes.setCheck_units(units);
                        xirrRes.setStamp_duty(stamp_duty);
                        xirrRes.setTr_charges(Double.parseDouble(tr_charges));
                        positive_list.add(xirrRes);
                    }

                    if (traddate.compareTo(grandfathered_date) < 0) {
                        grandfathered_units = grandfathered_units + units;
                        grandfathered_units = Double.parseDouble(unit_decimal.format(grandfathered_units));
                    }

                } else if (karvyNeutralTransactionArrayList.contains(trxn_type)) {
                    // No-op, matches original
                } else {
                    // Negative transaction processing
                    boolean flag = true;
                    Date purchase_date = null;
                    Double purchase_units = 0.0;
                    Double purchase_amount = 0.0;
                    Double purchase_nav = 0.0;
                    Double reliased_gain = 0.0;
                    int no_of_days = 0;
                    Double sold_tax = 0.0;
                    Double stt = 0.0;
                    Double pur_tr_charges = 0.0;
                    Double stamp_duty_value = 0.0;
                    Double sold_units = karvySchemeWiseInvestorTransactions.get(i).getUnits();
                    Double sold_amount = karvySchemeWiseInvestorTransactions.get(i).getAmount();
                    String purchase_transaction_type = "";
                    boolean grand_father_cost_apply = false;

                    if (karvySchemeWiseInvestorTransactions.get(i).getTdsamount() != null) {
                        sold_tax = karvySchemeWiseInvestorTransactions.get(i).getTdsamount();
                    }
                    if (karvySchemeWiseInvestorTransactions.get(i).getStt() != null) {
                        stt = karvySchemeWiseInvestorTransactions.get(i).getStt();
                    }

                    if (traddate.compareTo(grandfathered_date) < 0) {
                        grandfathered_units = grandfathered_units - units;
                        grandfathered_units = Double.parseDouble(unit_decimal.format(grandfathered_units));
                    }

                    if (traddate.compareTo(grandfathered_date) > 0 && grandfathered_flag) {
                        if (product_code.equalsIgnoreCase("178MCRG")) {
                            grandfathered_nav = 36.1810;
                        } else if (product_code.equalsIgnoreCase("RMF02GP") || product_code.equalsIgnoreCase("RMF02DP")) {
                            grandfathered_nav = 0.0567;
                        } else {
                            grandfathered_nav = getLatestNav(scheme_amfi_code, grandfathered_date_str);
                        }
                        grandfathered_flag = false;
                    }

                    for (int k = 0; k < positive_list.size(); k++) {
                        xirrRes = positive_list.get(k);
                        purchase_units = xirrRes.getCheck_units();

                        if (purchase_units > 0) {
                            Double remain_units = purchase_units - sold_units;
                            remain_units = Double.parseDouble(four_decimal.format(remain_units));

                            if (remain_units >= 0) {
                                xirrRes.setCheck_units(remain_units);
                                purchase_date = xirrRes.getTrxn_date();
                                purchase_units = xirrRes.getUnits();
                                purchase_amount = xirrRes.getAmount();
                                purchase_nav = xirrRes.getNav();
                                purchase_transaction_type = xirrRes.getTrxn_type();
                                stamp_duty_value = xirrRes.getStamp_duty();
                                pur_tr_charges = xirrRes.getTr_charges();
                                Double purchase_original_nav = purchase_nav;
                                Double pur_amt = purchase_amount + stamp_duty_value + pur_tr_charges;
                                Double purchase_new_nav = (pur_amt / purchase_units);
                                if (!segregated_flag && purchase_new_nav > purchase_nav) {
                                    purchase_nav = purchase_new_nav;
                                }
                                Date sold_date = karvySchemeWiseInvestorTransactions.get(i).getTransaction_date();
                                no_of_days = TrackerUtils.getDaysBetweenDates(purchase_date, sold_date);
                                Double sold_nav = karvySchemeWiseInvestorTransactions.get(i).getPrice();

                                if (price == 0) {
                                    if (product_code.equalsIgnoreCase("RMF01GP") || product_code.equalsIgnoreCase("108COSG") || product_code.equalsIgnoreCase("RMFMIMP")) {
                                        // no-op
                                    } else {
                                        no_of_days = 0;
                                    }
                                }

                                if (fofCategoryList.contains(scheme_advisorkhoj_category) && purchase_date.compareTo(debt_fund_checking_date) < 0 && traddate.compareTo(debt_fund_index_stop_date) < 0) {
                                    category = "Debt";
                                }

                                if (category.equalsIgnoreCase("Equity") && grandfathered_nav != 0) {
                                    if (no_of_days > 365 && purchase_date.compareTo(grandfathered_date) < 0) {
                                        if (sold_nav > grandfathered_nav && purchase_nav < grandfathered_nav) {
                                            grand_father_cost_apply = true;
                                            reliased_gain = (sold_nav - grandfathered_nav) * sold_units;
                                            reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                        } else if (sold_nav > purchase_nav && purchase_nav > grandfathered_nav) {
                                            reliased_gain = (sold_nav - purchase_nav) * sold_units;
                                            reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                        } else {
                                            if (sold_nav > purchase_nav) {
                                                reliased_gain = 0.0;
                                            } else {
                                                reliased_gain = (sold_nav - purchase_nav) * sold_units;
                                                reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                            }
                                        }
                                    } else {
                                        reliased_gain = (sold_nav - purchase_nav) * sold_units;
                                        reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                    }
                                } else {
                                    reliased_gain = (sold_nav - purchase_nav) * sold_units;
                                    reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                }

                                double actual_amount = (sold_units * purchase_original_nav) + ((stamp_duty_value / purchase_units) * sold_units);
                                actual_amount = Double.parseDouble(decimal.format(actual_amount));

                                if (sold_date.compareTo(financial_year_start_date) >= 0) {
                                    InvestorSchemeWiseTransactionTaxReport investorSchemeWiseTransactionTaxReport = buildBaseReport(
                                            scheme, hybrid_taxation, product_code, scheme_amfi_short_name, folio_number,
                                            isin_no, category);

                                    if (flag) {
                                        populateSoldFields(investorSchemeWiseTransactionTaxReport, traddate, units,
                                                amount, price, trxn_type, sold_tax, stt);
                                        flag = false;
                                    } else {
                                        populateEmptySoldFields(investorSchemeWiseTransactionTaxReport, traddate, true);
                                    }

                                    investorSchemeWiseTransactionTaxReport.setPurchase_date(purchase_date);
                                    investorSchemeWiseTransactionTaxReport.setPurchase_units(sold_units);
                                    investorSchemeWiseTransactionTaxReport.setPurchase_amount(Double.parseDouble(two_decimal.format(sold_units * purchase_nav)));
                                    investorSchemeWiseTransactionTaxReport.setPurchase_nav(Double.parseDouble(four_decimal.format(purchase_original_nav)));
                                    investorSchemeWiseTransactionTaxReport.setPurchase_transaction_type(purchase_transaction_type);
                                    investorSchemeWiseTransactionTaxReport.setRedeemed_units(sold_units);
                                    investorSchemeWiseTransactionTaxReport.setRedeemed_nav(price);
                                    investorSchemeWiseTransactionTaxReport.setActual_amount(actual_amount);
                                    investorSchemeWiseTransactionTaxReport.setGrandfathered_cost_apply(grand_father_cost_apply);

                                    applyGrandfathering(investorSchemeWiseTransactionTaxReport, category, purchase_date,
                                            grandfathered_date, sold_units, grandfathered_units, grandfathered_nav);

                                    investorSchemeWiseTransactionTaxReport.setNo_of_days(no_of_days);

                                    applyTaxSlab(investorSchemeWiseTransactionTaxReport, category, hybrid_taxation,
                                            no_of_days, purchase_date, traddate, reliased_gain, new_tax_slap_date,
                                            debt_fund_checking_date, debt_fund_index_stop_date, purchase_nav,
                                            sold_nav, sold_units, sold_nav);

                                    investorSchemeWiseTransactionTaxReportList.add(investorSchemeWiseTransactionTaxReport);
                                }
                                break;
                            } else {
                                purchase_date = xirrRes.getTrxn_date();
                                purchase_units = xirrRes.getUnits();
                                purchase_amount = xirrRes.getAmount();
                                purchase_nav = xirrRes.getNav();
                                purchase_transaction_type = xirrRes.getTrxn_type();
                                pur_tr_charges = xirrRes.getTr_charges();
                                stamp_duty_value = xirrRes.getStamp_duty();
                                Double purchase_original_nav = purchase_nav;
                                Double pur_amt = purchase_amount + stamp_duty_value + pur_tr_charges;
                                Double purchase_new_nav = (pur_amt / purchase_units);
                                if (!segregated_flag && purchase_new_nav > purchase_nav) {
                                    purchase_nav = purchase_new_nav;
                                }
                                Date sold_date = karvySchemeWiseInvestorTransactions.get(i).getTransaction_date();
                                no_of_days = TrackerUtils.getDaysBetweenDates(purchase_date, sold_date);
                                Double sold_nav = karvySchemeWiseInvestorTransactions.get(i).getPrice();
                                Double current_units = xirrRes.getCheck_units();
                                sold_units = sold_units - current_units;
                                sold_units = Double.parseDouble(four_decimal.format(sold_units));

                                if (price == 0) {
                                    if (product_code.equalsIgnoreCase("RMF01GP") || product_code.equalsIgnoreCase("108COSG") || product_code.equalsIgnoreCase("RMFMIMP")) {
                                        // no-op
                                    } else {
                                        no_of_days = 0;
                                    }
                                }

                                if (category.equalsIgnoreCase("Equity") && grandfathered_nav != 0) {
                                    if (no_of_days > 365 && purchase_date.compareTo(grandfathered_date) < 0) {
                                        if (sold_nav > grandfathered_nav && purchase_nav < grandfathered_nav) {
                                            grand_father_cost_apply = true;
                                            reliased_gain = (sold_nav - grandfathered_nav) * current_units;
                                            reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                        } else if (sold_nav > purchase_nav && purchase_nav > grandfathered_nav) {
                                            reliased_gain = (sold_nav - purchase_nav) * current_units;
                                            reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                        } else {
                                            if (sold_nav > purchase_nav) {
                                                reliased_gain = 0.0;
                                            } else {
                                                reliased_gain = (sold_nav - purchase_nav) * current_units;
                                                reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                            }
                                        }
                                    } else {
                                        reliased_gain = (sold_nav - purchase_nav) * current_units;
                                        reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                    }
                                } else {
                                    reliased_gain = (sold_nav - purchase_nav) * current_units;
                                    reliased_gain = Double.parseDouble(decimal.format(reliased_gain));
                                }
                                xirrRes.setCheck_units(0.0);

                                Double actual_amount = (current_units * purchase_nav) + ((stamp_duty_value / purchase_units) * current_units);
                                actual_amount = Double.parseDouble(decimal.format(actual_amount));

                                if (sold_date.compareTo(financial_year_start_date) >= 0) {
                                    InvestorSchemeWiseTransactionTaxReport investorSchemeWiseTransactionTaxReport = buildBaseReport(
                                            scheme, hybrid_taxation, product_code, scheme_amfi_short_name, folio_number,
                                            isin_no, category);

                                    if (flag) {
                                        populateSoldFields(investorSchemeWiseTransactionTaxReport, traddate, units,
                                                amount, price, trxn_type, sold_tax, stt);
                                        flag = false;
                                    } else {
                                        populateEmptySoldFields(investorSchemeWiseTransactionTaxReport, traddate, false);
                                    }

                                    investorSchemeWiseTransactionTaxReport.setPurchase_date(purchase_date);
                                    investorSchemeWiseTransactionTaxReport.setPurchase_units(current_units);
                                    investorSchemeWiseTransactionTaxReport.setPurchase_amount(Double.parseDouble(two_decimal.format(current_units * purchase_nav)));
                                    investorSchemeWiseTransactionTaxReport.setPurchase_nav(Double.parseDouble(four_decimal.format(purchase_original_nav)));
                                    investorSchemeWiseTransactionTaxReport.setPurchase_transaction_type(purchase_transaction_type);
                                    investorSchemeWiseTransactionTaxReport.setRedeemed_units(current_units);
                                    investorSchemeWiseTransactionTaxReport.setRedeemed_nav(price);
                                    investorSchemeWiseTransactionTaxReport.setActual_amount(actual_amount);
                                    investorSchemeWiseTransactionTaxReport.setGrandfathered_cost_apply(grand_father_cost_apply);

                                    applyGrandfathering(investorSchemeWiseTransactionTaxReport, category, purchase_date,
                                            grandfathered_date, current_units, grandfathered_units, grandfathered_nav);

                                    investorSchemeWiseTransactionTaxReport.setNo_of_days(no_of_days);

                                    applyTaxSlab(investorSchemeWiseTransactionTaxReport, category, hybrid_taxation,
                                            no_of_days, purchase_date, traddate, reliased_gain, new_tax_slap_date,
                                            debt_fund_checking_date, debt_fund_index_stop_date, purchase_nav,
                                            sold_nav, current_units, sold_nav);

                                    investorSchemeWiseTransactionTaxReportList.add(investorSchemeWiseTransactionTaxReport);
                                }
                            }
                        } else {
                            // New code start here - when purchase_units is 0 but we still have sold_units
                            if (sold_units > 0) {
                                continue;
                            }
                            // New code end here
                        }
                    }
                }
            }
        }
    }
}
