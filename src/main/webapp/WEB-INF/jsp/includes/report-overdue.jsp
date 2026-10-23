<div class="table-responsive"><table class="table align-middle data-table"><thead><tr><th>Student</th><th>Book / Loan</th><th>Days</th></tr></thead><tbody>
<% if (overdueLoans == null || overdueLoans.isEmpty()) { %><tr><td colspan="3">No overdue loans.</td></tr><% } else { for (ReportRow row : overdueLoans) { %>
<tr><td><%= HtmlUtil.escape(row.getLabel()) %></td><td><%= HtmlUtil.escape(row.getDetail()) %></td><td><%= row.getQuantity() %></td></tr>
<% } } %></tbody></table></div>
