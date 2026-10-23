package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class LibrarySettings implements Serializable {
    private static final long serialVersionUID = 1L;
    private int maxActiveLoans;
    private int loanPeriodDays;
    private BigDecimal dailyFine;
    private BigDecimal maxFine;
    public int getMaxActiveLoans() { return maxActiveLoans; }
    public void setMaxActiveLoans(int maxActiveLoans) { this.maxActiveLoans = maxActiveLoans; }
    public int getLoanPeriodDays() { return loanPeriodDays; }
    public void setLoanPeriodDays(int loanPeriodDays) { this.loanPeriodDays = loanPeriodDays; }
    public BigDecimal getDailyFine() { return dailyFine; }
    public void setDailyFine(BigDecimal dailyFine) { this.dailyFine = dailyFine; }
    public BigDecimal getMaxFine() { return maxFine; }
    public void setMaxFine(BigDecimal maxFine) { this.maxFine = maxFine; }
}
