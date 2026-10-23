<div class="table-responsive"><table class="table align-middle data-table"><thead><tr><th>Book</th><th>Author</th><th>Issues</th></tr></thead><tbody>
<% if (mostBorrowed == null || mostBorrowed.isEmpty()) { %><tr><td colspan="3">No issues in this range.</td></tr><% } else { for (ReportRow row : mostBorrowed) { %>
<tr><td><%= HtmlUtil.escape(row.getLabel()) %></td><td><%= HtmlUtil.escape(row.getDetail()) %></td><td><%= row.getQuantity() %></td></tr>
<% } } %></tbody></table></div>
