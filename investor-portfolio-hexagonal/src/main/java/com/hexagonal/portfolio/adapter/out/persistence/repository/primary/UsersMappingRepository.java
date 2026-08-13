package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.UsersMapping;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersMappingRepository extends JpaRepository<UsersMapping, Integer> {

    @Query("""
    SELECT u
    FROM UsersMapping u
    WHERE u.user_id = :userId
    AND u.client_name = :clientName
    """)
    List<UsersMapping> getFamilyMappedInvestor(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );
}
