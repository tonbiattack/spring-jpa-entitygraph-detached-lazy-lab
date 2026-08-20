package jp.tonbiattack.debuglab.receipt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ReceiptViewServiceTest {

    @Autowired
    private ReceiptRepository repository;

    @Autowired
    private ReceiptViewService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void loadsLineItemsIntoView_afterServiceTransactionHasCompleted() {
        Long receiptId = transactionTemplate.execute(status -> {
            Receipt receipt = new Receipt("receipt-001");
            receipt.addLine("Green tea");
            return repository.saveAndFlush(receipt).getId();
        });
        List<String> persistedLineNames = transactionTemplate.execute(status ->
                repository.findDetailedById(receiptId).orElseThrow().getLines().stream()
                        .map(ReceiptLine::getItemName)
                        .toList()
        );

        Receipt detachedReceipt = service.loadReceiptForView(receiptId);

        assertAll(
                () -> assertEquals(List.of("Green tea"), persistedLineNames,
                        "DBには受領書の明細行が保存されている"),
                () -> assertEquals(new ReceiptView("receipt-001", List.of("Green tea")),
                        assertDoesNotThrow(() -> ReceiptView.from(detachedReceipt),
                                "サービスのトランザクション完了後でも明細をDTOへ変換できる"),
                        "詳細DTOは見出しと保存済みの明細名を返す")
        );
    }
}
