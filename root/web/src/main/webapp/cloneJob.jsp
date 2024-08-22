<%-- 
    Copyright (c) 2024 mahendra.chavan@syncLite.io, all rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
    in compliance with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License
    is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
    or implied.  See the License for the specific language governing permissions and limitations
    under the License.
--%>

<%@page import="java.nio.file.Files"%>
<%@page import="java.time.Instant"%>
<%@page import="java.io.File"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.HashMap"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.sql.*"%>
<%@ page import="org.sqlite.*"%>
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href=css/SyncLiteStyle.css>

<script type="text/javascript">
</script>	
<title>Clone Jobs</title>
</head>


<%
String errorMsg = request.getParameter("errorMsg");

String srcJobName = "";
if (request.getParameter("src-job-name") != null) {
	srcJobName = request.getParameter("src-job-name").toString();
} 

String tgtJobName = "";
if (request.getParameter("tgt-job-name") != null) {
	tgtJobName = request.getParameter("tgt-job-name").toString();
}

%>

<body>
	<%@include file="html/menu.html"%>	

	<div class="main">
		<h2>Clone Jobs</h2>
		<%
		if (session.getAttribute("synclite-device-dir") == null) {
			out.println("<h4 style=\"color: red;\"> Please load job monitor</h4>");
			throw new javax.servlet.jsp.SkipPageException();		
		}

		if (session.getAttribute("jobNames") == null) {
			out.println("<h4 style=\"color: red;\"> Please load the job summary first.</h4>");
			throw new javax.servlet.jsp.SkipPageException();
		}

		if (errorMsg != null) {
			out.println("<h4 style=\"color: red;\">" + errorMsg + "</h4>");
		}
		%>

		<form action="${pageContext.request.contextPath}/cloneJob" method="post">
			<table>
				<tbody>
					<tr>
						<td>Source Job Name</td>
						<td><input type="text" size = 30 id="src-job-name" name="src-job-name" value="<%=srcJobName%>" title="Specify source job name to clone from"/></td>
					</tr>
					<tr>
						<td>Destination Job Name</td>
						<td><input type="text" size = 30 id="tgt-job-name" name="tgt-job-name" value="<%=tgtJobName%>" title="Specify target job name to clone to"/></td>
					</tr>

			</table>
			<center>
				<button type="submit" name="clone">Clone Jobs</button>
			</center>
		</form>
	</div>
</body>
</html>