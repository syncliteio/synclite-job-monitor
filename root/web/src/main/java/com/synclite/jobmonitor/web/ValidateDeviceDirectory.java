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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ValidateJobConfiguration
 */
@WebServlet("/validateDeviceDirectory")
public class ValidateDeviceDirectory extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	/**
	 * Default constructor. 
	 */
	public ValidateDeviceDirectory() {
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**	  
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {

			String syncLiteDeviceDirStr = request.getParameter("synclite-device-dir").toString();
			Path syncLiteDeviceDir;
			if ((syncLiteDeviceDirStr== null) || syncLiteDeviceDirStr.trim().isEmpty()) {
				throw new ServletException("\"SyncLite Device Directory Path\" must be specified");
			} else {
				syncLiteDeviceDir = Path.of(syncLiteDeviceDirStr);
				if (! Files.exists(syncLiteDeviceDir)) {
					try {
						Files.createDirectories(syncLiteDeviceDir);
					} catch (Exception e) {
						throw new ServletException("Failed to create device directory : " + syncLiteDeviceDirStr + " : " + e.getMessage(), e);
					}
				}
				if (! Files.exists(syncLiteDeviceDir)) {
					throw new ServletException("Specified \"SyncLite Device Directory Path\" : " + syncLiteDeviceDir + " does not exist, please specify a valid path.");
				}
			}
			
			if (! syncLiteDeviceDir.toFile().canRead()) {
				throw new ServletException("Specified \"SyncLite Device Directory Path\" does not have read permission");
			}

			if (! syncLiteDeviceDir.toFile().canWrite()) {
				throw new ServletException("Specified \"SyncLite Device Directory Path\" does not have write permission");
			}

			request.getSession().setAttribute("synclite-device-dir", syncLiteDeviceDirStr);

			response.sendRedirect("jobSummary.jsp");
		} catch (Exception e) {
			String errorMsg = e.getMessage();
			request.getRequestDispatcher("selectDeviceDirectory.jsp?errorMsg=" + errorMsg).forward(request, response);
			throw new ServletException(e);
		}
	}
}


