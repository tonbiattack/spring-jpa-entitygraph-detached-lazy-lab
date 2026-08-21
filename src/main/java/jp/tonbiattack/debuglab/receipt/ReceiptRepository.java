package jp.tonbiattack.debuglab.receipt;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    @EntityGraph(attributePaths = "lines")
    @Query("select receipt from Receipt receipt where receipt.id = :id")
    /**
     * 受付と明細を同時に取得します。
     *
     * <p>{@code lines} は通常 LAZY ですが、EntityGraph によりこの検索中に初期化されます。
     * そのため、トランザクション終了後にエンティティがデタッチされた状態でも、明細を参照できます。</p>
     */
    Optional<Receipt> findDetailedById(@Param("id") Long id);
}
