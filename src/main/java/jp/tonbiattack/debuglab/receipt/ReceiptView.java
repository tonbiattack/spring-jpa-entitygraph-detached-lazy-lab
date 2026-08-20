package jp.tonbiattack.debuglab.receipt;

import java.util.List;

public record ReceiptView(String receiptCode, List<String> lineItemNames) {

    public static ReceiptView from(Receipt receipt) {
        return new ReceiptView(
                receipt.getReceiptCode(),
                receipt.getLines().stream().map(ReceiptLine::getItemName).toList()
        );
    }
}
