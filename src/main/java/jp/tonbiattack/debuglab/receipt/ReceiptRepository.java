package jp.tonbiattack.debuglab.receipt;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    @EntityGraph(attributePaths = "lines")
    @Query("select receipt from Receipt receipt where receipt.id = :id")
    Optional<Receipt> findDetailedById(@Param("id") Long id);
}
