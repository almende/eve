package com.almende.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class StreamingUtil {
	/**
	 * Stream an input stream to a servlet response
	 * 
	 * @param inputStream
	 * @param mimeType
	 * @param response
	 * @throws IOException
	 */
	public static void streamBinaryData(InputStream inputStream, String mimeType, 
			HttpServletResponse response) throws IOException {
		// TODO: cleanup
		//String filename = "/com/almende/eve/resources/agent.html";
		//InputStream is = this.getClass().getResourceAsStream(filename);
		OutputStream os = response.getOutputStream();
		if (inputStream != null && os != null) {
			response.setContentType(mimeType);
			// TODO: use buffered streams?
			//BufferedInputStream bis = new BufferedInputStream(is);
			//BufferedOutputStream bos = new BufferedOutputStream(os);
			
			byte[] buff = new byte[1024];
			int bytesRead;
			while(-1 != (bytesRead = inputStream.read(buff, 0, buff.length))) {
				os.write(buff, 0, bytesRead);
			}
		}
	}

	/*
	 * Retrieve the mimetype for a file extension
	 * 
	 * @param String extension xml or JPG etc.
	 * @return String MIMEtype, for example "image/jpeg" or "application/pdf"
	 */
	public static String getMimeType(String format) {
		if (format.equalsIgnoreCase("pdf")) // check the out type
			return "application/pdf";
		else if (format.equalsIgnoreCase("html"))
			return "text/html";
		else if (format.equalsIgnoreCase("js"))
			return "text/javascript";
		else if (format.equalsIgnoreCase("css"))
			return "text/css";
		else if (format.equalsIgnoreCase("gif"))
			return "image/gif";
		else if (format.equalsIgnoreCase("jpeg"))
			return "image/jpeg";
		else if (format.equalsIgnoreCase("jpg"))
			return "image/jpeg";
		else if (format.equalsIgnoreCase("bmp"))
			return "image/bmp";
		else if (format.equalsIgnoreCase("png"))
			return "image/png";
		else if (format.equalsIgnoreCase("avi"))
			return "video/avi";
		else if (format.equalsIgnoreCase("mpeg"))
			return "video/mpeg";
		else if (format.equalsIgnoreCase("xml"))
			return "text/xml";
		else if (format.equalsIgnoreCase("json"))
			return "application/json";
		else if (format.equalsIgnoreCase("basic"))
			return "audio/basic";
		else if (format.equalsIgnoreCase("wav"))
			return "audio/wav";
		else
			return null;
	}
}
