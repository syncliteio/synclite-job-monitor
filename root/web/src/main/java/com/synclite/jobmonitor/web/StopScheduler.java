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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.quartz.Scheduler;

/**
 * Servlet implementation class StartJob
 */
@WebServlet("/stopScheduler")
public class StopScheduler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger globalTracer;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StopScheduler() {
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

				String syncLiteDeviceDir = request.getSession().getAttribute("synclite-device-dir").toString();

				initTracer(Path.of(syncLiteDeviceDir));

		        Scheduler scheduler = (Scheduler) request.getSession().getAttribute("syncite-job-starter-scheduler");
		        if (scheduler != null) {
		        	if (!scheduler.isShutdown()) {
		        		scheduler.shutdown();
		        	}
		        	request.getSession().setAttribute("syncite-job-starter-scheduler", null);
		        }
		        
		        Scheduler stopperScheduler = (Scheduler) request.getSession().getAttribute("syncite-job-stopper-scheduler");
		        if (stopperScheduler != null) {
		        	if (!stopperScheduler.isShutdown()) {
		        		stopperScheduler.shutdown();
		        	}
		        	request.getSession().setAttribute("syncite-job-stopper-scheduler", null);
		        }
		        
				response.sendRedirect("dashboard.jsp");
			}
		} catch (Exception e) {
			String errorMsg = e.getMessage();
			this.globalTracer.error("Failed to stop job scheduler : " + e.getMessage(), e);
			request.getRequestDispatcher("jobError.jsp?jobType=StopReadJobScheduler&errorMsg=" + errorMsg).forward(request, response);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
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
