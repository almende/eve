package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Attendee implements Serializable {
	public String displayName = null;
    public String email = null;
	public String agent = null;       // eve agent url
	public boolean mustAttend = true;
}
