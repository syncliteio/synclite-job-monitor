/*
 * Copyright (c) 2024 mahendra.chavan@synclite.io, all rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.synclite.jobmonitor.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Servlet implementation class StartJob
 */
@WebServlet("/deleteSchedules")
public class DeleteSchedules extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Logger globalTracer;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DeleteSchedules () {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (request.getSession().getAttribute("synclite-device-dir") == null) {
				response.sendRedirect("syncLiteTerms.jsp");			
			} else {
				
				if (request.getSession().getAttribute("syncite-job-starter-scheduler") != null) {
					throw new ServletException("Job Scheduler is running. Please stop it and then delete job schedules.");					
				}

				String syncLiteDeviceDir = request.getSession().getAttribute("synclite-device-dir").toString();
				Path confPath = Path.of(syncLiteDeviceDir, "synclite_job_schedules.json");

				initTracer(Path.of(syncLiteDeviceDir));
				
				Integer numSchedules = Integer.valueOf(request.getParameter("numSchedules").toString());

				if (numSchedules == 0) {
					throw new ServletException("No job schedules to delete.");
				}

				ArrayList<Long> schedulesToDelete = new ArrayList<Long>();
				for (int idx=1 ; idx <= numSchedules ; ++idx) {
					if (request.getParameter("select-" + idx) != null) {
						Long scheduleID = Long.valueOf(request.getParameter("synclite-job-scheduler-schedule-id-" + idx));
						schedulesToDelete.add(scheduleID);
					}
				}

				if (schedulesToDelete.isEmpty()) {
					throw new ServletException("No job schedules selected to delete.");
				}
				
				deleteSchedules(request, confPath, schedulesToDelete);
				
				response.sendRedirect("configureScheduler.jsp");
			}
		} catch (Exception e) {
			String errorMsg = e.getMessage();
			this.globalTracer.error("Failed to delete job schedules : " + e.getMessage(), e);
			request.getRequestDispatcher("configureScheduler.jsp?errorMsg=" + errorMsg).forward(request, response);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private void deleteSchedules(HttpServletRequest request, Path confPath, ArrayList<Long> schedulesToDelete) throws ServletException {
		try {
			JSONArray currentSchedules = null;
			if (Files.exists(confPath)) {
				currentSchedules = new JSONArray(Files.readString(confPath));
			} else {
				throw new ServletException("Schedule configuration file is missing : " + confPath); 
			}
			
			JSONArray newSchedules = new JSONArray();
			for (int idx = 0 ; idx < currentSchedules.length(); ++idx) {
				JSONObject scheduleObj = currentSchedules.getJSONObject(idx);
				
				if (!schedulesToDelete.contains(scheduleObj.getLong("scheduleID"))) {
					newSchedules.put(scheduleObj);
				}
			}
			//Write newSchedules to conf file.
			
			Files.writeString(confPath, newSchedules.toString(1));

			//Refresh schedule counts
			int totalNumSchedules = 0;
			HashMap<String, JobInfo> jobInfoMap = (HashMap<String, JobInfo>) request.getSession().getAttribute("jobInfoMap");				
			if (jobInfoMap != null) {
				for (JobInfo ji : jobInfoMap.values()) {
					ji.numSchedules = 0;
				}				
				for (int idx = 0 ; idx < newSchedules.length(); ++idx) {
					JSONObject scheduleObj = newSchedules.getJSONObject(idx);
					String key = scheduleObj.getString("jobName") + ":" + scheduleObj.getString("jobType");
					if (jobInfoMap.containsKey(key)) {
						++jobInfoMap.get(key).numSchedules;
						++totalNumSchedules;
					}
				}
				request.getSession().setAttribute("jobInfoMap", jobInfoMap);
				request.getSession().setAttribute("totalNumSchedules", totalNumSchedules);
			}
		} catch(Exception e) {			
			throw new ServletException("Failed to read/write existing schedule information from file : " + confPath + " : " + e.getMessage(), e);
		}

	}

	private final void initTracer(Path workDir) {
		this.globalTracer = Logger.getLogger(ValidateDeviceDirectory.class);
		if (this.globalTracer.getAppender("JobMonitorTracer") == null) {
			globalTracer.setLevel(Level.INFO);
			RollingFileAppender fa = new RollingFileAppender();
			fa.setName("JobMonitorTracer");
			fa.setFile(workDir.resolve("synclite_jobmonitor.trace").toString());
			fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
			fa.setMaxBackupIndex(10);
			fa.setAppend(true);
			fa.activateOptions();
			globalTracer.addAppender(fa);
		}
	}

}
