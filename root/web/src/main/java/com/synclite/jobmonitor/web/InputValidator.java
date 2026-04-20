package com.synclite.jobmonitor.web;

import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public final class InputValidator {

	private static final Pattern SAFE_JOB_NAME = Pattern.compile("^[a-zA-Z0-9_-]{1,16}$");

	private InputValidator() {
	}

	public static String requireParameter(HttpServletRequest request, String name, String displayName)
			throws ServletException {
		String value = request.getParameter(name);
		if (value == null || value.trim().isEmpty()) {
			throw new ServletException("\"" + displayName + "\" must be specified");
		}
		return value.trim();
	}

	public static String requireJobName(HttpServletRequest request, String name, String displayName)
			throws ServletException {
		String value = requireParameter(request, name, displayName);
		if (!SAFE_JOB_NAME.matcher(value).matches()) {
			throw new ServletException("Specified " + displayName
					+ " is invalid. Allowed characters are letters, numbers, underscore, and hyphen (max 16 characters).");
		}
		return value;
	}

	public static int requireIntInRange(HttpServletRequest request, String name, String displayName, int min, int max)
			throws ServletException {
		String raw = requireParameter(request, name, displayName);
		int value;
		try {
			value = Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			throw new ServletException("\"" + displayName + "\" must be a valid integer", e);
		}

		if (value < min || value > max) {
			throw new ServletException("\"" + displayName + "\" must be between " + min + " and " + max);
		}
		return value;
	}

	public static long requireLong(HttpServletRequest request, String name, String displayName) throws ServletException {
		String raw = requireParameter(request, name, displayName);
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ServletException("\"" + displayName + "\" must be a valid number", e);
		}
	}

	public static String requireEnum(HttpServletRequest request, String name, String displayName, Set<String> allowed)
			throws ServletException {
		String value = requireParameter(request, name, displayName);
		if (!allowed.contains(value)) {
			throw new ServletException("Invalid value for \"" + displayName + "\"");
		}
		return value;
	}
}