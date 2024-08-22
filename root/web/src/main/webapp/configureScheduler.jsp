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

<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.nio.file.Files"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.HashSet"%>
<%@page import="com.synclite.jobmonitor.web.JobSchedule"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href=css/SyncLiteStyle.css>
<title>Configure SyncLite Job Scheduler</title>
</head>

<body>
	<%@include file="html/menu.html"%>
	<div class="main">
		<h2>Configure Job Schedules</h2>
		<%
			if (session.getAttribute("synclite-device-dir") == null) {
				out.println("<h4 style=\"color: red;\"> Please load job monitor</h4>");
				throw new javax.servlet.jsp.SkipPageException();		
			}

			if (session.getAttribute("jobNames") == null) {
				out.println("<h4 style=\"color: red;\"> Please load the job summary first.</h4>");
				throw new javax.servlet.jsp.SkipPageException();
			}

		
			String syncLiteDeviceDir = session.getAttribute("synclite-device-dir").toString();
			String errorMsg = request.getParameter("errorMsg");
			Path confPath = Path.of(syncLiteDeviceDir, "synclite_job_schedules.json");
			JSONArray jobSchedulesJsonArray = null;
			try {
				//Read contents of the scheduler conf file in a json array
				if (Files.exists(confPath)) {
					jobSchedulesJsonArray = new JSONArray(Files.readString(confPath));
				} else {
					jobSchedulesJsonArray = new JSONArray();
				}
			} catch(Exception e) {
				out.println("<h4 style=\"color: red;\">Failed to read existing schedule information from file : " + confPath + " : " + e.getMessage() + "</h4>");
				throw new SkipPageException();
			}

			if (errorMsg != null) {
				out.println("<h4 style=\"color: red;\">Failed to save and start SyncLite job scheduler : " + errorMsg + "</h4>");
			}

			
			Integer numSchedules = jobSchedulesJsonArray.length();
			if (request.getParameter("numSchedules") != null) {
				numSchedules = Integer.valueOf(request.getParameter("numSchedules"));
			}

			HashMap<String, String> properties = new HashMap<String, String>();
			long nextScheduleID = System.currentTimeMillis();
			for (int idx = 1; idx <= numSchedules; ++idx) {
				JobSchedule jobSchedule = null;
				if ((idx - 1) < jobSchedulesJsonArray.length()) {
					JSONObject o = (JSONObject) jobSchedulesJsonArray.get(idx-1);
					jobSchedule = new JobSchedule(o);
				}

				if (request.getParameter("synclite-job-scheduler-schedule-id-" + idx) != null) {
					properties.put("synclite-job-scheduler-schedule-id-" + idx, request.getParameter("synclite-job-scheduler-schedule-id-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-schedule-id-" + idx, String.valueOf(jobSchedule.scheduleID));
					} else {
						properties.put("synclite-job-scheduler-schedule-id-" + idx, String.valueOf(nextScheduleID));
						++nextScheduleID;
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-name-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-name-" + idx, request.getParameter("synclite-job-scheduler-job-name-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-job-name-" + idx, jobSchedule.jobName);
					} else {
						properties.put("synclite-job-scheduler-job-name-" + idx, "");
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-type-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-type-" + idx, request.getParameter("synclite-job-scheduler-job-type-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-type-" + idx, jobSchedule.jobType);
					} else {
						properties.put("synclite-job-scheduler-job-type-" + idx, "NONE");
					}
				}

				if (request.getParameter("synclite-job-scheduler-start-hour-" + idx) != null) {
					properties.put("synclite-job-scheduler-start-hour-" + idx, request.getParameter("synclite-job-scheduler-start-hour-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-start-hour-" + idx, String.valueOf(jobSchedule.scheduleStartHour));
					} else {
						properties.put("synclite-job-scheduler-start-hour-" + idx, "9");
					}
				}
		
				if (request.getParameter("synclite-job-scheduler-start-minute-" + idx) != null) {
					properties.put("synclite-job-scheduler-start-minute-" + idx, request.getParameter("synclite-job-scheduler-start-minute-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-start-minute-" + idx, String.valueOf(jobSchedule.scheduleStartMinute));
					} else {
						properties.put("synclite-job-scheduler-start-minute-" + idx, "00");
					}
				}
		
				if (request.getParameter("synclite-job-scheduler-end-hour-" + idx) != null) {
					properties.put("synclite-job-scheduler-end-hour-" + idx, request.getParameter("synclite-job-scheduler-end-hour-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-end-hour-" + idx, String.valueOf(jobSchedule.scheduleEndHour));
					} else {
						properties.put("synclite-job-scheduler-end-hour-" + idx, "18");
					}
				}
		
				if (request.getParameter("synclite-job-scheduler-end-minute-" + idx) != null) {
					properties.put("synclite-job-scheduler-end-minute-" + idx, request.getParameter("synclite-job-scheduler-end-minute-" + idx));
				} else {
					if (jobSchedule != null) {
						properties.put("synclite-job-scheduler-end-minute-" + idx, String.valueOf(jobSchedule.scheduleEndMinute));
					} else {
						properties.put("synclite-job-scheduler-end-minute-" + idx, "00");
					}
				}
		
				if (request.getParameter("synclite-job-scheduler-job-run-duration-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-run-duration-" + idx, request.getParameter("synclite-job-scheduler-job-run-duration-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-run-duration-" + idx, String.valueOf(jobSchedule.jobRunDuration));
					} else {
						properties.put("synclite-job-scheduler-job-run-duration-" + idx, "0");
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-run-duration-unit-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-run-duration-unit-" + idx, request.getParameter("synclite-job-scheduler-job-run-duration-unit-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-run-duration-unit-" + idx, jobSchedule.jobRunDurationUnit);
					} else {
						properties.put("synclite-job-scheduler-job-run-duration-unit-" + idx, "MINUTES");
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-run-interval-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-run-interval-" + idx, request.getParameter("synclite-job-scheduler-job-run-interval-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-run-interval-" + idx, String.valueOf(jobSchedule.jobRunInterval));
					} else {
						properties.put("synclite-job-scheduler-job-run-interval-" + idx, "0");
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-run-interval-unit-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-run-interval-unit-" + idx, request.getParameter("synclite-job-scheduler-job-run-interval-unit-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-run-interval-unit-" + idx, jobSchedule.jobRunIntervalUnit);
					} else {
						properties.put("synclite-job-scheduler-job-run-interval-unit-" + idx, "MINUTES");
					}
				}

				if (request.getParameter("synclite-job-scheduler-job-sub-type-" + idx) != null) {
					properties.put("synclite-job-scheduler-job-sub-type-" + idx, request.getParameter("synclite-job-scheduler-job-sub-type-" + idx));
				} else {
					if (jobSchedule != null) {				
						properties.put("synclite-job-scheduler-job-sub-type-" + idx, jobSchedule.jobSubType);
					} else {
						properties.put("synclite-job-scheduler-job-sub-type-" + idx, "READ");
					}
				}
			}	
			
			HashSet<String> jobNames = (HashSet) session.getAttribute("jobNames");
			
		%>		
		<form action="${pageContext.request.contextPath}/configureScheduler" method="post">
			<table>
				<tr>
					<td colspan="8">
						Note: Please stop the job scheduler if already running before setting up job scheduler configurations.
					</td>
				</tr>
				
				<tr>
					<th>Select All <input type="checkbox" id="select-all" name="select-all"></th>
					<th>ID</th>
					<th>Job Name</th>
					<th>Job Type</th>
					<th>Schedule Start At (HH:MM)</th>
					<th>Schedule End At (HH:MM)</th>
					<th>Job Run Duration</th>
					<th>Job Run Interval</th>
					<th>Job Sub Type</th>
				</tr>
			<%
				for (int idx = 1; idx <= numSchedules; ++idx) {
					out.println("<tr>");

					out.println("<td><input type=\"checkbox\" id=\"select-" + idx + "\" name=\"select-" + idx + "\" value=\"" + "1" + "\"/></td>");
					
					out.println("<td>" + idx + "</td>");
					out.println("<input type=\"hidden\" id=\"synclite-job-scheduler-schedule-id-" + idx + "\" name=\"synclite-job-scheduler-schedule-id-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-schedule-id-" + idx) + "\"/>");
					out.println("<td>");
					out.println("<select id=\"synclite-job-scheduler-job-name-" + idx +  "\" name=\"synclite-job-scheduler-job-name-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-name-" + idx) + "\" title=\"Select SyncLite job.\">");
					out.println("<option value=\"NONE\">NONE</option>");
					for (String jobName : jobNames) {
						if (properties.get("synclite-job-scheduler-job-name-" + idx).equals(jobName)) {
							out.println("<option value=\"" + jobName + "\" selected>"+  jobName + "</option>");
						} else {
							out.println("<option value=\"" + jobName + "\">"+  jobName + "</option>");
						}						
					}
					out.println("</select>");
					out.println("</td>");
					
					out.println("<td>");
					out.println("<select id=\"synclite-job-scheduler-job-type-" + idx +  "\" name=\"synclite-job-scheduler-job-type-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-type-" + idx) + "\" title=\"Select SyncLite job type.\">");
					if (properties.get("synclite-job-scheduler-job-type-" + idx).equals("DBREADER")) {
						out.println("<option value=\"DBREADER\" selected>DBReader</option>");
					} else {
						out.println("<option value=\"DBREADER\">DBReader</option>");
					}
					if (properties.get("synclite-job-scheduler-job-type-" + idx).equals("QREADER")) {
						out.println("<option value=\"QREADER\" selected>QReader</option>");
					} else {
						out.println("<option value=\"QREADER\">QReader</option>");
					}
					if (properties.get("synclite-job-scheduler-job-type-" + idx).equals("CONSOLIDATOR")) {
						out.println("<option value=\"CONSOLIDATOR\" selected>Consolidator</option>");
					} else {
						out.println("<option value=\"CONSOLIDATOR\">Consolidator</option>");
					}
					out.println("</select>");
					out.println("</td>");

					out.println("<td>");
					out.print("<input type=\"text\" size=\"2\" id=\"synclite-job-scheduler-start-hour-" + idx + "\" name=\"synclite-job-scheduler-start-hour-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-start-hour-" + idx) + "\" title=\"Specify SyncLite dbreader job scheduler start hour.\"/>");
					out.print(":<input type=\"text\" size=\"2\" id=\"synclite-job-scheduler-start-minute-" + idx + "\" name=\"synclite-job-scheduler-start-minute-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-start-minute-" + idx) + "\" title=\"Specify SyncLite dbreader job scheduler start minute.\"/>");				
					out.println("</td>");

					out.println("<td>");
					out.print("<input type=\"text\" size=\"2\" id=\"synclite-job-scheduler-end-hour-" + idx + "\" name=\"synclite-job-scheduler-end-hour-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-end-hour-" + idx) + "\" title=\"Specify SyncLite dbreader job scheduler end hour.\"/>");
					out.print(":<input type=\"text\" size=\"2\" id=\"synclite-job-scheduler-end-minute-" + idx + "\" name=\"synclite-job-scheduler-end-minute-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-end-minute-" + idx) + "\" title=\"Specify SyncLite dbreader job scheduler end minute.\"/>");				
					out.println("</td>");

					out.println("<td>");
					out.println("<input type=\"text\" size=\"4\" id=\"synclite-job-scheduler-job-run-duration-" + idx + "\" name=\"synclite-job-scheduler-job-run-duration-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-run-duration-" + idx) + "\" title=\"Specify SyncLite dbreader job run duration. Value 0 indicates keep the job running once started until stopped.\"/>");
					out.println("<select id=\"synclite-job-scheduler-job-run-duration-unit-" + idx +  "\" name=\"synclite-job-scheduler-job-run-duration-unit-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-run-duration-unit-" + idx) + "\" title=\"Select DBReader job run duration unit\">");
					if (properties.get("synclite-job-scheduler-job-run-duration-unit-" + idx).equals("SECONDS")) {
						out.println("<option value=\"SECONDS\" selected>SECONDS</option>");
					} else {
						out.println("<option value=\"SECONDS\">SECONDS</option>");
					}
					if (properties.get("synclite-job-scheduler-job-run-duration-unit-" + idx).equals("MINUTES")) {
						out.println("<option value=\"MINUTES\" selected>MINUTES</option>");
					} else {
						out.println("<option value=\"MINUTES\">MINUTES</option>");
					}
					if (properties.get("synclite-job-scheduler-job-run-duration-unit-" + idx).equals("HOURS")) {
						out.println("<option value=\"HOURS\" selected>HOURS</option>");
					} else {
						out.println("<option value=\"HOURS\">HOURS</option>");
					}
					out.println("</select>");
					out.println("</td>");
					
					out.println("<td>");
					out.println("<input type=\"text\" size=\"4\" id=\"synclite-job-scheduler-job-run-interval-" + idx + "\" name=\"synclite-job-scheduler-job-run-interval-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-run-interval-" + idx) + "\" title=\"Specify SyncLite dbreader job run interval. Value 0 indicates no periodic starting of the job.\"/>");
					out.println("<select id=\"synclite-job-scheduler-job-run-interval-unit-" + idx +  "\" name=\"synclite-job-scheduler-job-run-interval-unit-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-run-interval-unit-" + idx) + "\" title=\"Select DBReader job run interval unit\">");
					if (properties.get("synclite-job-scheduler-job-run-interval-unit-" + idx).equals("SECONDS")) {
						out.println("<option value=\"SECONDS\" selected>SECONDS</option>");
					} else {
						out.println("<option value=\"SECONDS\">SECONDS</option>");
					}
					if (properties.get("synclite-job-scheduler-job-run-interval-unit-" + idx).equals("MINUTES")) {
						out.println("<option value=\"MINUTES\" selected>MINUTES</option>");
					} else {
						out.println("<option value=\"MINUTES\">MINUTES</option>");
					}
					if (properties.get("synclite-job-scheduler-job-run-interval-unit-" + idx).equals("HOURS")) {
						out.println("<option value=\"HOURS\" selected>HOURS</option>");
					} else {
						out.println("<option value=\"HOURS\">HOURS</option>");
					}
					out.println("</select>");
					out.println("</td>");

					out.println("<td>");
					out.println("<select id=\"synclite-job-scheduler-job-sub-type-" + idx +  "\" name=\"synclite-job-scheduler-job-sub-type-" + idx + "\" value=\"" + properties.get("synclite-job-scheduler-job-type-" + idx) + "\" title=\"Select DBReader job sub-type to schedule.\">");
					if (properties.get("synclite-job-scheduler-job-sub-type-" + idx).equals("READ")) {
						out.println("<option value=\"READ\" selected>READ/SYNC</option>");
					} else {
						out.println("<option value=\"READ\">READ/SYNC</option>");
					}
					if (properties.get("synclite-job-scheduler-job-sub-type-" + idx).equals("DELETE-SYNC")) {
						out.println("<option value=\"DELETE-SYNC\" selected>DELETE-SYNC</option>");
					} else {
						out.println("<option value=\"DELETE-SYNC\">DELETE-SYNC</option>");
					}
					out.println("</select>");
					out.println("</td>");
					out.println("</tr>");
					}
				%>
				 <input type="hidden" name ="numSchedules" id ="numSchedules" value="<%=numSchedules%>">
			</table>
			
			<center>
				<button type="button" name="add" onclick="this.form.action='configureScheduler.jsp?numSchedules=<%=numSchedules + 1%>'; this.form.submit();">Add New Schedule</button>
				<button type="button" name="delete" onclick="this.form.action='deleteSchedules'; this.form.submit();">Delete Schedules</button>
				<button type="submit" name="next">Save And Start</button>
			</center>			
		</form>
	</div>
	
<script type="text/javascript">
	// Get references to the checkboxes
	const selectAllCheckbox = document.getElementById("select-all");
	const selectIndividualCheckboxes = document.querySelectorAll('input[type="checkbox"][name^="select-"]');
	
	// Add an event listener to the "select-all" checkbox
	selectAllCheckbox.addEventListener("change", function() {
	  const isChecked = selectAllCheckbox.checked;
	  // Set the state of individual checkboxes to match the "select-all" checkbox
	  selectIndividualCheckboxes.forEach(function(checkbox) {
	    checkbox.checked = isChecked;
	  });
	});
</script>

</body>
</html>