/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * The Class StreamingUtil.
 */
public final class StreamingUtil {
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
	
	/**
	 * Instantiates a new streaming util.
	 */
	private StreamingUtil() {
	};
	
	/**
	 * Stream an input stream to a servlet response.
	 * 
	 * @param inputStream
	 *            the input stream
	 * @param mimeType
	 *            the mime type
	 * @param response
	 *            the response
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
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
	
	/*
	 * Retrieve the mimetype for a file extension
	 * 
	 * @param String extension xml or JPG etc.
	 * 
	 * @return String MIMEtype, for example "image/jpeg" or "application/pdf"
	 */
	/**
	 * Gets the mime type.
	 * 
	 * @param format
	 *            the format
	 * @return the mime type
	 */
	public static String getMimeType(final String format) {
		return mimeTypes.get(format.toLowerCase());
	}
	
}
