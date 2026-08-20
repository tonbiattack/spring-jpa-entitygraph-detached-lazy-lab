package jp.tonbiattack.debuglab.receipt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class EntityGraphInitializationObservationTest {

    @Autowired
    private ReceiptRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void detailedEntityGraphInitializesLines_butRegularFindByIdDoesNot() {
        Long receiptId = transactionTemplate.execute(status -> {
            Receipt receipt = new Receipt("receipt-observation-001");
            receipt.addLine("Green tea");
            return repository.saveAndFlush(receipt).getId();
        });

        boolean regularFindInitialized = transactionTemplate.execute(status ->
                Hibernate.isInitialized(repository.findById(receiptId).orElseThrow().getLines())
        );
        boolean entityGraphFindInitialized = transactionTemplate.execute(status ->
                Hibernate.isInitialized(repository.findDetailedById(receiptId).orElseThrow().getLines())
        );

        assertAll(
                () -> assertFalse(regularFindInitialized,
                        "通常のfindByIdはLAZYな明細コレクションを初期化しない"),
                () -> assertTrue(entityGraphFindInitialized,
                        "EntityGraph付きの詳細取得は明細コレクションを初期化する")
        );
    }
}
