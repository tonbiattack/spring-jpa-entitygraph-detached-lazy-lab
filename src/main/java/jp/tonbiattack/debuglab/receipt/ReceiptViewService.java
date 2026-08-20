package jp.tonbiattack.debuglab.receipt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptViewService {

    private final ReceiptRepository repository;

    public ReceiptViewService(ReceiptRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Receipt loadReceiptForView(Long receiptId) {
        return repository.findDetailedById(receiptId).orElseThrow();
    }
}
