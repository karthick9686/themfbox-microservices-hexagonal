package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.client_name = :clientName")
    Optional<User> findByIdAndClientName(
            @Param("id") Integer id,
            @Param("clientName") String clientName);
}
