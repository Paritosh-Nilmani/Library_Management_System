<div class="table-responsive"><table class="table align-middle data-table"><thead><tr><th>Borrower</th><th>Registration</th><th>Issues</th></tr></thead><tbody>
<% if (activeBorrowers == null || activeBorrowers.isEmpty()) { %><tr><td colspan="3">No borrowers in this range.</td></tr><% } else { for (ReportRow row : activeBorrowers) { %>
<tr><td><%= HtmlUtil.escape(row.getLabel()) %></td><td><%= HtmlUtil.escape(row.getDetail()) %></td><td><%= row.getQuantity() %></td></tr>
<% } } %></tbody></table></div>
