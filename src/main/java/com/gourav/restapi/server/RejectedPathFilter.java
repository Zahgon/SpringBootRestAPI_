package com.gourav.restapi.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Refuses request paths the API has never accepted, before any response shaping happens.
 *
 * <p>A path with an empty segment is turned away with a status and nothing else — not even the
 * header block every routed response carries — which is what callers have always observed.
 */
public class RejectedPathFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String path = ((HttpServletRequest) request).getRequestURI();
		if (path.startsWith("//")) {
			((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		chain.doFilter(request, response);
	}
}
