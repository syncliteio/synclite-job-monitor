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

<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.ZoneId"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.ZonedDateTime"%>
<%@page import="java.time.Instant"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.nio.file.Files"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.sql.*"%>
<%@ page import="org.sqlite.*"%>
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href=css/SyncLiteStyle.css>
<title>SyncLite Job Monitor Dashboard</title>

<script type="text/javascript">

function autoRefreshSetTimeout() {
    const refreshInterval = parseInt(document.getElementById("refresh-interval").value);
    
    if (!isNaN(refreshInterval)) {
    	const val = refreshInterval * 1000;
    	if (val === 0) {
    		const timeoutObj = setTimeout("autoRefresh()", 1000);
    		clearTimeout(timeoutObj);    		
    	} else {    		
    		setTimeout("autoRefresh()", val);
    	}
	}	
}

function autoRefresh() {
	document.forms['dashboardForm'].submit();
}

</script>

</head>

<body onload="autoRefreshSetTimeout()">
	<%@include file="html/menu.html"%>	
	<div class="main">
		<h2>SyncLite Job Monitor Dashboard</h2>
		<%
			if (session.getAttribute("synclite-device-dir") == null) {
				out.println("<h4 style=\"color: red;\"> Please load job monitor.</h4>");
				throw new javax.servlet.jsp.SkipPageException();		
			}		
		%>

		<center>
			<%-- response.setIntHeader("Refresh", 2); --%>
			<table>
				<tbody>
					<%	
					String syncliteDeviceDir = session.getAttribute("synclite-device-dir").toString();
					int refreshInterval = 30;
					if (request.getParameter("refresh-interval") != null) {
						try {
							refreshInterval = Integer.valueOf(request.getParameter("refresh-interval").toString());
						} catch (Exception e) {
							refreshInterval = 30;
						}
					}

					Integer numDBReaderJobs = 0;
					if (session.getAttribute("numDBReaderJobs") != null) {
						numDBReaderJobs = Integer.valueOf(session.getAttribute("numDBReaderJobs").toString());
					}
					Integer numQReaderJobs = 0;
					if (session.getAttribute("numQReaderJobs") != null) {
						numQReaderJobs = Integer.valueOf(session.getAttribute("numQReaderJobs").toString());
					}
					Integer numConsolidatorJobs = 0;
					if (session.getAttribute("numConsolidatorJobs") != null) {
						numConsolidatorJobs = Integer.valueOf(session.getAttribute("numConsolidatorJobs").toString());
					}
					
					Integer numTotalJobs = numDBReaderJobs + numQReaderJobs + numConsolidatorJobs;

					String scheduleStatus = "STOPPED";
					if (session.getAttribute("syncite-job-starter-scheduler") != null) {
						scheduleStatus = "STARTED";
					}
					
					Integer totalNumSchedules = 0;
					if (session.getAttribute("totalNumSchedules") != null) {
						totalNumSchedules = Integer.valueOf(session.getAttribute("totalNumSchedules").toString());
					}
					
					out.println("<table>");
                	out.println("<tr>");
                	out.println("<td></td>");
                	out.println("<td>");
                	out.println("<form name=\"dashboardForm\" method=\"post\" action=\"dashboard.jsp\">");
                	out.println("<div class=\"pagination\">");
                	out.println("REFRESH IN ");
                	out.println("<input type=\"text\" id=\"refresh-interval\" name=\"refresh-interval\" value =\"" + refreshInterval + "\" size=\"1\" onchange=\"autoRefreshSetTimeout()\">");
                	out.println(" SECONDS");
                	out.println("</div>");
                	out.println("</form>");
                	out.println("</td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> SyncLite Base Directory</td>");
                	out.println("<td>" + syncliteDeviceDir + "</td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> Total Jobs </td>");
                	out.println("<td><a href=\"jobSummary.jsp\">" + numTotalJobs + "</a></td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> DBReader Jobs </td>");
                	out.println("<td><a href=\"jobSummary.jsp?jobType=DBREADER\">" + numDBReaderJobs + "</a></td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> QReader Jobs </td>");
                	out.println("<td><a href=\"jobSummary.jsp?jobType=QREADER\">" + numQReaderJobs + "</a></td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> Consolidator Jobs </td>");
                	out.println("<td><a href=\"jobSummary.jsp?jobType=CONSOLIDATOR\">" + numConsolidatorJobs + "</a></td>");
                	out.println("</tr>");

                	out.println("<tr>");
                	out.println("<td> Job Scheduler Status </td>");
                	out.println("<td>"+ scheduleStatus + "</td>");
                	out.println("</tr>");

                	
                	out.println("<tr>");
                	out.println("<td> Total Schedules </td>");
                	out.println("<td><a href=\"configureScheduler.jsp\">"+ totalNumSchedules + "</td>");
                	out.println("</tr>");
                	
                    out.println("</table>");
            	%>
				</tbody>
			</table>
		</center>
	</div>
</body>
</html>