package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FinePayment implements Serializable {
    private static final long serialVersionUID = 1L;
    private long paymentId;
    private long issueId;
    private String bookTitle;
    private String studentName;
    private BigDecimal amount;
    private String method;
    private LocalDateTime paidAt;
    private String recordedBy;
    private String note;

    public long getPaymentId() { return paymentId; }
    public void setPaymentId(long paymentId) { this.paymentId = paymentId; }
    public long getIssueId() { return issueId; }
    public void setIssueId(long issueId) { this.issueId = issueId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
