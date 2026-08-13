package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorTransactionKarvy;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorTransactionKarvyRepository extends JpaRepository<InvestorTransactionKarvy, Long> {

    @Query("FROM InvestorTransactionKarvy t WHERE t.user_id = :userId AND t.client_name = :clientName " +
            "AND t.transaction_date <= :endDate " +
            "ORDER BY t.transaction_date ASC, t.transaction_type ASC, t.units DESC")
    List<InvestorTransactionKarvy> findAllByUserAndDateNew(@Param("userId") Integer userId,
                                                        @Param("clientName") String clientName,
                                                        @Param("endDate") Date endDate);

    @Query("""
            SELECT k.folio_number, k.fund, k.scheme_code, k.fund_description
            FROM InvestorTransactionKarvy k
            WHERE k.user_id               = :userId
              AND k.transaction_description IN (:trxnTypes)
              AND k.transaction_date      <= :endDate
              AND k.client_name            = :clientName
            GROUP BY k.folio_number, k.fund, k.scheme_code
            """)
    List<Object[]> findDistinctSchemes(
            @Param("userId")     Integer userId,
            @Param("trxnTypes")  List<String> trxnTypes,
            @Param("endDate")    Date endDate,
            @Param("clientName") String clientName
    );

    @Query("FROM InvestorTransactionKarvy t " +
            "WHERE t.user_id = :userId " +
            "AND t.folio_number = :folioNumber " +
            "AND CONCAT(t.fund, t.scheme_code) = :productCode " +
            "AND t.transaction_date <= :endDate " +
            "AND t.client_name = :clientName " +
            "ORDER BY t.transaction_date ASC, t.purchase_date1 ASC, t.units DESC")
    List<InvestorTransactionKarvy> findSchemeTransactions(
            @Param("userId")      Integer userId,
            @Param("folioNumber") String  folioNumber,
            @Param("productCode") String  productCode,
            @Param("endDate")     Date    endDate,
            @Param("clientName")  String  clientName);

    @Query("SELECT k FROM InvestorTransactionKarvy k WHERE k.user_id = :user_id AND k.folio_number = :folio_number AND CONCAT(k.fund, k.scheme_code) = :product_code AND k.transaction_date <= :financial_year_end_date AND k.client_name = :client_name")
    List<InvestorTransactionKarvy> findSegregatedTransactionsNew(
            @Param("user_id") Integer user_id,
            @Param("folio_number") String folio_number,
            @Param("product_code") String product_code,
            @Param("financial_year_end_date") Date financial_year_end_date,
            @Param("client_name") String client_name
    );
}
