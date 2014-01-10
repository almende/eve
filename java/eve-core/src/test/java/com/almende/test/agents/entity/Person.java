/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test.agents.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class Person.
 */
public class Person {
	private String			name;
	private String			firstName;
	private String			lastName;
	private List<Double>	marks	= new ArrayList<Double>();
	
	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the last name.
	 * 
	 * @param lastName
	 *            the new last name
	 */
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}
	
	/**
	 * Gets the last name.
	 * 
	 * @return the last name
	 */
	public String getLastName() {
		return lastName;
	}
	
	/**
	 * Sets the first name.
	 * 
	 * @param firstName
	 *            the new first name
	 */
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}
	
	/**
	 * Gets the first name.
	 * 
	 * @return the first name
	 */
	public String getFirstName() {
		return firstName;
	}
	
	/**
	 * Sets the marks.
	 * 
	 * @param marks
	 *            the new marks
	 */
	public void setMarks(final List<Double> marks) {
		this.marks = marks;
	}
	
	/**
	 * Gets the marks.
	 * 
	 * @return the marks
	 */
	public List<Double> getMarks() {
		return marks;
	}
}
