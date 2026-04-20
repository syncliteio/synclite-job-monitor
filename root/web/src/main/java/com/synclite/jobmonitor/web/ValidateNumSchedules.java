package com.synclite.jobmonitor.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/validateNumSchedules")
public class ValidateNumSchedules extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("configureNumSchedules.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			int numSchedules = InputValidator.requireIntInRange(
					request,
					"synclite-dbreader-scheduler-num-schedules",
					"Number of Schedules",
					1,
					500);
			response.sendRedirect("configureScheduler.jsp?numSchedules=" + numSchedules);
		} catch (Exception e) {
			request.setAttribute("errorMsg", e.getMessage());
			request.getRequestDispatcher("configureNumSchedules.jsp").forward(request, response);
		}
	}
}