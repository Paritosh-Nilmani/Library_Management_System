<div class="table-responsive"><table class="table align-middle data-table"><thead><tr><th>Month</th><th>Issues</th><th>Returns</th></tr></thead><tbody>
<% if (monthlyTrends == null || monthlyTrends.isEmpty()) { %><tr><td colspan="3">No circulation in this range.</td></tr><% } else { for (ReportRow row : monthlyTrends) { %>
<tr><td><%= HtmlUtil.escape(row.getLabel()) %></td><td><%= row.getQuantity() %></td><td><%= row.getSecondaryQuantity() %></td></tr>
<% } } %></tbody></table></div>
