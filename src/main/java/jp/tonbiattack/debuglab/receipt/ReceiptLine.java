package jp.tonbiattack.debuglab.receipt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_lines")
public class ReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    protected ReceiptLine() {
    }

    ReceiptLine(String itemName) {
        this.itemName = itemName;
    }

    void assignTo(Receipt receipt) {
        this.receipt = receipt;
    }

    public String getItemName() {
        return itemName;
    }
}
