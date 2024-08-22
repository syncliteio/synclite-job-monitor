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
<title>Delete Jobs</title>
</head>


<%
String errorMsg = request.getParameter("errorMsg");

String jobName = "";
if (request.getParameter("job-name") != null) {
	jobName = request.getParameter("job-name").toString();
} 

%>

<body>
	<%@include file="html/menu.html"%>	

	<div class="main">
		<h2>Delete Jobs</h2>
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
		
		<form action="${pageContext.request.contextPath}/deleteJob" method="post">
			<table>
				<tbody>
					<tr>
						<td>Please note that all the SyncLite jobs with this job name will be deleted.</td>
					</tr>
					<tr>
						<td>Job Name</td>
						<td><input type="text" size = 30 id="job-name" name="job-name" value="<%=jobName%>" title="Specify job name to delete"/></td>
					</tr>
			</table>
			<center>
				<button type="submit" name="delete">Delete Jobs</button>
			</center>
		</form>
	</div>
</body>
</html>