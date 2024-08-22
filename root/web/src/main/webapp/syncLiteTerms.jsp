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
<title>SyncLite - End User License Agreement</title>
</head>

<%
String syncLiteTerms ="";
if (request.getParameter("synclite-terms") != null) {
	//RequestDispatcher rd = request.getRequestDispatcher("configureDBReader.jsp");
	//rd.forward(request, response);
	response.sendRedirect("selectDeviceDirectory.jsp");
} else {
	String libPath = application.getRealPath("/WEB-INF/lib");
	Path termsFilePath = Path.of(libPath, "synclite_end_user_license_agreement.txt");
	syncLiteTerms = Files.readString(termsFilePath);
}
%>

<body>
	<%@include file="html/menu.html"%>
	<div class="main">
		<h2>SyncLite - End User License Agreement</h2>
		<form method="post">
			<table>
				<tbody>
					<tr>
						<td><textarea name="synclite-terms" id="workload" rows="40" cols="160" readonly><%=syncLiteTerms%></textarea></td>
					</tr>
				</tbody>
			</table>
			<center>
				<button type="submit" name="agree" id="agree">Agree</button>
			</center>			
		</form>
	</div>
</body>
</html>