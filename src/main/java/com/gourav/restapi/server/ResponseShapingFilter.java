package com.gourav.restapi.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the response headers the API has always sent, in the order it has always sent them.
 *
 * <p>The header set (three {@code Vary} entries plus six security headers) used to come from the
 * container's security filter chain; the order used to be an accident of that chain. Both are part
 * of the observable contract, so both are reproduced here. Headers written by the application are
 * buffered until the response commits and then emitted in {@link #ORDER}, because a JAX-RS response
 * filter cannot control the order in which the container serialises them.
 */
public class ResponseShapingFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseShapingFilter.class);

	/** The route-specific headers, which sit between the {@code Vary} block and the security block. */
	private static final List<String> ORDER = List.of(
			"access-control-allow-origin",
			"access-control-allow-methods",
			"access-control-allow-headers",
			"access-control-max-age",
			"allow",
			"accept",
			"accept-patch",
			"location",
			"last-modified",
			"accept-ranges");

	/** Framing is the container's to decide; it is emitted after everything the application wrote. */
	private static final Set<String> FRAMING = Set.of("content-length", "transfer-encoding", "date");

	/** Buffered header names are normalised to lower case, so the marker is looked up that way too. */
	private static final String SUPPRESS_CONTENT_TYPE_KEY =
			InternalHeaders.SUPPRESS_CONTENT_TYPE.toLowerCase(Locale.ROOT);

	private static final ZoneId GMT = ZoneId.of("GMT");

	private static final DateTimeFormatter RFC_1123 =
			DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		boolean bodyElided = "HEAD".equals(((HttpServletRequest) request).getMethod());
		BufferedResponse buffered = new BufferedResponse((HttpServletResponse) response, bodyElided);
		try {
			chain.doFilter(request, buffered);
		} finally {
			buffered.commitHeaders();
		}
	}

	/**
	 * Collects every header the application sets and replays them in canonical order at commit time.
	 */
	private static final class BufferedResponse extends HttpServletResponseWrapper {

		private final Map<String, List<String>> headers = new LinkedHashMap<>();
		private final boolean bodyElided;
		private boolean committed;
		private ServletOutputStream stream;
		private PrintWriter writer;

		private BufferedResponse(HttpServletResponse delegate, boolean bodyElided) {
			super(delegate);
			this.bodyElided = bodyElided;
		}

		private void commitHeaders() {
			if (committed) {
				return;
			}
			committed = true;
			HttpServletResponse target = (HttpServletResponse) getResponse();
			boolean suppressContentType = headers.remove(SUPPRESS_CONTENT_TYPE_KEY) != null;
			if (suppressContentType) {
				headers.remove("content-type");
			}
			target.setHeader("Vary", "Origin");
			target.addHeader("Vary", "Access-Control-Request-Method");
			target.addHeader("Vary", "Access-Control-Request-Headers");
			headers.remove("vary");
			for (String name : ORDER) {
				emit(target, name);
			}
			emitSecurityHeaders(target);
			boolean described = headers.containsKey("content-type");
			emit(target, "content-type");
			emit(target, "content-language");
			for (String name : new ArrayList<>(headers.keySet())) {
				if (FRAMING.contains(name)) {
					continue;
				}
				emit(target, name);
			}
			emit(target, "content-length");
			forceStreamedFraming(target, described);
		}

		/**
		 * A described answer is framed as a stream whether or not the caller asked for its body. The
		 * container would otherwise measure the bytes it never wrote and announce a length of zero.
		 */
		private void forceStreamedFraming(HttpServletResponse target, boolean described) {
			if (!bodyElided || !described || headers.containsKey("content-length")) {
				return;
			}
			try {
				target.flushBuffer();
			} catch (IOException e) {
				LOGGER.debug("Could not frame a body-less answer: {}", e.getMessage());
			}
		}

		private void emitSecurityHeaders(HttpServletResponse target) {
			target.setHeader("X-Content-Type-Options", "nosniff");
			target.setHeader("X-XSS-Protection", "0");
			target.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
			target.setHeader("Pragma", "no-cache");
			target.setHeader("Expires", "0");
			target.setHeader("X-Frame-Options", "DENY");
		}

		private void emit(HttpServletResponse target, String name) {
			List<String> values = headers.remove(name);
			if (values == null) {
				return;
			}
			for (String value : values) {
				target.addHeader(canonical(name), value);
			}
		}

		private static String canonical(String lowerCaseName) {
			StringBuilder builder = new StringBuilder(lowerCaseName.length());
			boolean upper = true;
			for (int i = 0; i < lowerCaseName.length(); i++) {
				char c = lowerCaseName.charAt(i);
				builder.append(upper ? Character.toUpperCase(c) : c);
				upper = c == '-';
			}
			return builder.toString();
		}

		private void store(String name, String value, boolean replace) {
			if (name == null) {
				return;
			}
			String key = name.toLowerCase(Locale.ROOT);
			if (committed) {
				HttpServletResponse target = (HttpServletResponse) getResponse();
				if (replace) {
					target.setHeader(name, value);
				} else {
					target.addHeader(name, value);
				}
				return;
			}
			if (replace || !headers.containsKey(key)) {
				List<String> values = new ArrayList<>(1);
				values.add(value);
				headers.put(key, values);
			} else {
				headers.get(key).add(value);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			store(name, value, true);
		}

		@Override
		public void addHeader(String name, String value) {
			store(name, value, false);
		}

		@Override
		public void setIntHeader(String name, int value) {
			store(name, Integer.toString(value), true);
		}

		@Override
		public void addIntHeader(String name, int value) {
			store(name, Integer.toString(value), false);
		}

		@Override
		public void setContentType(String type) {
			store("Content-Type", type, true);
		}

		@Override
		public void setContentLength(int length) {
			store("Content-Length", Integer.toString(length), true);
		}

		@Override
		public void setContentLengthLong(long length) {
			store("Content-Length", Long.toString(length), true);
		}

		@Override
		public void setDateHeader(String name, long date) {
			store(name, RFC_1123.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(date), GMT)), true);
		}

		@Override
		public void addDateHeader(String name, long date) {
			store(name, RFC_1123.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(date), GMT)), false);
		}

		@Override
		public String getContentType() {
			List<String> values = headers.get("content-type");
			return values == null ? null : values.get(0);
		}

		@Override
		public boolean containsHeader(String name) {
			return headers.containsKey(name.toLowerCase(Locale.ROOT)) || super.containsHeader(name);
		}

		@Override
		public String getHeader(String name) {
			List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
			return values == null ? super.getHeader(name) : values.get(0);
		}

		@Override
		public void setStatus(int sc) {
			super.setStatus(sc);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (stream == null) {
				commitHeaders();
				stream = new CommittingStream(super.getOutputStream());
			}
			return stream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (writer == null) {
				commitHeaders();
				writer = super.getWriter();
			}
			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			commitHeaders();
			super.flushBuffer();
		}

		@Override
		public void reset() {
			headers.clear();
			super.reset();
		}
	}

	/**
	 * Plain delegating stream; header commit already happened when it was created.
	 */
	private static final class CommittingStream extends ServletOutputStream {

		private final ServletOutputStream delegate;

		private CommittingStream(ServletOutputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public void write(int b) throws IOException {
			delegate.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			delegate.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setWriteListener(WriteListener listener) {
			delegate.setWriteListener(listener);
		}
	}
}
