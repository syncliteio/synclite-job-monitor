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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * Servlet implementation class ValidateJobConfiguration
 */
@WebServlet("/deleteJob")
public class DeleteJob extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private Logger globalTracer;

	/**
	 * Default constructor. 
	 */
	public DeleteJob() {
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**	  
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			Path syncLiteDeviceDir = Path.of(request.getSession().getAttribute("synclite-device-dir").toString());
			String jobName = request.getParameter("job-name").toString();
			Path jobPath = syncLiteDeviceDir.resolve(jobName);
			if (jobName.isBlank()) {
				throw new ServletException("Please specify a valid job name");			
			} else {
				Path srcJobPath = syncLiteDeviceDir.resolve(jobName);
				if (! Files.exists(srcJobPath)) {
					throw new ServletException("Job directory path " + srcJobPath + " does not exist for specified job : " + jobName);
				}
			}
			
			initTracer(syncLiteDeviceDir);

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
				if (line.contains(jobPath.toString())) {
					currentJobPID = Long.valueOf(line.split(" ")[0]);
				}
				line = stdout.readLine();
			}
			if(currentJobPID != 0) {
				String errorMessage = "Specified job is running with Process ID : " + currentJobPID + ". Please stop the job and then run Delete Job";
				request.getRequestDispatcher("resetJob.jsp?errorMsg=" + errorMessage).forward(request, response);
			} else {
				this.globalTracer.info("Starting to delete Job : " + jobName + " with job directory : " + jobPath);

				Files.walkFileTree(jobPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						try {
							Files.delete(file);
						} catch (IOException e) {
							//Ignore
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						try {
							Files.delete(dir);
						} catch (IOException e) {
							//Ignore
						}
						return FileVisitResult.CONTINUE;
					}
				});

				// Delete the parent directory itself
				try {
					Files.deleteIfExists(jobPath);
				} catch (IOException e) {
					this.globalTracer.info("Failed to delete job directory : " + jobPath + " : " + e.getMessage(), e);
				}
			        
				this.globalTracer.info("Finished deleting Job : " + jobName + " with job directory : " + jobPath);

				response.sendRedirect("jobSummary.jsp");
			}
		} catch (Exception e) {
			//System.out.println("exception : " + e);
			String errorMsg = e.getMessage();

			if (this.globalTracer != null) {
				this.globalTracer.error("Failed cloning job with error : " + e.getMessage(), e);
			}

			request.getRequestDispatcher("cloneJob.jsp?errorMsg=" + errorMsg).forward(request, response);
			throw new ServletException(e);
		}
	}
	
	private boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	private final void initTracer(Path workDir) {
		this.globalTracer = Logger.getLogger(ValidateDeviceDirectory.class);
		if (this.globalTracer.getAppender("JobMonitorTracer") == null) {
			globalTracer.setLevel(Level.INFO);
			RollingFileAppender fa = new RollingFileAppender();
			fa.setName("consolidatorTracer");
			fa.setFile(workDir.resolve("synclite_jobmonitor.trace").toString());
			fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
			fa.setMaxBackupIndex(10);
			fa.setAppend(true);
			fa.activateOptions();
			globalTracer.addAppender(fa);
		}
	}
}
