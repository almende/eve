package com.almende.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public final class StreamingUtil {
	
	private StreamingUtil() {
	};
	
	/**
	 * Stream an input stream to a servlet response
	 * 
	 * @param inputStream
	 * @param mimeType
	 * @param response
	 * @throws IOException
	 */
	public static void streamBinaryData(final InputStream inputStream,
			final String mimeType, final HttpServletResponse response)
			throws IOException {
		final OutputStream os = response.getOutputStream();
		if (inputStream != null && os != null) {
			response.setContentType(mimeType);
			// TODO: use buffered streams?
			final byte[] buff = new byte[1024];
			int bytesRead;
			while (-1 != (bytesRead = inputStream.read(buff, 0, buff.length))) {
				os.write(buff, 0, bytesRead);
			}
		}
	}
	
	private static Map<String, String>	mimeTypes;
	static {
		mimeTypes = new HashMap<String, String>();
		mimeTypes.put("pdf", "application/pdf");
		mimeTypes.put("html", "text/html");
		mimeTypes.put("js", "text/javascript");
		mimeTypes.put("css", "text/css");
		mimeTypes.put("gif", "image/gif");
		mimeTypes.put("jpeg", "image/jpeg");
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("bmp", "image/bmp");
		mimeTypes.put("png", "image/png");
		mimeTypes.put("avi", "video/avi");
		mimeTypes.put("mpeg", "video/mpeg");
		mimeTypes.put("xml", "text/xml");
		mimeTypes.put("json", "application/json");
		mimeTypes.put("basic", "audio/basic");
		mimeTypes.put("wav", "audio/wav");
	}
	
	/*
	 * Retrieve the mimetype for a file extension
	 * 
	 * @param String extension xml or JPG etc.
	 * 
	 * @return String MIMEtype, for example "image/jpeg" or "application/pdf"
	 */
	public static String getMimeType(final String format) {
		return mimeTypes.get(format.toLowerCase());
	}
	
}
