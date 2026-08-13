package com.hexagonal.portfolio.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class FolioMasterSummary {

    private String scheme;
    private String scheme_amfi_short_name;
    private String amc_logo;
    private Double units;
    private Double current_value;
    private String mod_of_holding;
    private String folio_no;

    private List<FolioMasterResponse> folio_master_details;
}
