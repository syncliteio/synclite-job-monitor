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
@WebServlet("/configureScheduler")
public class ConfigureScheduler extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Logger globalTracer;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ConfigureScheduler () {
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
					throw new ServletException("Job Scheduler is running. Please stop it and then configure the scheduler.");					
				}

				String corePath = Path.of(getServletContext().getRealPath("/"), "WEB-INF", "lib").toString();
				String syncLiteDeviceDir = request.getSession().getAttribute("synclite-device-dir").toString();
				Path confPath = Path.of(syncLiteDeviceDir, "synclite_job_schedules.json");
				String schedulerStatsFile = Path.of(syncLiteDeviceDir, "synclite_job_scheduler_statistics.db").toString();
				initTracer(Path.of(syncLiteDeviceDir));
				
				setupSchedulerStatsFile(schedulerStatsFile);
				
				Integer numSchedules = Integer.valueOf(request.getParameter("numSchedules").toString());
				
				if (numSchedules == 0) {
					throw new ServletException("Please add at least one schedule to save and start.");
				}
				
				ArrayList<JobSchedule> jobSchedules = new ArrayList<JobSchedule>();
				JSONArray jobSchedulesJSONArr  = new JSONArray();
				for (int idx=1 ; idx <= numSchedules ; ++idx) {
					JobSchedule jobSchedule = new JobSchedule();

					jobSchedule.scheduleID = Long.valueOf(request.getParameter("synclite-job-scheduler-schedule-id-" + idx));
					
					String jobName = request.getParameter("synclite-job-scheduler-job-name-" + idx);					
					if (jobName.equals("NONE")) {
						throw new ServletException("Please select a valid job for schedule index : " + idx);
					}
					jobSchedule.jobName = jobName;
					
					String jobType = request.getParameter("synclite-job-scheduler-job-type-" + idx);
					jobSchedule.jobType = jobType;
					
					String jobStartHourStr = request.getParameter("synclite-job-scheduler-start-hour-" + idx);					
					try {
						if (Integer.valueOf(jobStartHourStr) == null) {
							throw new ServletException("Please specify a valid numeric value for \"Job Start Hour\" for schedule number : " + idx);
						} else if ((Integer.valueOf(jobStartHourStr)) < 0 || (Integer.valueOf(jobStartHourStr) > 24)) {
							throw new ServletException("Please specify a value between 0 and 24 for \"Job Start Hour\" for schedule number : " + idx);
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a valid numeric value for \"Job Start Hour\" for schedule number : " + idx);
					}
					jobSchedule.scheduleStartHour = Integer.valueOf(jobStartHourStr);

					String jobStartMinuteStr = request.getParameter("synclite-job-scheduler-start-minute-" + idx);
					try {
						if (Integer.valueOf(jobStartMinuteStr) == null) {
							throw new ServletException("Please specify a valid numeric value for \"Job Start Minute\" for schedule number : " + idx);
						} else if ((Integer.valueOf(jobStartMinuteStr)) < 0 || (Integer.valueOf(jobStartMinuteStr) > 60)) {
							throw new ServletException("Please specify a value between 0 and 24 for \"Job Start Minute\" for schedule number : " + idx);
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a valid numeric value for \"Job Start Minute\" for schedule number : " + idx);
					}
					jobSchedule.scheduleStartMinute = Integer.valueOf(jobStartMinuteStr);


					String jobEndHourStr = request.getParameter("synclite-job-scheduler-end-hour-" + idx);
					try {
						if (Integer.valueOf(jobEndHourStr) == null) {
							throw new ServletException("Please specify a valid numeric value for \"Job End Hour\" for schedule number : " + idx);
						} else if ((Integer.valueOf(jobEndHourStr)) < 0 || (Integer.valueOf(jobEndHourStr) > 24)) {
							throw new ServletException("Please specify a value between 0 and 24 for \"Job End Hour\" for schedule number : " + idx);
						}

						if (Integer.valueOf(jobEndHourStr) < Integer.valueOf(jobStartHourStr)) {
							throw new ServletException("Specified \"Job End Hour\" is earlier than the \"Job Start Hour\" for schedule number : " + idx);
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a valid numeric value for \"Job End Hour\" for schedule number : " + idx);
					}
					jobSchedule.scheduleEndHour = Integer.valueOf(jobEndHourStr);

					String jobEndMinuteStr = request.getParameter("synclite-job-scheduler-end-minute-" + idx);
					try {
						if (Integer.valueOf(jobEndMinuteStr) == null) {
							throw new ServletException("Please specify a valid numeric value for \"Job End Minute\" for schedule number : " + idx);
						} else if ((Integer.valueOf(jobEndMinuteStr)) < 0 || (Integer.valueOf(jobEndMinuteStr) > 60)) {
							throw new ServletException("Please specify a value between 0 and 24 for \"Job End Minute\" for schedule number : " + idx);
						}
						if (Integer.valueOf(jobEndHourStr) == Integer.valueOf(jobStartHourStr)) {
							if (Integer.valueOf(jobEndMinuteStr) < Integer.valueOf(jobStartMinuteStr)) {
								throw new ServletException("Specified \"Job End Hour:Minute \" is earlier than the \"Job Start Hour:Minute\" for schedule number : " + idx);
							}
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a valid numeric value for \"Job End Minute\" for schedule number : " + idx);
					}
					jobSchedule.scheduleEndMinute = Integer.valueOf(jobEndMinuteStr);


					String jobRunDurationStr = request.getParameter("synclite-job-scheduler-job-run-duration-" + idx);
					try {
						if (Integer.valueOf(jobRunDurationStr) == null) {
							throw new ServletException("Please specify a non-negative numeric value for \"Job Run Duration\" for schedule number : " + idx);
						} else if (Integer.valueOf(jobRunDurationStr) < 0) {
							throw new ServletException("Please specify a non-negative numeric value for \"Job Run Duration\" for schedule number : " + idx);
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a non-negative numeric value for \"Job Run Duration\" for schedule number : " + idx);
					}
					jobSchedule.jobRunDuration = Integer.valueOf(jobRunDurationStr);

					String jobRunDurationUnit = request.getParameter("synclite-job-scheduler-job-run-duration-unit-" + idx);
					jobSchedule.jobRunDurationUnit = jobRunDurationUnit;

					String jobRunIntervalStr = request.getParameter("synclite-job-scheduler-job-run-interval-" + idx);
					try {
						if (Integer.valueOf(jobRunIntervalStr) == null) {
							throw new ServletException("Please specify a non-negative numeric value for \"Job Run Interval\" for schedule number : " + idx);
						} else if (Integer.valueOf(jobRunIntervalStr) < 0) {
							throw new ServletException("Please specify a non-negative numeric value for \"Job Run Interval\" for schedule number : " + idx);
						}
					} catch (NumberFormatException e) {
						throw new ServletException("Please specify a non-negative numeric value for \"Job Run Interval\" for schedule number : " + idx);
					}
					jobSchedule.jobRunInterval = Integer.valueOf(jobRunIntervalStr);

					String jobRunIntervalUnit = request.getParameter("synclite-job-scheduler-job-run-interval-unit-" + idx);
					jobSchedule.jobRunIntervalUnit = jobRunIntervalUnit;

					String jobSubType = request.getParameter("synclite-job-scheduler-job-sub-type-" + idx);
					
					if (jobType.equals("QREADER") || jobType.equals("CONSOLIDATOR")) {
						if (! jobSubType.equals("READ")) {
							throw new ServletException("Job sub-type " + jobSubType + " invalid for job type : " + jobType);
						}
					}
					jobSchedule.jobSubType = jobSubType;
					
					
					jobSchedules.add(jobSchedule);
					jobSchedulesJSONArr.put(jobSchedule.getJSONObject());
				}

				//Write out configurations to scheduler conf file.
				
				writeConfigurations(confPath, jobSchedulesJSONArr);
				
				Scheduler scheduler = (Scheduler) request.getSession().getAttribute("syncite-job-starter-scheduler");
				if (scheduler != null) {
					scheduler.clear();
					if (!scheduler.isShutdown()) {
						scheduler.shutdown();
					}
					request.getSession().setAttribute("syncite-job-starter-scheduler", null);
				}

				SchedulerFactory schedulerFactory = new StdSchedulerFactory();
				scheduler = schedulerFactory.getScheduler();
				request.getSession().setAttribute("syncite-job-starter-scheduler", scheduler);

				for (int idx=1 ; idx <= numSchedules ; ++idx) {		

					JobSchedule jobSchedule = jobSchedules.get(idx-1);
					
					String jobName = jobSchedule.jobName;
					String jobType = jobSchedule.jobType;
					String jobSubType = jobSchedule.jobSubType;
					long scheduleID = jobSchedule.scheduleID;
					Integer jobRunDurationS = getDurationInSeconds(jobSchedule.jobRunDuration, jobSchedule.jobRunDurationUnit);
					long scheduleStartTime = getTimeInMillis(jobSchedule.scheduleStartHour, jobSchedule.scheduleStartMinute);
					long scheduleEndTime = getTimeInMillis(jobSchedule.scheduleStartHour, jobSchedule.scheduleEndMinute);
					Integer jobRunIntervalS= getDurationInSeconds(jobSchedule.jobRunInterval, jobSchedule.jobRunIntervalUnit); 
							
					JobDataMap jobDataMap = new JobDataMap();
					jobDataMap.put("session", request.getSession());
					jobDataMap.put("globalTracer", this.globalTracer);
					jobDataMap.put("scheduleID", scheduleID);
					jobDataMap.put("jobName", jobName);
					jobDataMap.put("jobType", jobType);
					jobDataMap.put("jobRunDurationS", jobRunDurationS);
					jobDataMap.put("jobSubType", jobSubType);
					jobDataMap.put("scheduleStartTime", scheduleStartTime);
					jobDataMap.put("scheduleEndTime", scheduleEndTime);
					jobDataMap.put("jobRunIntervalS", jobRunIntervalS);
					
					JobDetail job = JobBuilder.newJob(JobStarter.class)
								.withIdentity("syncLiteJobStarter-" + idx, "syncLiteJobStarterGroup-" + idx)
								.usingJobData(jobDataMap)
								.build();
					/*
					Trigger trigger = TriggerBuilder.newTrigger()
							.withIdentity("yourTrigger", "group1")
							//.withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * 1/1 * ? *")) // Example: Run every 1 minutes
							.withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ?"))
							//.withSchedule(CronScheduleBuilder.) // Example: Run every 5 minutes
							.build();
					 */

					Trigger trigger = null;
					if (jobRunIntervalS > 0) {
						globalTracer.info("Creating a periodic trigger for start time : " + jobSchedule.scheduleStartHour + ":" + jobSchedule.scheduleStartMinute + " and end time : " + jobSchedule.scheduleEndHour + ":" + jobSchedule.scheduleEndMinute + " with run interval : " + jobRunIntervalS + " seconds");
						trigger = TriggerBuilder.newTrigger()
								.withIdentity("syncLiteJobStarter-" + idx, "syncLiteJobStarterGroup-" + idx)
								.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
										.startingDailyAt(
												TimeOfDay.hourAndMinuteOfDay(
														Integer.valueOf(jobSchedule.scheduleStartHour), 
														Integer.valueOf(jobSchedule.scheduleStartMinute)
														)
												)
										.endingDailyAt(
												TimeOfDay.hourAndMinuteOfDay(
														Integer.valueOf(jobSchedule.scheduleEndHour), 
														Integer.valueOf(jobSchedule.scheduleEndMinute)
														)
												)
										.withIntervalInSeconds(jobRunIntervalS)
										//.withMisfireHandlingInstructionFireAndProceed()									
										)
								.build();
					} else {
						globalTracer.info("Creating a periodic trigger for start time : " + jobSchedule.scheduleStartHour + ":" + jobSchedule.scheduleStartMinute + " and end time : " + jobSchedule.scheduleEndHour + ":" + jobSchedule.scheduleEndMinute + " with run interval : " + jobRunIntervalS + " seconds");
						trigger = TriggerBuilder.newTrigger()
								.withIdentity("syncLiteJobStarter-" + idx, "syncLiteJobStarterGroup-" + idx)
								.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
										.startingDailyAt(
												TimeOfDay.hourAndMinuteOfDay(
														Integer.valueOf(jobSchedule.scheduleStartHour), 
														Integer.valueOf(jobSchedule.scheduleStartMinute)
														)
												)
										.endingDailyAt(
												TimeOfDay.hourAndMinuteOfDay(
														Integer.valueOf(jobSchedule.scheduleEndHour), 
														Integer.valueOf(jobSchedule.scheduleEndMinute)
														)
												)
										//.withMisfireHandlingInstructionFireAndProceed()									
										)
								.build();

						/*
					 	//globalTracer.info("Creating one time trigger for start time : " + jobSchedule.scheduleStartHour + " : " + jobSchedule.scheduleStartMinute);
						trigger = TriggerBuilder.newTrigger()
								.withIdentity("syncLiteJobStarter-" + idx, "syncLiteJobStarterGroup-" + idx)
								.startAt(DateBuilder.todayAt(
										Integer.valueOf(jobSchedule.scheduleStartHour), 
										Integer.valueOf(jobSchedule.scheduleStartMinute),
										0
										)										
										).build();
						 */
					}

					scheduler.scheduleJob(job, trigger);
					this.globalTracer.info("Added scheduled job");
				}
				scheduler.start();
				this.globalTracer.info("Started schedule");

				//Refresh schedule counts
				int totalNumSchedules = 0;
				HashMap<String, JobInfo> jobInfoMap = (HashMap<String, JobInfo>) request.getSession().getAttribute("jobInfoMap");				
				if (jobInfoMap != null) {
					for (JobInfo ji : jobInfoMap.values()) {
						ji.numSchedules = 0;
					}				
					for (JobSchedule js : jobSchedules) {
						String key = js.jobName + ":" + js.jobType;
						if (jobInfoMap.containsKey(key)) {
							++jobInfoMap.get(key).numSchedules;
							++totalNumSchedules;
						}
					}
					request.getSession().setAttribute("jobInfoMap", jobInfoMap);
					request.getSession().setAttribute("totalNumSchedules", totalNumSchedules);
				}
				response.sendRedirect("dashboard.jsp");
			}
		} catch (Exception e) {
			String errorMsg = e.getMessage();
			this.globalTracer.error("Failed to configure and schedule job : " + e.getMessage(), e);
			request.getRequestDispatcher("configureScheduler.jsp?errorMsg=" + errorMsg).forward(request, response);
		}
	}

	private Integer getDurationInSeconds(Integer val, String unit) {
		switch (unit) {
		case "SECONDS":
			return val;
		case "MINUTES":
			return val * 60;
		case "HOURS":
			return val * 60 * 60;			
		}
		return 0;
	}

	private void writeConfigurations(Path confPath, JSONArray jobSchedulesJSONArr) throws ServletException {
		try {
			Files.writeString(confPath, jobSchedulesJSONArr.toString(1));
		} catch (Exception e) {
			globalTracer.error("Failed to write configurations to schedules json file : " + confPath + " : " + e.getMessage(), e);
			throw new ServletException("Failed to write configurations to schedules json file : " + confPath + " : " + e.getMessage(), e);
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
	
	private void setupSchedulerStatsFile(String schedulerStatsPath) throws ServletException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch(Exception e) {
			throw new ServletException("Failed to load sqlite JDBC driver : " + e.getMessage(), e);
		}
		String url = "jdbc:sqlite:" + schedulerStatsPath;
		String createTableSql = "CREATE TABLE IF NOT EXISTS statistics(schedule_id long, trigger_id long, job_name text, job_type text, job_sub_type text, schedule_start_time long, schedule_end_time long, job_run_interval_s int, job_run_duration_s int, job_start_time long, job_start_status text, job_start_status_description text, job_stop_time long, job_stop_status text, job_stop_status_description text, PRIMARY KEY(schedule_id, trigger_id))";
		try (Connection conn = DriverManager.getConnection(url)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute(createTableSql);
			}
		} catch (Exception e) {
			String errorMsg = "Failed to create scheduler statistics table in scheduler statistics db file : " + schedulerStatsPath  + e.getMessage();
			this.globalTracer.error(errorMsg, e);
			throw new ServletException(errorMsg, e);
		}
		
	}

	private long getTimeInMillis(Integer hour, Integer minute) {
      // Get today's date at 12:00 AM
      LocalDateTime todayMidnight = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

      // Add given hour and minute
      LocalDateTime targetDateTime = todayMidnight.plusHours(hour).plusMinutes(minute);

      // Convert to milliseconds since the epoch
      long millisSinceEpoch = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      return millisSinceEpoch;
	}
}
