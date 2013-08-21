package com.almende.util;

import java.io.IOException;
import java.io.InputStream;

public final class StringUtil {
	
	private StringUtil(){};
	
	/**
	 * Convert a stream to a string
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String streamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		int n = 0;
		while(true){
			n = in.read(b);
			if (n == -1) {
				break;
			}
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
}
