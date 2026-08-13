package com.hexagonal.portfolio.application.service;
import com.hexagonal.portfolio.application.port.in.GetFolioMasterSummaryUseCase;
import com.hexagonal.portfolio.application.port.out.LoadCamsFolioMasterPort;
import com.hexagonal.portfolio.application.port.out.LoadKarvyFolioMasterPort;
import com.hexagonal.portfolio.domain.model.FolioMasterResponse;
import com.hexagonal.portfolio.domain.model.FolioMasterSummary;
import com.hexagonal.portfolio.domain.model.FolioMasterSummaryResponse;
import com.hexagonal.portfolio.domain.model.InvestorMasterCams;
import com.hexagonal.portfolio.domain.model.InvestorMasterKarvy;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWisePortfolioResponse;
import com.hexagonal.portfolio.domain.service.MyMFBoxUtils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolioMasterSummaryService implements GetFolioMasterSummaryUseCase {

    @Autowired
    private LoadCamsFolioMasterPort loadCamsFolioMasterPort;

    @Autowired
    private LoadKarvyFolioMasterPort loadKarvyFolioMasterPort;

    @Value("${amc.logo.url}")
    private String amcLogoPath;

    public List<FolioMasterResponse> getSchemeMaster(Integer userId, String clientName) {

        List<FolioMasterResponse> responseList = new ArrayList<>();

        try {

            // 🔹 CAMS DATA
            List<InvestorMasterCams> camsList =
                    loadCamsFolioMasterPort.findData(userId, clientName);

            for (InvestorMasterCams cams : camsList) {

                FolioMasterResponse res = new FolioMasterResponse();
                res.setFolio_number(cams.getFoliochk());
                res.setScheme(cams.getSch_name());
                res.setProduct_code(cams.getProduct());
                res.setBroker_code(cams.getBroker_cod());

                res.setHoldername(cams.getInv_name());
                res.setHolderpan(cams.getPan_no());
                res.setFirstjointholdername(cams.getJnt_name1());
                res.setFirstjointholderpan(cams.getJoint1_pan());
                res.setSecondjointholdername(cams.getJnt_name2());
                res.setSecondjointholderpan(cams.getJoint2_pan());

                res.setDob(formatDate(cams.getInv_dob()));
                res.setMod_of_holding(cams.getHolding_na());
                res.setAddress(buildAddress(cams.getAddress1(), cams.getAddress2(), cams.getAddress3()));
                res.setCity(cams.getCity());
                res.setPincode(cams.getPincode());
                res.setMobile(cams.getMobile_no());
                res.setPhone_off(cams.getPhone_off());
                res.setPhone_res(cams.getPhone_res());
                res.setEmail(cams.getEmail());

                res.setBank(cams.getBank_name());
                res.setBranch(cams.getBranch());
                res.setAccount_type(cams.getAc_type());
                res.setBankCity(cams.getB_city());
                res.setAcnumber(cams.getAc_no());
                res.setIfsc(cams.getIfsc_code());

                res.setGuard_name(cams.getGuard_name());
                res.setGuard_pan(cams.getGuard_pan());
                res.setTax_status(cams.getTax_status());
                res.setOccupation(cams.getOccupation());
                res.setUpdated_date(formatDate(cams.getRep_date()));

                // Nominee 1
                res.setNom1_name(cams.getNom_name());
                res.setNom1_relation(cams.getRelation());
                res.setNom1_addr1(cams.getNom_addr1());
                res.setNom1_addr2(cams.getNom_addr2());
                res.setNom1_addr3(cams.getNom_addr3());
                res.setNom1_city(cams.getNom_city());
                res.setNom1_state(cams.getNom_state());
                res.setNom1_pincod(cams.getNom_pincod());
                res.setNom1_ph_off(cams.getNom_ph_off());
                res.setNom1_ph_res(cams.getNom_ph_res());
                res.setNom1_email(cams.getNom_email());
                res.setNom1_percen(toStr(cams.getNom_percen()));

                // Nominee 2
                res.setNom2_name(cams.getNom2_name());
                res.setNom2_relation(cams.getNom2_relat());
                res.setNom2_addr1(cams.getNom2_addr1());
                res.setNom2_addr2(cams.getNom2_addr2());
                res.setNom2_addr3(cams.getNom2_addr3());
                res.setNom2_city(cams.getNom2_city());
                res.setNom2_state(cams.getNom2_state());
                res.setNom2_pincod(cams.getNom2_pinco());
                res.setNom2_ph_off(cams.getNom2_ph_of());
                res.setNom2_ph_res(cams.getNom2_ph_re());
                res.setNom2_email(cams.getNom2_email());
                res.setNom2_percen(toStr(cams.getNom2_perce()));

                // Nominee 3
                res.setNom3_name(cams.getNom3_name());
                res.setNom3_relation(cams.getNom3_relat());
                res.setNom3_addr1(cams.getNom3_addr1());
                res.setNom3_addr2(cams.getNom3_addr2());
                res.setNom3_addr3(cams.getNom3_addr3());
                res.setNom3_city(cams.getNom3_city());
                res.setNom3_state(cams.getNom3_state());
                res.setNom3_pincod(cams.getNom3_pinco());
                res.setNom3_ph_off(cams.getNom3_ph_of());
                res.setNom3_ph_res(cams.getNom3_ph_re());
                res.setNom3_email(cams.getNom3_email());
                res.setNom3_percen(toStr(cams.getNom3_perce()));

                responseList.add(res);
            }

            // 🔹 KARVY DATA
            List<InvestorMasterKarvy> karvyList =
                    loadKarvyFolioMasterPort.findByUserIdAndClientNameOrderByIdDesc(userId, clientName);

            for (InvestorMasterKarvy karvy : karvyList) {

                FolioMasterResponse res = new FolioMasterResponse();
                res.setFolio_number(karvy.getFolio());
                res.setScheme(karvy.getFund_description());
                res.setProduct_code(karvy.getProduct_code());
                res.setBroker_code(karvy.getBroker_code());

                res.setHoldername(karvy.getInvestor_name());
                res.setHolderpan(karvy.getPan_number());
                res.setFirstjointholdername(karvy.getJoint_name1());
                res.setFirstjointholderpan(karvy.getPan2());
                res.setSecondjointholdername(karvy.getJoint_name2());
                res.setSecondjointholderpan(karvy.getPan3());

                res.setDob(karvy.getDate_of_birth());
                res.setMod_of_holding(karvy.getMode_of_holding_description());
                res.setAddress(buildAddress(karvy.getAddress1(), karvy.getAddress2(), karvy.getAddress3()));
                res.setCity(karvy.getCity());
                res.setPincode(karvy.getPincode());
                res.setMobile(karvy.getMobile());
                res.setPhone_off(karvy.getPhone_office());
                res.setPhone_res(karvy.getPhone_residence());
                res.setEmail(karvy.getEmail());

                res.setBank(karvy.getBank_name());
                res.setBranch(karvy.getBranch());
                res.setAccount_type(karvy.getAccount_type());
                res.setBankCity(karvy.getBank_city());
                res.setAcnumber(karvy.getBankaccno());
                res.setIfsc(karvy.getIfsc());

                res.setGuard_name(karvy.getGuard_name());
                res.setGuard_pan(karvy.getGuardian_pan());
                res.setTax_status(karvy.getTax_status());
                res.setOccupation(karvy.getOccupation_description());
                res.setUpdated_date(karvy.getLast_update());

                // Nominee 1
                res.setNom1_name(karvy.getNominee());
                res.setNom1_relation(karvy.getNominee_rel());

                // Nominee 2
                res.setNom2_name(karvy.getNominee2());
                res.setNom2_relation(karvy.getNominee2_r3());

                // Nominee 3
                res.setNom3_name(karvy.getNominee3());
                res.setNom3_relation(karvy.getNominee3_r4());

                responseList.add(res);
            }

        } catch (Exception ex) {
            System.out.println("Exception Date & Time = " + new Date());
            ex.printStackTrace();
            throw new RuntimeException(
                    "Failed to getSchemeMaster --> " + ex.getMessage(),
                    ex
            );
        }

        return responseList;
    }

    /**
     * Builds the scheme-wise folio master summary consumed by
     * {@code /investor/getInvestorPortfolioNew?report_type=folio}.
     *
     * @param portfolio  the computed portfolio (from the investor-portfolio use case)
     * @param userId     investor user id
     * @param clientName mfd client name
     */
    @Override
    public FolioMasterSummaryResponse buildFolioMasterSummary(
            InvestorPortfolioResponse portfolio, Integer userId, String clientName) {

        FolioMasterSummaryResponse response = new FolioMasterSummaryResponse();
        response.setStatus(200);
        response.setStatus_msg("Success");
        response.setMsg("Success");

        List<FolioMasterSummary> summaryList = new ArrayList<>();

        if (portfolio == null) {
            response.setTotal_current_value(0.0);
            response.setFolio_master_summary(summaryList);
            return response;
        }

        response.setTotal_current_value(portfolio.getTotalCurrentValue());

        List<InvestorSchemeWisePortfolioResponse> schemes =
                portfolio.getInvestorSchemeWisePortfolioResponses();

        if (schemes == null || schemes.isEmpty()) {
            response.setFolio_master_summary(summaryList);
            return response;
        }

        List<FolioMasterResponse> masterList = getSchemeMaster(userId, clientName);

        for (InvestorSchemeWisePortfolioResponse sch : schemes) {

            FolioMasterSummary item = new FolioMasterSummary();
            item.setScheme(sch.getScheme());
            item.setScheme_amfi_short_name(sch.getScheme_amfi_short_name());
            item.setAmc_logo(amcLogoPath + MyMFBoxUtils.getLogoByAmcNameOrSchemeName(sch.getScheme()));

            item.setUnits(sch.getTotalUnits());
            item.setCurrent_value(sch.getTotalCurrentValue());
            item.setFolio_no(sch.getFoliono());

            List<FolioMasterResponse> details = masterList.stream()
                    .filter(m -> m.getFolio_number() != null
                            && m.getProduct_code() != null
                            && m.getFolio_number().equalsIgnoreCase(sch.getFoliono())
                            && m.getProduct_code().equalsIgnoreCase(sch.getScheme_code()))
                    .collect(Collectors.toList());

            // holding_nature is never populated on the portfolio scheme, so derive the
            // mode of holding from the matched CAMS/Karvy master row instead.
            String modOfHolding = details.stream()
                    .map(FolioMasterResponse::getMod_of_holding)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst()
                    .orElse("");
            item.setMod_of_holding(modOfHolding);

            item.setFolio_master_details(details);
            summaryList.add(item);
        }

        response.setFolio_master_summary(summaryList);
        return response;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd-MM-yyyy").format(date);
    }

    private String buildAddress(String a1, String a2, String a3) {
        return nvl(a1) + "," + nvl(a2) + "," + nvl(a3);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String toStr(Double value) {
        return value == null ? "" : String.valueOf(value);
    }
}
