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
import java.nio.file.Path;
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
		response.sendRedirect("configureScheduler.jsp");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (request.getSession().getAttribute("synclite-device-dir") == null) {
				response.sendRedirect("syncLiteTerms.jsp");			
			} else {
				
				if (request.getSession().getAttribute("syncite-job-starter-scheduler") != null) {
					throw new ServletException("Job Scheduler is running. Please stop it and then delete job schedules.");					
				}

				String syncLiteDeviceDir = request.getSession().getAttribute("synclite-device-dir").toString();
				Path syncLiteDeviceDirPath = Path.of(syncLiteDeviceDir);

				initTracer(syncLiteDeviceDirPath);
				
				Integer numSchedules = InputValidator.requireIntInRange(request, "numSchedules", "numSchedules", 1, 500);

				if (numSchedules == 0) {
					throw new ServletException("No job schedules to delete.");
				}

				ArrayList<Long> schedulesToDelete = new ArrayList<Long>();
				for (int idx=1 ; idx <= numSchedules ; ++idx) {
					if (request.getParameter("select-" + idx) != null) {
						Long scheduleID = InputValidator.requireLong(
								request,
								"synclite-job-scheduler-schedule-id-" + idx,
								"schedule id");
						schedulesToDelete.add(scheduleID);
					}
				}

				if (schedulesToDelete.isEmpty()) {
					throw new ServletException("No job schedules selected to delete.");
				}
				
				deleteSchedules(request, syncLiteDeviceDirPath, schedulesToDelete);
				
				response.sendRedirect("configureScheduler.jsp");
			}
		} catch (ServletException e) {
			response.setStatus(400);
			if (this.globalTracer != null) {
				this.globalTracer.error("Failed to delete job schedules : " + e.getMessage(), e);
			}
			request.setAttribute("errorMsg", e.getMessage());
			request.getRequestDispatcher("configureScheduler.jsp").forward(request, response);
		
		} catch (Exception e) {
			response.setStatus(500);
			if (this.globalTracer != null) {
				this.globalTracer.error("Failed to delete job schedules : " + e.getMessage(), e);
			}
			request.setAttribute("errorMsg", e.getMessage());
			request.getRequestDispatcher("configureScheduler.jsp").forward(request, response);
		
		}
	}

	@SuppressWarnings("unchecked")
	private void deleteSchedules(HttpServletRequest request, Path deviceDir, ArrayList<Long> schedulesToDelete) throws ServletException {
		try {
			MetadataManager.deleteSchedulesByIds(deviceDir, schedulesToDelete);

			JSONArray remaining = MetadataManager.loadAllSchedules(deviceDir);

			//Refresh schedule counts
			int totalNumSchedules = 0;
			HashMap<String, JobInfo> jobInfoMap = (HashMap<String, JobInfo>) request.getSession().getAttribute("jobInfoMap");
			if (jobInfoMap != null) {
				for (JobInfo ji : jobInfoMap.values()) {
					ji.numSchedules = 0;
				}
				for (int idx = 0 ; idx < remaining.length(); ++idx) {
					JSONObject scheduleObj = remaining.getJSONObject(idx);
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
			throw new ServletException("Failed to update schedules in metadata DB at " + deviceDir + " : " + e.getMessage(), e);
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
