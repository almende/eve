/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * The Class StringUtil.
 */
public final class StringUtil {
	
	/**
	 * Instantiates a new string util.
	 */
	private StringUtil() {
	};
	
	/**
	 * Convert a stream to a string.
	 *
	 * @param in the in
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String streamToString(final InputStream in) throws IOException {
		final StringBuffer out = new StringBuffer();
		final byte[] b = new byte[4096];
		int n = 0;
		while (true) {
			n = in.read(b);
			if (n == -1) {
				break;
			}
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
}
