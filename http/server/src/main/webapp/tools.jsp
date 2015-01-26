<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core"%>

<html>
<title>Tupelo: Managed Disk Listing and Tools</title>

<body>
The following managed disks are in this store.
<p>

<table>
<c:forEach items="${mdds}" var="mdd" varStatus="s">
<tr>
<td><c:out value="${mdd}"/></td>
<td><form method="post" action="./tools/digest/<c:out value="${mdd.diskID}/${mdd.session}"/>">
    <input type="submit" name="submit" value="Digest" /></form></td>
<td><form method="post" action="./tools/hashvs/<c:out value="${mdd.diskID}/${mdd.session}"/>">
    <input type="submit" name="submit" value="Hash VolumeSystem" /></form></td>
<td><form method="post" action="./tools/hashfs/<c:out value="${mdd.diskID}/${mdd.session}"/>">
    <input type="submit" name="submit" value="Hash FileSystem(s)" /></form></td>
<td><form method="post" action="./tools/bodyfile/<c:out value="${mdd.diskID}/${mdd.session}"/>">
    <input type="submit" name="submit" value="Body File(s)" /></form></td>
</tr>
</c:forEach>
</table>
</body>
</html>