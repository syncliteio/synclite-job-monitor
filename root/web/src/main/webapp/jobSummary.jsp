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

<%@page import="java.nio.charset.Charset"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.nio.file.Files"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.sql.*"%>
<%@ page import="org.sqlite.*"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="java.io.File"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.time.Instant"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.ZoneId"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.IOException"%>
<%@page import="com.synclite.jobmonitor.web.JobInfo"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href=css/SyncLiteStyle.css>

<title>SyncLite Jobs</title>
</head>

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
	document.forms['tableForm'].submit();
}

</script>

<body onload="autoRefreshSetTimeout()">
	<%@include file="html/menu.html"%>	
	<div class="main">
		<h2>SyncLite Jobs</h2>
		<%		
			if (session.getAttribute("synclite-device-dir") == null) {
				out.println("<h4 style=\"color: red;\"> Please load job monitor</h4>");
				throw new javax.servlet.jsp.SkipPageException();		
			}

	 	    String currentURL = request.getRequestURL().toString();
			String prefix =  currentURL.substring(0, currentURL.indexOf("/"));
			
			String consolidatorURL = currentURL;
			String consolidatorAppName = "synclite-consolidator";

			String dbReaderURL = currentURL;
			String dbReaderAppName = "synclite-dbreader";			

			String qReaderURL = currentURL;
			String qReaderAppName = "synclite-qreader";
			
			if (session.getAttribute("synclite-product-version") != null) {
				String currentProductVersion = session.getAttribute("synclite-product-version").toString();
				if (currentURL.contains(currentProductVersion)) {
					consolidatorAppName = "synclite-consolidator-" + currentProductVersion;
					consolidatorURL = prefix + "/" + "synclite-consolidator-" + currentProductVersion + "/loadJob";
					
					dbReaderURL = prefix + "/" + "synclite-dbreader-" + currentProductVersion + "/loadJob";
					dbReaderAppName = "synclite-dbreader-" + currentProductVersion;
					
					qReaderURL = prefix + "/" + "synclite-qreader-" + currentProductVersion + "/loadJob";
					qReaderAppName = "synclite-qreader-" + currentProductVersion;
				} else {
					consolidatorURL = prefix + "/" + "synclite-consolidator" + "/loadJob"; 
					dbReaderURL = prefix + "/" + "synclite-dbreader" + "/loadJob";
					qReaderURL = prefix + "/" + "synclite-qreader" + "/loadJob";
				}
			}
			String consolidatorCorePath = Path.of(Path.of(getServletContext().getRealPath("/")).getParent().toString(), consolidatorAppName, "WEB-INF", "lib").toString();
			String dbReaderCorePath = Path.of(Path.of(getServletContext().getRealPath("/")).getParent().toString(), dbReaderAppName, "WEB-INF", "lib").toString();
			String qReaderCorePath = Path.of(Path.of(getServletContext().getRealPath("/")).getParent().toString(), qReaderAppName, "WEB-INF", "lib").toString();
			
			String errorMsg = "";
			Path syncliteDeviceDir = Path.of(session.getAttribute("synclite-device-dir").toString());
			Long numRecords = 100L;
			LinkedHashMap<String, JobInfo> jobInfoMap = new LinkedHashMap<String, JobInfo>();
			int refreshInterval = 30;
			String jobName = "";
			String jobType = "ALL";
			String jobStatus = "ALL";
			HashSet<String> jobNames = new HashSet<String>();
			Integer numDBReaderJobs = 0;
			Integer numQReaderJobs = 0;
			Integer numConsolidatorJobs = 0;
			try {
				if (request.getParameter("jobName") != null) {
					jobName = request.getParameter("jobName").trim();
				}			

				if (request.getParameter("jobType") != null) {
					jobType  = request.getParameter("jobType").trim();
				}
				
				if (request.getParameter("jobStatus") != null) {
					jobStatus = request.getParameter("jobStatus").trim();
				}
				
				if (request.getParameter("refresh-interval") != null) {
					try {
						refreshInterval = Integer.valueOf(request.getParameter("refresh-interval").toString());
					} catch (Exception e) {
						refreshInterval = 5;
					}
				}
			
				//Code to identify jobs with their types and status'	
				//Iterate on the base directory
				//Validate each sub-directory ( job directory)
				//- if it contains db and workDir folders
				//- if db contains a synclite-dbreader.conf file or synclite-qreader.conf file AND workDir contains synclite-consolidator.conf file
				// Get the status of jobs using jps.
				//
		
				File[] subDirs = syncliteDeviceDir.toFile().listFiles();
	
	            // Iterate over the list of files and directories
	            for (File jobDir : subDirs) {
	                // Check if the current item is a directory
	                if (jobDir.isDirectory()) {					
						Path dbDir = jobDir.toPath().resolve("db");
						Path workDir = jobDir.toPath().resolve("workDir");
						
						if (Files.exists(dbDir)) {						
							Path dbReaderConfigFilepath = dbDir.resolve("synclite_dbreader.conf");
							Path qReaderConfigFilePath = dbDir.resolve("synclite_qreader.conf");
							
							if (Files.exists(dbReaderConfigFilepath)) {
								JobInfo jobInfo = new JobInfo();
								jobInfo.name = jobDir.toPath().getFileName().toString();
								jobInfo.confPath = dbReaderConfigFilepath;
								jobInfo.rootPath = dbDir;
								jobInfo.type = "DBREADER";
								jobInfo.componentName = "com.synclite.dbreader.Main";
								jobInfo.typeDisplayName = "SyncLite DBReader";
								jobInfo.url = dbReaderURL + "?synclite-device-dir=" + URLEncoder.encode(dbDir.toString(), Charset.defaultCharset());
								jobInfo.status = "STOPPED";
								jobInfo.pid = 0L;
								jobInfo.numSchedules = 0;
								jobInfoMap.put(jobInfo.name + ":" + jobInfo.type, jobInfo);
								jobNames.add(jobInfo.name);
								++numDBReaderJobs;
							}
	
							if (Files.exists(qReaderConfigFilePath)) {
								JobInfo jobInfo = new JobInfo();
								jobInfo.name = jobDir.toPath().getFileName().toString();
								jobInfo.confPath = qReaderConfigFilePath;
								jobInfo.rootPath = dbDir;
								jobInfo.type = "QREADER";
								jobInfo.componentName = "com.synclite.qreader.Main";
								jobInfo.typeDisplayName = "SyncLite QReader";
								jobInfo.url = qReaderURL + "?synclite-device-dir=" + URLEncoder.encode(dbDir.toString(), Charset.defaultCharset());
								jobInfo.status = "STOPPED";
								jobInfo.pid = 0L;
								jobInfo.numSchedules = 0;
								jobInfoMap.put(jobInfo.name + ":" + jobInfo.type, jobInfo);
								jobNames.add(jobInfo.name);
								++numQReaderJobs;
							}
						}
						
						if (Files.exists(workDir)) {
							Path consolidatorConfigFilePath = workDir.resolve("synclite_consolidator.conf");
	
							if (Files.exists(consolidatorConfigFilePath)) {
								JobInfo jobInfo = new JobInfo();
								jobInfo.name = jobDir.toPath().getFileName().toString();
								jobInfo.confPath = consolidatorConfigFilePath;
								jobInfo.rootPath = workDir;
								jobInfo.type = "CONSOLIDATOR";
								jobInfo.componentName = "com.synclite.consolidator.Main";
								jobInfo.typeDisplayName = "SyncLite Consolidator";
								jobInfo.url = consolidatorURL + "?device-data-root=" + URLEncoder.encode(workDir.toString(), Charset.defaultCharset());
								jobInfo.status = "STOPPED";
								jobInfo.pid = 0L;
								jobInfo.numSchedules = 0;
								jobInfoMap.put(jobInfo.name + ":" + jobInfo.type, jobInfo);
								jobNames.add(jobInfo.name);
								++numConsolidatorJobs;
							}
						}
	                }
				}

	            //Try to load schedule info for jobs
	            int totalNumSchedules = 0;
				try {
			        Path confPath = syncliteDeviceDir.resolve("synclite_job_schedules.json");
					JSONArray jobSchedulesJsonArray = null;
					//Read contents of the scheduler conf file in a json array
					if (Files.exists(confPath)) {
						jobSchedulesJsonArray = new JSONArray(Files.readString(confPath));
						
						for (int idx=0; idx < jobSchedulesJsonArray.length(); ++idx) {
							JSONObject obj = jobSchedulesJsonArray.getJSONObject(idx);
							
							String key = obj.getString("jobName") + ":" + obj.getString("jobType");							
							if (jobInfoMap.containsKey(key)) {
								++jobInfoMap.get(key).numSchedules;
								++totalNumSchedules;
							}
						}
					} 
				} catch(Exception e) {					
					throw e;
				}

				
	            //Now get the process IDs for all java process and try to find PID for each job.
				//Get current job PID if running
				long currentJobPID = 0;
				Process jpsProc;
				if (! System.getProperty("os.name").startsWith("Windows")) {
					String javaHome = System.getenv("JAVA_HOME");			
					String scriptPath = "jps";
					if (javaHome != null) {
						scriptPath = javaHome + "/bin/jps";
					} else {
						scriptPath = "jps";
					}
					String[] cmdArray = {scriptPath, "-l", "-m"};
					jpsProc = Runtime.getRuntime().exec(cmdArray);
				} else {
					String javaHome = System.getenv("JAVA_HOME");			
					String scriptPath = "jps";
					if (javaHome != null) {
						scriptPath = javaHome + "\\bin\\jps";
					} else {
						scriptPath = "jps";
					}
					String[] cmdArray = {scriptPath, "-l", "-m"};
					jpsProc = Runtime.getRuntime().exec(cmdArray);
				}

				BufferedReader stdout = new BufferedReader(new InputStreamReader(jpsProc.getInputStream()));
				String line = stdout.readLine();
				while (line != null) {
					for (JobInfo jd : jobInfoMap.values()) {
						if (line.contains(jd.componentName) && line.contains(jd.rootPath.toString())) {
							jd.pid = Long.valueOf(line.split(" ")[0]);
							jd.status = "RUNNING";
						}
					}
					line = stdout.readLine();
				}

	            session.setAttribute("jobInfoMap", jobInfoMap);
	            session.setAttribute("jobNames", jobNames);
	            session.setAttribute("numDBReaderJobs", numDBReaderJobs);
	            session.setAttribute("numQReaderJobs", numQReaderJobs);
	            session.setAttribute("numConsolidatorJobs", numConsolidatorJobs);
	            session.setAttribute("consolidatorCorePath", consolidatorCorePath);
	            session.setAttribute("dbReaderCorePath", dbReaderCorePath);
	            session.setAttribute("qReaderCorePath", qReaderCorePath);
	            session.setAttribute("totalNumSchedules", totalNumSchedules);

			} catch (Exception e) {
				errorMsg = "Failed to load SyncLite job information from base directory : " + syncliteDeviceDir.toString() + " : " + e.getMessage();
				out.println("<h4 style=\"color: red;\">" + errorMsg + "</h4>");
				throw new javax.servlet.jsp.SkipPageException();		
			}

		%>
			<center>
				<form name="tableForm" id="tableForm" method="post">
					<table>
						<tr>
							<td>
								Job Name <input type="text" name = "jobName" id = "jobName" value = <%= jobName%>>					 
							</td>

							<td>Job Type <select id="jobType" name="jobType">
	                             <%
	                             if (jobType.equals("DBREADER")) {
	                             	out.println("<option value=\"DBREADER\" selected>SyncLite DBReader</option>");
	                             } else {
	                             	out.println("<option value=\"DBREADER\">SyncLite DBReader</option>");
	                             }
	                             if (jobType.equals("QREADER")) {
		                             out.println("<option value=\"QREADER\" selected>SyncLite QReader</option>");
	                             } else {
	                             	out.println("<option value=\"QREADER\">SyncLite QReader</option>");
	                             }
	                             if (jobType.equals("CONSOLIDATOR")) {
	                             	out.println("<option value=\"CONSOLIDATOR\" selected>SyncLite Consolidator</option>");
	                             } else {
	                             	out.println("<option value=\"CONSOLIDATOR\">SyncLite Consolidator</option>");
	                             }
	                             if (jobType.equals("ALL")) {
	                             	out.println("<option value=\"ALL\" selected>All</option>");
	                             } else {
	                             	out.println("<option value=\"ALL\">All</option>");
	                             }	                             
	                             %>
							</td>
							<td>Job Status <select id="jobStatus" name="jobStatus">
	                             <%
	                             if (jobStatus.equals("RUNNING")) {
	                             	out.println("<option value=\"RUNNING\" selected>Running</option>");
	                             } else {
	                             	out.println("<option value=\"RUNNING\">Running</option>");
	                             }
	                             if (jobStatus.equals("STOPPED")) {
		                             out.println("<option value=\"STOPPED\" selected>Stopped</option>");
	                             } else {
	                             	out.println("<option value=\"STOPPED\">Stopped</option>");
	                             }
	                             if (jobStatus.equals("ALL")) {
	                             	out.println("<option value=\"ALL\" selected>All</option>");
	                             } else {
	                             	out.println("<option value=\"ALL\">All</option>");
	                             }
	                             %>
							</td>

							<td>				
								<input type="button" name="Go" id="Go" value="Go" onclick = "this.form.action='jobSummary.jsp'; this.form.submit()">
							</td>
							<td>				
								<div class="pagination">
									REFRESH IN <input type="text" id="refresh-interval" name="refresh-interval" value ="<%=refreshInterval%>" size="1" onchange="autoRefreshSetTimeout()"> SECONDS
								</div>
							</td>
						</tr>
					</table>
					<table>
					<tr></tr>
					<tr>
						<th>ID</th>
						<th>Name</th>
						<th>Type</th>
						<th>Status</th>
						<th>PID</th>
						<th>Schedules</th>
					</tr>
					<%
						int idx = 1;
						for (JobInfo job : jobInfoMap.values()) {
							if (!jobType.equals("ALL")) {
								if (! job.type.equals(jobType)) {
									continue;
								}
							}
							if (!jobStatus.equals("ALL")) {
								if (! job.status.equals(jobStatus)) {
									continue;
								}
							}
							if (!jobName.isBlank()) {
								if (!job.name.equalsIgnoreCase(jobName)) {
									continue;
								}
							}

							out.println("<tr>");
							out.println("<td>" + idx + "</td>");
							out.println("<td><a href=\"" + job.url + "\" target=\"_blank\">" + job.name + "</a></td>");
							out.println("<td>" + job.typeDisplayName + "</td>");
							out.println("<td>" + job.status+ "</td>");
							if (job.status.equals("RUNNING")) {
								out.println("<td>" + job.pid+ "</td>");
							} else {
								out.println("<td></td>");
							}
							out.println("<td><a href=\"configureScheduler.jsp\">" + job.numSchedules + "</td>");
							++idx;
						}
					%>
				</table>
			</form>
		</center>
	</div>
</body>
</html>	