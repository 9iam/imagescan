<form action="/imagescan" method="GET">
    <input type="text" placeholder="http://en.wikipedia.org/" name="url"
        <% if (request.getParameter("url") != null) { %>
            value="<%= request.getParameter("url") %>"
        <% } %>
    />
    <input type="submit" value="Scan" disabled-disabled="disabled"/>
</form>