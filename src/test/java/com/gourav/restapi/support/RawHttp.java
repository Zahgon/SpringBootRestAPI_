package com.gourav.restapi.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A minimal HTTP/1.1 client that speaks straight to a socket.
 *
 * <p>The response contract of this service is made of more than status codes and JSON: header
 * order, header casing and the choice between {@code Content-Length} and chunked transfer are all
 * observable. A pooling client normalises those away, so the tests read the bytes themselves.
 */
public final class RawHttp {

	private RawHttp() {
	}

	public static Response request(int port, String method, String path, Map<String, String> headers, String body) {
		StringBuilder request = new StringBuilder();
		request.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
		request.append("Host: localhost:").append(port).append("\r\n");
		headers.forEach((name, value) -> request.append(name).append(": ").append(value).append("\r\n"));
		byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
		if (body != null) {
			request.append("Content-Length: ").append(payload.length).append("\r\n");
		}
		request.append("Connection: close\r\n\r\n");

		try (Socket socket = new Socket("127.0.0.1", port)) {
			socket.setSoTimeout(20_000);
			OutputStream out = socket.getOutputStream();
			out.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
			out.write(payload);
			out.flush();
			return read(socket.getInputStream());
		} catch (IOException e) {
			throw new IllegalStateException(method + " " + path + " failed", e);
		}
	}

	public static Response get(int port, String path) {
		return request(port, "GET", path, Map.of(), null);
	}

	public static Response get(int port, String path, Map<String, String> headers) {
		return request(port, "GET", path, headers, null);
	}

	public static Response json(int port, String method, String path, String body) {
		return request(port, method, path, Map.of("Content-Type", "application/json"), body);
	}

	private static Response read(InputStream in) throws IOException {
		ByteArrayOutputStream head = new ByteArrayOutputStream();
		int matched = 0;
		int b;
		while (matched < 4 && (b = in.read()) != -1) {
			head.write(b);
			matched = switch (matched) {
				case 0, 2 -> b == '\r' ? matched + 1 : 0;
				case 1, 3 -> b == '\n' ? matched + 1 : 0;
				default -> 0;
			};
		}
		String[] lines = head.toString(StandardCharsets.ISO_8859_1).split("\r\n");
		String statusLine = lines.length == 0 ? "" : lines[0];
		List<String> names = new ArrayList<>();
		Map<String, List<String>> values = new LinkedHashMap<>();
		for (int i = 1; i < lines.length; i++) {
			int colon = lines[i].indexOf(':');
			if (colon < 0) {
				continue;
			}
			String name = lines[i].substring(0, colon);
			String value = lines[i].substring(colon + 1).trim();
			names.add(name);
			values.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(value);
		}

		byte[] body;
		List<String> encoding = values.get("transfer-encoding");
		List<String> length = values.get("content-length");
		if (encoding != null && encoding.get(0).contains("chunked")) {
			body = readChunked(in);
		} else if (length != null) {
			body = in.readNBytes(Integer.parseInt(length.get(0)));
		} else {
			body = in.readAllBytes();
		}
		int status = statusLine.startsWith("HTTP/1.1 ") ? Integer.parseInt(statusLine.substring(9, 12)) : -1;
		return new Response(statusLine, status, names, values, new String(body, StandardCharsets.UTF_8));
	}

	private static byte[] readChunked(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (true) {
			String size = readLine(in);
			int chunk = Integer.parseInt(size.split(";")[0].trim(), 16);
			if (chunk == 0) {
				return out.toByteArray();
			}
			out.write(in.readNBytes(chunk));
			readLine(in);
		}
	}

	private static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream line = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			if (b == '\n') {
				break;
			}
			if (b != '\r') {
				line.write(b);
			}
		}
		return line.toString(StandardCharsets.ISO_8859_1);
	}

	/** One response, with the header names kept in the order they arrived on the wire. */
	public record Response(String statusLine, int status, List<String> headerNames,
			Map<String, List<String>> headers, String body) {

		public String header(String name) {
			List<String> found = headers.get(name.toLowerCase(Locale.ROOT));
			return found == null ? null : found.get(0);
		}

		public boolean has(String name) {
			return headers.containsKey(name.toLowerCase(Locale.ROOT));
		}

		/** The header names as they were written, which is what the previous framework pinned. */
		public List<String> orderedNames() {
			return headerNames;
		}
	}
}
