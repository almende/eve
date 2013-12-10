package com.almende.eve.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.state.google.KeyValue;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

@SuppressWarnings("serial")
public class TestServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		QueryResultIterator<KeyValue> query = datastore.find().type(KeyValue.class).now();
		
		int count = 0;
		while (query.hasNext()) {
			query.next();
			count++;
		}
		
		resp.getWriter().println("we have " + count + " agents");
	}
}
