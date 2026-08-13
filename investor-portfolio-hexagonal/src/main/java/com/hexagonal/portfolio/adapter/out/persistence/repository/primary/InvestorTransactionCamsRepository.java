package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorTransactionCams;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorTransactionCamsRepository extends JpaRepository<InvestorTransactionCams, Long> {

    @Query("FROM InvestorTransactionCams WHERE user_id = :userId AND client_name = :clientName " +
            "AND traddate <= :cutoffDate ORDER BY traddate ASC, trxnno ASC, units DESC")
    List<InvestorTransactionCams> findAllByUserAndDate(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName,
            @Param("cutoffDate") Date cutoffDate);

    @Query("""
            SELECT c.folio_no, c.prodcode, c.scheme
            FROM InvestorTransactionCams c
            WHERE c.user_id   = :userId
              AND c.trxn_type_ IN (:trxnTypes)
              AND c.traddate  <= :endDate
              AND c.client_name = :clientName
            GROUP BY c.folio_no, c.prodcode
            """)
    List<Object[]> findDistinctSchemes(
            @Param("userId")     Integer userId,
            @Param("trxnTypes")  List<String> trxnTypes,
            @Param("endDate")    Date endDate,
            @Param("clientName") String clientName
    );

    @Query("from InvestorTransactionCams t where t.user_id = :userId and t.folio_no = :folioNo " +
            "and t.prodcode = :prodcode and t.traddate <= :endDate and t.client_name = :clientName " +
            "order by t.traddate asc, t.purprice asc, t.units desc")
    List<InvestorTransactionCams> findSchemeTransactionsPriceAsc(@Param("userId") Integer userId,
                                                                 @Param("folioNo") String folioNo,
                                                                 @Param("prodcode") String prodcode,
                                                                 @Param("endDate") Date endDate,
                                                                 @Param("clientName") String clientName);

    @Query("from InvestorTransactionCams t where t.user_id = :userId and t.folio_no = :folioNo " +
            "and t.prodcode = :prodcode and t.traddate <= :endDate and t.client_name = :clientName " +
            "order by t.traddate asc, t.purprice desc, t.units desc")
    List<InvestorTransactionCams> findSchemeTransactionsPriceDesc(@Param("userId") Integer userId,
                                                                  @Param("folioNo") String folioNo,
                                                                  @Param("prodcode") String prodcode,
                                                                  @Param("endDate") Date endDate,
                                                                  @Param("clientName") String clientName);
}
