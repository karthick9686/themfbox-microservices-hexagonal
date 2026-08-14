package com.hexagonal.portfolio.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Portfolio domain rules carried over verbatim from the legacy {@code MfboxUtils}.
 * Only the members reachable from the investor-portfolio use case are retained.
 */
@Slf4j
public class MfboxUtils
{

    public static String checkParem(String param)
    {
        if (param == null || param.trim().equalsIgnoreCase("null") || param.trim().equalsIgnoreCase("undefined"))
        {
            return "";
        }

        return param.trim();
    }

    public static boolean getSameSchemeMultipleArnAdvisors(String client_name)
    {
        boolean flag = false;
        HashMap<String,String> advisor_map = new HashMap<String,String>();
        advisor_map.put("mutualfundsaving","mutualfundsaving");
        advisor_map.put("bijuyohannan","bijuyohannan");

        String client = advisor_map.get(client_name);
        if(client != null) {
            flag = true;
        }
        return flag;
    }

    public static String segregatedSchemes(String scheme_code)
    {
        String segre_percent = "";
        try
        {
            HashMap<String,String> segregated_map = new HashMap<String,String>();
            segregated_map.put("RMFCPGP","1.45"); //NIPPON INDIA ULTRA SHORT DURATION FUND
            segregated_map.put("RMFCPDP","1.45"); //NIPPON INDIA ULTRA SHORT DURATION FUND
            segregated_map.put("RMFCPQP","1.45"); //NIPPON INDIA ULTRA SHORT DURATION FUND
            segregated_map.put("RMFCPMP","1.45"); //NIPPON INDIA ULTRA SHORT DURATION FUND
            segregated_map.put("RMFCPDD","1.45"); //NIPPON INDIA ULTRA SHORT DURATION FUND

            segregated_map.put("RMFSHGP","0.10"); //NIPPON INDIA EQUITY HYBRID FUND
            segregated_map.put("RMFSHDP","0.10"); //NIPPON INDIA EQUITY HYBRID FUND
            segregated_map.put("RMFSHMP","0.10"); //NIPPON INDIA EQUITY HYBRID FUND
            segregated_map.put("RMFSHQP","0.10"); //NIPPON INDIA EQUITY HYBRID FUND
            segregated_map.put("RMFSHDD","0.10"); //NIPPON INDIA EQUITY HYBRID FUND

            segregated_map.put("RMFMIGP","3.33"); //NIPPON INDIA HYBRID BOND FUND //ALPPR7506Q EF this pan nav not reduced in tax report
            segregated_map.put("RMFMIDP","3.33"); //NIPPON INDIA HYBRID BOND FUND
            segregated_map.put("RMFMIMP","3.33"); //NIPPON INDIA HYBRID BOND FUND
            segregated_map.put("RMFMIQP","3.33"); //NIPPON INDIA HYBRID BOND FUND
            segregated_map.put("RMFMIDD","3.33"); //NIPPON INDIA HYBRID BOND FUND

            /*segregated_map.put("RMFCBGP","0.37"); //NIPPON INDIA STRATEGIC DEBT FUND //AXQPS2410K EF this pan nav not reduced in tax report
            segregated_map.put("RMFCBDP","0.37"); //NIPPON INDIA STRATEGIC DEBT FUND
            segregated_map.put("RMFCBMP","0.37"); //NIPPON INDIA STRATEGIC DEBT FUND
            segregated_map.put("RMFCBQP","0.37"); //NIPPON INDIA STRATEGIC DEBT FUND
            segregated_map.put("RMFCBDD","0.37"); //NIPPON INDIA STRATEGIC DEBT FUND*/

            segregated_map.put("RMFSDGP","0.56"); //NIPPON INDIA CREDIT RISK FUND
            segregated_map.put("RMFSDDP","0.56"); //NIPPON INDIA CREDIT RISK FUND
            segregated_map.put("RMFSDDR","0.56"); //NIPPON INDIA CREDIT RISK FUND
            segregated_map.put("RMFSDMP","0.56"); //NIPPON INDIA CREDIT RISK FUND
            segregated_map.put("RMFSDQP","0.56"); //NIPPON INDIA CREDIT RISK FUND
            segregated_map.put("RMFSDDD","0.56"); //NIPPON INDIA CREDIT RISK FUND

            segregated_map.put("RMFESGP","1.45"); //NIPPON INDIA EQUITY SAVINGS FUND
            segregated_map.put("RMFESDP","1.45"); //NIPPON INDIA EQUITY SAVINGS FUND
            segregated_map.put("RMFESMP","1.45"); //NIPPON INDIA EQUITY SAVINGS FUND
            segregated_map.put("RMFESQP","1.45"); //NIPPON INDIA EQUITY SAVINGS FUND
            segregated_map.put("RMFESDD","1.45"); //NIPPON INDIA EQUITY SAVINGS FUND

            segregated_map.put("B321G","5.62"); //Aditya Birla Sun Life Dynamic Bond Fund-Growth-Regular Plan
            segregated_map.put("B321MD","5.62"); //Aditya Birla Sun Life Dynamic Bond Fund-Monthly Dividend-Regular Plan
            segregated_map.put("B321QD","5.62"); //Aditya Birla Sun Life Dynamic Bond Fund-Quarterly Dividend-Regular Plan
            segregated_map.put("B321A","5.62"); //Aditya Birla Sun Life Dynamic Bond Fund - Dividend-Regular Plan

            segregated_map.put("B303G","7.54"); //Aditya Birla Sun Life Medium Term Plan - Growth-Regular Plan - //7.53992
            segregated_map.put("B303MD","7.54"); //Aditya Birla Sun Life Medium Term Plan
            segregated_map.put("B303FD","7.54"); //Aditya Birla Sun Life Medium Term Plan
            segregated_map.put("B303WD","7.54"); //Aditya Birla Sun Life Medium Term Plan

            segregated_map.put("B380B","3.69"); //Aditya Birla Sun Life Credit Risk Fund - Gr. REGULAR
            segregated_map.put("B380A","3.69"); //Aditya Birla Sun Life Credit Risk Fund

            segregated_map.put("TFG1","1.6548"); //Tata Treasury Advantage Fund Regular Plan - Growth
            segregated_map.put("TFD1","1.6548"); //Tata Treasury Advantage Fund Regular Plan - Growth
            segregated_map.put("TFW1","1.6548"); //Tata Treasury Advantage Fund Regular Plan - Growth
            segregated_map.put("TFWS","73.3877"); //Tata Treasury Advantage Fund Regular Plan - Weekly Dividend  Segregated Portfolio 1
            //segregated_map.put("TFG1S","74.8608"); //Tata Treasury Advantage Fund Regular Plan - Growth  Segregated Portfolio 1

            segregated_map.put("TIAG","5.1525"); //Tata Medium Term Fund Regular Plan - Growth

            segregated_map.put("TTMHG","15.0153"); //Tata Corporate Bond Fund Regular Plan - Growth
            segregated_map.put("TTMHD","15.0153"); //Tata Corporate Bond Fund
            segregated_map.put("TTMHM","15.0153"); //Tata Corporate Bond Fund
            segregated_map.put("TTMHW","15.0153"); //Tata Corporate Bond Fund
            //segregated_map.put("TTMHGS","75.0208"); //Tata Corporate Bond Fund Regular Plan - Growth  Segregated Portfolio 1

            //segregated_map.put("FTI104","1.75"); //Franklin India Short Term Income Plan
            segregated_map.put("FTI105","1.75"); //Franklin India Short Term Income Plan
            segregated_map.put("FTI106","1.75"); //Franklin India Short Term Income Plan -  Retail Plan
            segregated_map.put("FTI107","1.75"); //Franklin India Short Term Income Plan

            segregated_map.put("FTI155","0.91"); //Franklin India Dynamic Accrual Fund
            segregated_map.put("FTI156","0.91"); //Franklin India Dynamic Accrual Fund

            segregated_map.put("FTI406","1.55"); //Franklin India Credit Risk Fund
            segregated_map.put("FTI405","1.55"); //Franklin India Credit Risk Fund
            segregated_map.put("FTI405P","1.55"); //Franklin India Credit Risk Fund

            segregated_map.put("FTI063","1.75"); //Franklin India Debt Hybrid Fund - Plan A
            segregated_map.put("FTI064","1.75"); //Franklin India Debt Hybrid Fund - Plan A
            segregated_map.put("FTI065","1.75"); //Franklin India Debt Hybrid Fund - Plan A

            segregated_map.put("FTI053","1.75"); //Franklin India Debt Hybrid Fund - Plan B
            segregated_map.put("FTI054","1.75"); //Franklin India Debt Hybrid Fund - Plan B
            segregated_map.put("FTI055","1.75"); //Franklin India Debt Hybrid Fund - Plan B

            segregated_map.put("10803GP","2.96"); //UTI Bond Fund (Segregated - 17022020) - Regular Growth Plan
            segregated_map.put("10803DP","2.96"); //UTI Bond Fund (Segregated - 17022020) - Regular Plan
            //UTI Bond Fund - Regular Growth Plan, 108BNGP
            //UTI-BOND FUND - DIVIDEND PLAN, 108BNDP

            segregated_map.put("10805GP","1.961"); //UTI Regular Saving Fund (Segregated - 17022020) - Regular Growth Plan
            segregated_map.put("10805MD","1.961"); //UTI Regular Saving Fund (Segregated - 17022020) - Regular Monthly Dividend Plan
            segregated_map.put("10805FD","1.961"); //UTI Regular Saving Fund (Segregated - 17022020) - Regular Flexi Dividend Plan
            //'UTI REGULAR SAVINGS FUND - FLEXI DIVIDEND PLAN', '108MIFD'
            //'UTI REGULAR SAVINGS FUND - MONTHLY DIVIDEND PLAN', '108MIMD'
            //'UTI Regular Savings Fund - Regular Growth Plan', '108MIGP'

            segregated_map.put("10804GP","1.355"); //UTI Dynamic Bond Fund (Segregated - 17022020) - Regular Growth Plan
            //UTI Dynamic Bond Fund - Regular Growth Plan, 108DBGP
            //UTI-Dynamic Bond Fund - QUARTERLY DIVIDEND PLAN, 108DBDP

            segregated_map.put("108COGP","72.9384"); //UTI Credit Risk Fund - Regular Plan

            if(segregated_map.containsKey(scheme_code))
            {
                segre_percent = segregated_map.get(scheme_code);
            }
        }
        catch(Exception ex)
        {
            log.error("Failed to derive segregated scheme percentage", ex);
        }
        return segre_percent;
    }

    public static List<String> KarvyNeutralDividendTransactionType()
    {
        List<String> list = new ArrayList<String>();
        list.add("Gross Dividend");
        list.add("Gross Dividend Rejection");
        list.add("Gross Dividend Rejection Reversal");
        list.add("Dividend Sweep Out");
        list.add("Dividend Sweep Out Rej");
        list.add("Dividend Sweep Out Rej.");
        return list;
    }
}
