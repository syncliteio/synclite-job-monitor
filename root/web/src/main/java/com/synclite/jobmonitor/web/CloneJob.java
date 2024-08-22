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
@WebServlet("/cloneJob")
public class CloneJob extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private Logger globalTracer;

	/**
	 * Default constructor. 
	 */
	public CloneJob() {
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
			String srcJobName = request.getParameter("src-job-name").toString();
			if (srcJobName.isBlank()) {
				throw new ServletException("Please specify a valid source job name.");			
			} else {
				//Check if specified jobName is in correct format
				if (srcJobName.length() > 16 ) {
					throw new ServletException("Source job name must be upto 16 characters in length");
				}
				if (!srcJobName.matches("[a-zA-Z0-9-_]+")) {
					throw new ServletException("Specified source job name is invalid. Allowed characters are alphanumeric characters or hyphens.");
				}
				
				Path srcJobPath = syncLiteDeviceDir.resolve(srcJobName);
				if (! Files.exists(srcJobPath)) {
					throw new ServletException("Source job path " + srcJobPath + " does not exist");
				}
			}
			
			String tgtJobName = request.getParameter("tgt-job-name").toString();
			if (tgtJobName.isBlank()) {
				throw new ServletException("Please specify a valid target job name.");			
			} else {
				//Check if specified jobName is in correct format
				if (tgtJobName.length() > 16 ) {
					throw new ServletException("Target job name must be upto 16 characters in length");
				}
				if (!tgtJobName.matches("[a-zA-Z0-9-_]+")) {
					throw new ServletException("Specified target job name is invalid. Allowed characters are alphanumeric characters or hyphens.");
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
			final Path srcJobPath = syncLiteDeviceDir.resolve(srcJobName);
			while (line != null) {
				if (line.contains(srcJobPath.toString())) {
					currentJobPID = Long.valueOf(line.split(" ")[0]);
				}
				line = stdout.readLine();
			}
			if(currentJobPID != 0) {
				String errorMessage = "Specified source job is running with Process ID : " + currentJobPID + ". Please stop the job and then run Clone Job";
				request.getRequestDispatcher("resetJob.jsp?errorMsg=" + errorMessage).forward(request, response);
			} else {
				this.globalTracer.info("Starting to clone Job : " + srcJobName + " to target job : " + tgtJobName + " under job directory : " + syncLiteDeviceDir);

				final Path tgtJobPath = syncLiteDeviceDir.resolve(tgtJobName);				
				try {
		            Files.walkFileTree(srcJobPath, new SimpleFileVisitor<Path>() {
		                @Override
		                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		                    Path targetDir = tgtJobPath.resolve(srcJobPath.relativize(dir));
		                    Files.createDirectories(targetDir);
		                    return FileVisitResult.CONTINUE;
		                }

		                @Override
		                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		                	Path tgtFilePath = tgtJobPath.resolve(srcJobPath.relativize(file));
		                    Files.copy(file, tgtFilePath, StandardCopyOption.REPLACE_EXISTING);
		                    //If it is a conf file. Search and replace job-name config value from source job name to target job name.
		                    
		                    if (tgtFilePath.toString().endsWith(".conf")) {
		                    	String content = Files.readString(tgtFilePath);
		                        // Construct the regular expression pattern
		                        String pattern = "job-name\\s=\\s" + Pattern.quote(srcJobName);
		                        // Compile the pattern
		                        Pattern regex = Pattern.compile(pattern);

		                        // Replace the substring
		                        String result = regex.matcher(content).replaceAll("job-name = " + tgtJobName);
		                        
		                        Files.writeString(tgtFilePath, result);
		                    }
		                    return FileVisitResult.CONTINUE;
		                }
		            });
		        } catch (IOException e) {
		            throw new ServletException("Failed to clone job with error : " + e.getMessage(), e);
				}
				this.globalTracer.info("Finished cloning Job : " + srcJobName + " to target job : " + tgtJobName + " under work directory : " + syncLiteDeviceDir);

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
