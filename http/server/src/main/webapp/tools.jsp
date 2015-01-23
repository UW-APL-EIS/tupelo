<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core"%>

<html>
<title>Tupelo: Disk Listing</title>

<body>
The following managed disks are in this store.
<p>

<table>
<c:forEach items="${mdds}" var="mdd" varStatus="s">
<tr><td><c:out value="${mdd}"/></td><td><form method="post" action="./tools/digest/<c:out value="${mdd.diskID}/${mdd.session}"/>">
    <input type="submit" name="submit" value="Digest" /></form></td>
</tr>
</c:forEach>
</table>
</body>
</html>