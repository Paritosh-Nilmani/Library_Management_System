<div class="table-responsive"><table class="table align-middle data-table"><thead><tr><th>Category</th><th>Total Copies</th><th>Available</th></tr></thead><tbody>
<% if (inventory == null || inventory.isEmpty()) { %><tr><td colspan="3">No catalog data.</td></tr><% } else { for (ReportRow row : inventory) { %>
<tr><td><%= HtmlUtil.escape(row.getLabel()) %></td><td><%= row.getQuantity() %></td><td><%= row.getSecondaryQuantity() %></td></tr>
<% } } %></tbody></table></div>
