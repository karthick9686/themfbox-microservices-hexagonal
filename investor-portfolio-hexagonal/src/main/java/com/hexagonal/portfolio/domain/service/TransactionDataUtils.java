package com.hexagonal.portfolio.domain.service;

import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;
import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minus/reversal transaction cancellation rules, carried over verbatim from the legacy
 * {@code TransactionDataUtils}. Only the CAMS and Karvy variants are reachable from the
 * investor-portfolio use case.
 */
public class TransactionDataUtils {

    public static List<InvestorTransactionCams> removeCamsMinusTransaction(List<InvestorTransactionCams> list)
    {
        try
        {


            Double unit = 0.0;

            List<InvestorTransactionCams> units_minus_list = list.stream().filter(trxn -> trxn.getUnits().compareTo(unit) < 0).collect(Collectors.toList());


            for (InvestorTransactionCams cams : units_minus_list)
            {
                List<InvestorTransactionCams> remove_list = new ArrayList<InvestorTransactionCams>();

                Date date = cams.getTraddate();
                Double units = cams.getUnits();
                String transaction_type = cams.getTrxn_type_();

                Double units2 = units * -1;

                if(transaction_type.equalsIgnoreCase("SIP Rejection"))
                {
                    InvestorTransactionCams cams2 = list.stream().filter(trxn -> trxn.getTraddate().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0 && (transaction_type.equalsIgnoreCase("Fresh Purchase Systematic") || transaction_type.equalsIgnoreCase("Additional Purchase Systematic"))).findFirst().orElse(null);
                    if(cams2 == null)
                    {

                    }else
                    {
                        remove_list.add(cams);
                        remove_list.add(cams2);

                        list.removeIf(x -> remove_list.contains(x));


                    }
                }else
                {
                    InvestorTransactionCams cams2 = list.stream().filter(trxn -> trxn.getTraddate().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0 && trxn.getTrxn_type_().equalsIgnoreCase(transaction_type)).findFirst().orElse(null);
                    if(cams2 == null)
                    {

                    }else
                    {
                        remove_list.add(cams);
                        remove_list.add(cams2);

                        list.removeIf(x -> remove_list.contains(x));


                    }
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<InvestorTransactionKarvy> removeKarvyMinusTransaction(List<InvestorTransactionKarvy> list)
    {
        try
        {


            Double unit = 0.0;

            List<InvestorTransactionKarvy> units_minus_list = list.stream().filter(trxn -> trxn.getUnits().compareTo(unit) < 0).collect(Collectors.toList());


            for (InvestorTransactionKarvy karvy : units_minus_list)
            {
                List<InvestorTransactionKarvy> remove_list = new ArrayList<InvestorTransactionKarvy>();

                Date date = karvy.getTransaction_date();
                Double units = karvy.getUnits();
                String transaction_type = karvy.getTransaction_description();

                Double units2 = units * -1;

                InvestorTransactionKarvy karvy2 = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getUnits().compareTo(units2) == 0).findFirst().orElse(null);

                if(karvy2 == null)
                {
                    String ihno = karvy.getIhno();

                    Double total_units = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getIhno().equalsIgnoreCase(ihno) && trxn.getUnits() > 0).mapToDouble(InvestorTransactionKarvy::getUnits).sum();

                    if(total_units.compareTo(units2) == 0) {
                        List<InvestorTransactionKarvy> karvy_sub_list = list.stream().filter(trxn -> trxn.getTransaction_date().compareTo(date) == 0 && trxn.getIhno().equalsIgnoreCase(ihno) && trxn.getUnits() > 0).collect(Collectors.toList());

                        remove_list.add(karvy);
                        remove_list.addAll(karvy_sub_list);

                        list.removeIf(x -> remove_list.contains(x));


                    }

                }else
                {
                    remove_list.add(karvy);
                    remove_list.add(karvy2);

                    list.removeIf(x -> remove_list.contains(x));


                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return list;
    }
}
