package jp.tonbiattack.debuglab.receipt;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String receiptCode;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<ReceiptLine> lines = new ArrayList<>();

    protected Receipt() {
    }

    public Receipt(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public void addLine(String itemName) {
        ReceiptLine line = new ReceiptLine(itemName);
        line.assignTo(this);
        lines.add(line);
    }

    public Long getId() {
        return id;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public List<ReceiptLine> getLines() {
        return lines;
    }
}
