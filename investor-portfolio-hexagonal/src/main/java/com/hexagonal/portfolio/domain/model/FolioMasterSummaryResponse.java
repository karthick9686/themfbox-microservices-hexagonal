package com.hexagonal.portfolio.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class FolioMasterSummaryResponse {

    private int status;
    private String status_msg;
    private String msg;
    private Double total_current_value;
    private List<FolioMasterSummary> folio_master_summary;
}
