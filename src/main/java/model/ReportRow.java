package model;

import java.math.BigDecimal;

public class ReportRow {
    private String label;
    private String detail;
    private long quantity;
    private long secondaryQuantity;
    private BigDecimal amount = BigDecimal.ZERO;

    public ReportRow(String label, String detail, long quantity, long secondaryQuantity, BigDecimal amount) {
        this.label = label;
        this.detail = detail;
        this.quantity = quantity;
        this.secondaryQuantity = secondaryQuantity;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }
    public String getLabel() { return label; }
    public String getDetail() { return detail; }
    public long getQuantity() { return quantity; }
    public long getSecondaryQuantity() { return secondaryQuantity; }
    public BigDecimal getAmount() { return amount; }
}
