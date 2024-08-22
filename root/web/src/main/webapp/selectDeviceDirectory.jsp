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
<%@page import="java.nio.file.Path"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href=css/SyncLiteStyle.css>
<title>Configure SyncLite Job Monitor</title>
</head>

<% 
	String jobName = request.getParameter("job-name");
	String errorMsg = request.getParameter("errorMsg");
	
	String syncLiteDeviceDir = request.getParameter("synclite-device-dir");
	if (syncLiteDeviceDir == null) {	
		Path defaultDeviceDir = Path.of(System.getProperty("user.home"), "synclite");
		syncLiteDeviceDir = defaultDeviceDir.toString();
	}
%>

<body>
	<%@include file="html/menu.html"%>
	<div class="main">
		<h2>Configure SyncLite Job Monitor</h2>
		<%	
		if (errorMsg != null) {
			out.println("<h4 style=\"color: red;\">" + errorMsg + "</h4>");
		}
		%>
	
		<form method="post" action="validateDeviceDirectory">
			<table>
				<tbody>
					<tr>
						<td>SyncLite Base Directory</td>
						<td><input type="text" size = 60 id="synclite-device-dir" name="synclite-device-dir" value="<%=syncLiteDeviceDir%>" title="Specify a work directory for SyncLite dbreader to store SyncLite devices holding extracted data frpom source database."/></td>
					</tr>
				</tbody>
			</table>
			<center>
				<button type="submit" name="next">Next</button>
			</center>			
			
		</form>
	</div>
</body>
</html>