/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.http;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.util.TypeUtil;
import com.almende.util.uuid.UUID;

/**
 * The Class RestServlet.
 */
@SuppressWarnings("serial")
public class RestServlet extends HttpServlet {
	private final Logger	logger	= Logger.getLogger(this.getClass()
											.getSimpleName());
	private AgentHost		host	= null;
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() {
		if (AgentHost.getInstance().getStateFactory() == null) {
			logger.severe("DEPRECIATED SETUP: Please add com.almende.eve.transport.http.AgentListener as a Listener to your web.xml!");
			AgentListener.init(getServletContext());
		}
		host = AgentHost.getInstance();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws IOException {
		try {
			// get method from url
			final String uri = req.getRequestURI();
			final String[] path = uri.split("\\/");
			final String agentId = (path.length > 2) ? path[path.length - 2]
					: null;
			final String method = (path.length > 1) ? path[path.length - 1]
					: null;
			
			// get query parameters
			final JSONRequest request = new JSONRequest();
			request.setMethod(method);
			final Enumeration<String> params = req.getParameterNames();
			while (params.hasMoreElements()) {
				final String param = params.nextElement();
				request.putParam(param, req.getParameter(param));
			}
			
			final String tag = new UUID().toString();
			final SyncCallback<Object> callback = new SyncCallback<Object>();
			
			final CallbackInterface<Object> callbacks = host
					.getCallbackService("HttpTransport", Object.class);
			callbacks.store(tag, callback);
			
			final String senderUrl = null;
			host.receive(agentId, request, senderUrl, tag);
			
			final JSONResponse response = TypeUtil.inject(callback.get(),
					JSONResponse.class);
			
			// return response
			resp.addHeader("Content-Type", "application/json");
			resp.getWriter().println(response.getResult());
		} catch (final Exception err) {
			resp.getWriter().println(err.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws IOException {
		doGet(req, resp);
	}
}
