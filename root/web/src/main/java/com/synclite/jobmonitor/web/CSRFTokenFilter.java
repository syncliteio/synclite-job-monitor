package com.synclite.jobmonitor.web;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter("/*")
public class CSRFTokenFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// No initialization needed.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		setSecurityHeaders(httpResponse);

		HttpSession session = httpRequest.getSession(true);
		String csrfToken = (String) session.getAttribute("csrfToken");
		if (csrfToken == null || csrfToken.isBlank()) {
			csrfToken = UUID.randomUUID().toString();
			session.setAttribute("csrfToken", csrfToken);
		}

		if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
			String submittedToken = httpRequest.getParameter("csrfToken");
			if (submittedToken == null || !csrfToken.equals(submittedToken)) {
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
						"Request rejected due to missing or invalid CSRF token.");
				return;
			}
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// No resources to release.
	}

	private void setSecurityHeaders(HttpServletResponse response) {
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Content-Security-Policy",
				"default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
	}
}