package jp.tonbiattack.debuglab.receipt;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    /**
        * 受付と、その受付に紐づく {@link ReceiptLine} の一覧を同時に取得します。
     *
        * <p>{@code attributePaths} に指定する {@code "lines"} は任意の変数名ではなく、
        * {@link Receipt} エンティティの {@code lines} プロパティ名です。
        * {@code lines} は {@code @OneToMany(fetch = FetchType.LAZY)} のため通常は後から読み込まれますが、
        * {@code @EntityGraph} によりこの検索時に読み込まれます。
        * そのため、トランザクション終了後にデタッチされた {@link Receipt} からも明細を参照できます。</p>
     */
        // Receipt.lines を検索結果に含め、ReceiptLine の一覧を先に読み込む
    @EntityGraph(attributePaths = "lines")
    @Query("select receipt from Receipt receipt where receipt.id = :id")
    Optional<Receipt> findDetailedById(@Param("id") Long id);
}
