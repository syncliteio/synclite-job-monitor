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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;


public class JobStarter implements Job {
	private Logger globalTracer;

	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		String jobName= "";
		String jobType = "";
		String jobSubType = "";
		Integer jobRunDurationS = 0;
		Long scheduleID = 0L;			
		Long scheduleStartTime = 0L;	
		Long scheduleEndTime = 0L;		
		Integer jobRunIntervalS = 0;
		Long triggerId = 0L;
		Long jobStartTime = System.currentTimeMillis();
		String jobStartStatus = "";
		String jobStartStatusDescription = "";
		String jobComponentName = "";
		
		String syncLiteDeviceDir = null;
		String jobRootPath = null;
		String jobConfPath = null;
		String schedulerStatsPath = null;
		String jobScriptName= "";
		Path jobScriptPath = null;
		String jobVariablesScriptName = "";
		String[] jobCmdArray = null;
		String corePath = ""; 
		
		try {
			
	        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

			HttpSession session = (HttpSession) jobDataMap.get("session");
			LinkedHashMap<String, JobInfo> jobInfoMap = (LinkedHashMap<String, JobInfo>) session.getAttribute("jobInfoMap");
			globalTracer = (Logger) jobDataMap.get("globalTracer");
			scheduleID = (Long) jobDataMap.get("scheduleID");
			jobName= (String) jobDataMap.get("jobName");
			jobType = (String) jobDataMap.get("jobType");
			jobSubType = (String) jobDataMap.get("jobSubType");
			jobRunDurationS = (Integer) jobDataMap.get("jobRunDurationS");
			scheduleStartTime = (Long) jobDataMap.get("scheduleStartTime");	
			scheduleEndTime = (Long) jobDataMap.get("scheduleEndTime");		
			jobRunIntervalS = (Integer) jobDataMap.get("jobRunIntervalS");

			syncLiteDeviceDir = session.getAttribute("synclite-device-dir").toString();
			jobRootPath = jobInfoMap.get(jobName + ":" + jobType).rootPath.toString();
			jobConfPath = jobInfoMap.get(jobName + ":" + jobType).confPath.toString();
			jobComponentName = jobInfoMap.get(jobName + ":" + jobType).componentName;

			schedulerStatsPath = Path.of(syncLiteDeviceDir, "synclite_job_scheduler_statistics.db").toString();
			
			switch(jobType) {
			case "DBREADER":
				String jobCmd = "read";
				if (jobSubType.equals("DELETE-SYNC")) {
					jobCmd = "delete-sync";
				}
				String scriptName = "";
				if (isWindows()) {
					jobVariablesScriptName = "synclite-dbreader-variables.bat";
					scriptName = "synclite-dbreader.bat";
				} else {
					jobVariablesScriptName = "synclite-dbreader-variables.sh";
					scriptName = "synclite-dbreader.sh";	
				}
				corePath = session.getAttribute("dbReaderCorePath").toString();
				jobScriptPath = Path.of(corePath, scriptName);
				jobCmdArray = new String[] {jobScriptPath.toString(), jobCmd, "--db-dir", jobRootPath, "--config", jobConfPath};				
				break;
				
			case "QREADER":
				jobCmd = "read";
				scriptName = "";
				if (isWindows()) {
					jobVariablesScriptName = "synclite-qreader-variables.bat";
					scriptName = "synclite-qreader.bat";
				} else {
					jobVariablesScriptName = "synclite-qreader-variables.sh";
					scriptName = "synclite-qreader.sh";				
				}
				corePath = session.getAttribute("qReaderCorePath").toString();
				jobScriptPath = Path.of(corePath, scriptName);
				jobCmdArray = new String[] {jobScriptPath.toString(), jobCmd, "--db-dir", jobRootPath, "--config", jobConfPath};				
				break;
				
			case "CONSOLIDATOR":
				scriptName = "";
				if (isWindows()) {
					jobVariablesScriptName = "synclite-variables.bat";
					scriptName = "synclite-consolidator.bat";
				} else {
					jobVariablesScriptName = "synclite-qreader-variables.sh";
					scriptName = "synclite-consolidator.sh";				
				}
				corePath = session.getAttribute("consolidatorCorePath").toString();
				jobScriptPath = Path.of(corePath, scriptName);
				jobCmdArray = new String[] {jobScriptPath.toString(), "sync", "--work-dir", jobRootPath, "--config", jobConfPath};
				break;
			}
			
			
			globalTracer.info("Scheduler attempting to trigger job : " + jobName + ", job type :" + jobType + ", sub type : " + jobSubType +", job duration : " + jobRunDurationS + ", schedule ID : " + scheduleID);
			long currentJobPID = 0;
			Process jpsProc;
			if (isWindows()) {
				String javaHome = System.getenv("JAVA_HOME");			
				String scriptPath = "jps";
				if (javaHome != null) {
					scriptPath = javaHome + "\\bin\\jps";
				} else {
					scriptPath = "jps";
				}
				String[] cmdArray = {scriptPath, "-l", "-m"};
				jpsProc = Runtime.getRuntime().exec(cmdArray);
			} else {
				String javaHome = System.getenv("JAVA_HOME");			
				String scriptPath = "jps";
				if (javaHome != null) {
					scriptPath = javaHome + "/bin/jps";
				} else {
					scriptPath = "jps";
				}
				String[] cmdArray = {scriptPath, "-l", "-m"};
				jpsProc = Runtime.getRuntime().exec(cmdArray);
			}
			BufferedReader stdout = new BufferedReader(new InputStreamReader(jpsProc.getInputStream()));
			String line = stdout.readLine();
			while (line != null) {
				if (line.contains(jobComponentName) && line.contains(jobRootPath)) {
					currentJobPID = Long.valueOf(line.split(" ")[0]);
				}
				line = stdout.readLine();
			}

			triggerId = System.currentTimeMillis();
			jobStartStatus = "";
			jobStartStatusDescription = "";
			//Start if the job is not found
			if(currentJobPID == 0) {
				//Get env variable 
				String jvmArgs = "";
				if (session.getAttribute("jvm-arguments") != null) {
					jvmArgs = session.getAttribute("jvm-arguments").toString();
				}	
				Process p;
				if (isWindows()) {
					if (!jvmArgs.isBlank()) {
						try {
							//Delete and re-create a file variables.bat under scriptPath and set the variable JVM_ARGS
							Path varFilePath = Path.of(corePath, jobVariablesScriptName);
							if (Files.exists(varFilePath)) {
								Files.delete(varFilePath);
							}
							String varString = "set \"JVM_ARGS=" + jvmArgs + "\""; 
							Files.writeString(varFilePath, varString, StandardOpenOption.CREATE);
						} catch (Exception e) {
							throw new ServletException("Failed to write jvm-arguments to " + jobVariablesScriptName + " file : " + e.getMessage(), e);
						}
					}
					p = Runtime.getRuntime().exec(jobCmdArray);						
					jobStartTime = System.currentTimeMillis();
					jobStartStatus = "SUCCESS";
				} else {
					if (!jvmArgs.isBlank()) {
						try {
							//Delete and re-create a file variables.sh under scriptPath and set the variable JVM_ARGS
							Path varFilePath = Path.of(corePath, jobVariablesScriptName);
							String varString = "JVM_ARGS=\"" + jvmArgs + "\"";
							if (Files.exists(varFilePath)) {
								Files.delete(varFilePath);
							}
							Files.writeString(varFilePath, varString, StandardOpenOption.CREATE);
							Set<PosixFilePermission> perms = Files.getPosixFilePermissions(varFilePath);
							perms.add(PosixFilePermission.OWNER_EXECUTE);
							Files.setPosixFilePermissions(varFilePath, perms);
						} catch (Exception e) {
							throw new ServletException("Failed to write jvm-arguments to " + jobVariablesScriptName + " file : " + e.getMessage(), e);
						}
					}

					// Get the current set of script permissions
					Set<PosixFilePermission> perms = Files.getPosixFilePermissions(jobScriptPath);
					// Add the execute permission if it is not already set
					if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
						perms.add(PosixFilePermission.OWNER_EXECUTE);
						Files.setPosixFilePermissions(jobScriptPath, perms);
					}
					p = Runtime.getRuntime().exec(jobCmdArray);
					jobStartTime = System.currentTimeMillis();
					jobStartStatus = "SUCCESS";
				}
				
				
				if (jobRunDurationS > 0) {					
					Scheduler scheduler = context.getScheduler();
					
					JobDataMap argJobDataMap = new JobDataMap();
					argJobDataMap.put("session", session);
					argJobDataMap.put("globalTracer", this.globalTracer);
					argJobDataMap.put("scheduleID", scheduleID);
					argJobDataMap.put("triggerId", triggerId);
					argJobDataMap.put("jobRootDir", jobRootPath);
					argJobDataMap.put("jobComponentName", jobComponentName);
	
					Trigger trigger = TriggerBuilder.newTrigger()
			                .withIdentity("syncLiteJobStopper-" + scheduleID, "syncLiteJobStopperGroup-" + scheduleID)
			                .startAt(DateBuilder.futureDate(jobRunDurationS, DateBuilder.IntervalUnit.SECOND)) 
			                .build();	
				
					JobDetail job = JobBuilder.newJob(JobStopper.class)
							.withIdentity("syncLiteJobStopper-" + scheduleID, "syncLiteJobStopperGroup-" + scheduleID)
							.usingJobData(argJobDataMap)
							.build();
					
					JobKey jk= (JobKey) context.get("stopper-job-key");
					if (jk != null) {
						scheduler.deleteJob(jk);
					}
					context.put("stopper-job-key", job.getKey());
								
					scheduler.scheduleJob(job, trigger);
				}
				
				globalTracer.info("Scheduler triggered job successfully");
			} else {
				jobStartStatus = "SKIPPED";
				jobStartStatusDescription = "Job found running with PID : " + currentJobPID;
				globalTracer.info("Scheduler skipped to trigger job as job found running with PID : " + currentJobPID);
			}
		} catch (Exception e) {
			this.globalTracer.error("Failed to trigger scheduled DBReader job : " + e.getMessage(), e);
		} finally {
			if (schedulerStatsPath != null) {
				try {
					//Make an entry in the schedule report.
					addToSchedulerStats(schedulerStatsPath, scheduleID, triggerId, jobName, jobType, jobSubType, scheduleStartTime, scheduleEndTime, jobRunIntervalS, jobRunDurationS, jobStartTime, jobStartStatus, jobStartStatusDescription);
				} catch (Exception e) {
					this.globalTracer.error("Failed to log an etry in schedule statistics file : " + e.getMessage(), e);
				}			
			}
		}
	}


	private boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	private void addToSchedulerStats(String schedulerStatsPath, long scheduleID, long triggerId, String jobName, String jobType, String jobSubType, long scheduleStartTime, long scheduleEndTime, int jobRunIntervalS, int jobRunDurationS, long jobStartTime, String jobStartStatus, String jobStartStatusDescription) {	
		String url = "jdbc:sqlite:" + schedulerStatsPath;
		StringBuilder insertSqlBuilder = new StringBuilder();
		insertSqlBuilder.append("INSERT INTO statistics(schedule_id, trigger_id, job_name, job_type, job_sub_type, schedule_start_time, schedule_end_time, job_run_interval_s, job_run_duration_s, job_start_time, job_start_status, job_start_status_description, job_stop_time, job_stop_status, job_stop_status_description) VALUES (");
		insertSqlBuilder.append(scheduleID);
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(triggerId);
		insertSqlBuilder.append(",");
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(jobName);
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(",");		
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(jobType);
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(",");		
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(jobSubType);
		insertSqlBuilder.append("'");
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(scheduleStartTime);
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(scheduleEndTime);		
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(jobRunIntervalS);		
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(jobRunDurationS);		
		insertSqlBuilder.append(",");
		insertSqlBuilder.append(jobStartTime);
		insertSqlBuilder.append(",'");
		insertSqlBuilder.append(jobStartStatus);
		insertSqlBuilder.append("','");
		insertSqlBuilder.append(jobStartStatusDescription);
		insertSqlBuilder.append("',");
		insertSqlBuilder.append(0);
		insertSqlBuilder.append(",");
		insertSqlBuilder.append("''");
		insertSqlBuilder.append(",");
		insertSqlBuilder.append("'')");

		try (Connection conn = DriverManager.getConnection(url)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute(insertSqlBuilder.toString());
			}
		} catch (Exception e) {
			this.globalTracer.error("Failed to log an entry of job trigger event in the stats file : " + schedulerStatsPath + " : sql : "  + insertSqlBuilder.toString() + ", error : " + e.getMessage(), e);
		}
	}

}
