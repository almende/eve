package com.almende.test.agents.entity;

import java.util.ArrayList;
import java.util.List;

public class Person {
	private String name;
	private String firstName;
	private String lastName;
	private List<Double> marks = new ArrayList<Double>();
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setMarks(List<Double> marks) {
		this.marks = marks;
	}

	public List<Double> getMarks() {
		return marks;
	}
}
