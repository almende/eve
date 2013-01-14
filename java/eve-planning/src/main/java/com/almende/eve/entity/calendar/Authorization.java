/**
 * Helper class to store authorization tokens
 */
package com.almende.eve.entity.calendar;

import java.io.Serializable;

import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class Authorization implements Serializable {
	public Authorization() {}
	
	public Authorization(String accessToken, String tokenType, 
			DateTime expiresAt, String refreshToken) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresAt = expiresAt;
		this.refreshToken = refreshToken;
	}
	
	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public DateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(DateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
	
	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	private String accessToken = null;
	private String tokenType = null;
	private DateTime expiresAt = null;
	private String refreshToken = null;
}
