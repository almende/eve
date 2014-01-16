/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.almende.eve.agent.callback.AsyncCallbackQueue;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.config.Config;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.state.StateFactory;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;

/**
 * The AgentHost is a factory to instantiate and invoke Eve Agents within the
 * configured state. The AgentHost can invoke local as well as remote agents.
 * 
 * An AgentHost must be instantiated with a valid Eve configuration file.
 * This configuration is needed to load the configured agent classes and
 * instantiate a state for each agent.
 * 
 * Example usage: // generic constructor Config config = new Config("eve.yaml");
 * AgentHost factory = new AgentHost(config);
 * 
 * // construct in servlet InputStream is =
 * getServletContext().getResourceAsStream("/WEB-INF/eve.yaml"); Config config =
 * new Config(is); AgentHost factory = new AgentHost(config);
 * 
 * // create or get a shared instance of the AgentHost AgentHost factory =
 * AgentHost.createInstance(namespace, config); AgentHost factory =
 * AgentHost.getInstance(namespace);
 * 
 * // invoke a local agent by its id response = factory.invoke(agentId,
 * request);
 * 
 * // invoke a local or remote agent by its url response =
 * factory.send(senderId, receiverUrl, request);
 * 
 * // create a new agent Agent agent = factory.createAgent(agentType, agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * // instantiate an existing agent Agent agent = factory.getAgent(agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * @author jos
 */
/**
 * @author Almende
 * 
 */
public abstract class AgentHost {
	protected static AgentHost	HOST	= null;
	
	/**
	 * Get the shared AgentHost instance.
	 * 
	 * @return Returns the host instance
	 */
	public synchronized static AgentHost getInstance() {
		if (HOST == null) {
			HOST = new AgentHostDefImpl();
		}
		return HOST;
	}
	
	/**
	 * Instantiate the services from the given config.
	 * 
	 * @param config
	 *            the config
	 */
	public abstract void loadConfig(Config config);
	
	/**
	 * Signal all agents about AgentHost event.
	 * 
	 * @param event
	 *            the event
	 */
	public abstract void signalAgents(AgentSignal<?> event);
	
	/**
	 * Get an agent by its id. Returns null if the agent does not exist
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentId
	 *            the agent id
	 * @return agent
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract Agent getAgent(String agentId)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException;
	
	/**
	 * Create an agent proxy from an java interface.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param sender
	 *            Sender Agent, used to authentication purposes.
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return the t
	 */
	public abstract <T extends AgentInterface> T createAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			Class<T> agentInterface);
	
	/**
	 * Create an asynchronous agent proxy from an java interface, each call will
	 * return a future for handling the results.
	 * 
	 * @param <T>
	 *            extends AgentInterface
	 * @param sender
	 *            Internal id of the sender agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests)
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return the async proxy
	 */
	public abstract <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			Class<T> agentInterface);
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param <T>
	 *            extends Agent
	 * @param agentType
	 *            full class path
	 * @param agentId
	 *            the agent id
	 * @return the t
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract <T extends Agent> T createAgent(String agentType,
			String agentId) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException, IOException;
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param <T>
	 *            extends Agent
	 * @param agentType
	 *            the agent type
	 * @param agentId
	 *            the agent id
	 * @return the t
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract <T extends Agent> T createAgent(Class<T> agentType,
			String agentId) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException;
	
	/**
	 * Create a new agent, using the base AspectAgent class. This agent has a
	 * namespace "sub", to which the given class's methods are added.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param aspect
	 *            the aspect
	 * @param agentId
	 *            the agent id
	 * @return the aspect agent
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract <T> AspectAgent<T> createAspectAgent(
			Class<? extends T> aspect, String agentId)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException;
	
	/**
	 * Delete an agent.
	 * 
	 * @param agentId
	 *            the agent id
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 */
	public abstract void deleteAgent(String agentId)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException;
	
	/**
	 * Test if an agent exists.
	 * 
	 * @param agentId
	 *            the agent id
	 * @return true if the agent exists
	 */
	public abstract boolean hasAgent(String agentId);
	
	/**
	 * Get the event logger. The event logger is used to temporary log triggered
	 * events, and display them on the agents web interface.
	 * 
	 * @return eventLogger
	 */
	public abstract EventLogger getEventLogger();
	
	/**
	 * Receive a message for an agent.
	 * 
	 * @param receiverId
	 *            the receiver id
	 * @param message
	 *            the message
	 * @param senderUri
	 *            the sender uri
	 * @param tag
	 *            the tag
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract void receive(String receiverId, Object message,
			URI senderUri, String tag) throws IOException;
	
	/**
	 * Asynchronously send a message to an agent.
	 * 
	 * @param receiverUrl
	 *            the receiver url
	 * @param message
	 *            the message
	 * @param sender
	 *            the sender
	 * @param tag
	 *            the tag
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	
	public abstract void sendAsync(final URI receiverUrl, final Object message,
			final AgentInterface sender, final String tag) throws IOException;
	
	/**
	 * Get the agentId from given agentUrl. The url can be any protocol. If the
	 * url matches any of the registered transport services, an agentId is
	 * returned. This means that the url represents a local agent. It is
	 * possible that no agent with this id exists.
	 * 
	 * @param agentUrl
	 *            the agent url
	 * @return agentId
	 * @throws URISyntaxException
	 */
	public abstract String getAgentId(URI agentUrl) throws URISyntaxException;
	
	/**
	 * Determines best senderUrl for this agent, match receiverUrl transport
	 * method if possible. (fallback from HTTPS to HTTP included)
	 * 
	 * @param agentId
	 *            the agent id
	 * @param receiverUrl
	 *            the receiver url
	 * @return URI SenderUrl
	 */
	public abstract URI getSenderUrl(String agentId, URI receiverUrl);
	
	/**
	 * Get the loaded config file.
	 * 
	 * @param config
	 *            A configuration file
	 */
	public abstract void setConfig(Config config);
	
	/**
	 * Get the loaded config file.
	 * 
	 * @return config A configuration file
	 */
	public abstract Config getConfig();
	
	/**
	 * Utility method to keep reference of in-memory objects from the agent. The
	 * references are stored as WeakReferences, so the life-cycle of the
	 * referenced objects will not be influenced by storing them.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param agentId
	 *            the agent id
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	public abstract <T> void putRef(String agentId, TypedKey<T> key, T value);
	
	/**
	 * Utility method to get back references to in-memory objects from the
	 * agent. The references are stored as WeakReferences, so the life-cycle of
	 * the referenced objects will not be influenced by storing them. However,
	 * the return value of this method is the hard-reference again.
	 * If the object has been garbage collected, this method will return null.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param agentId
	 *            the agent id
	 * @param key
	 *            the key
	 * @return the ref
	 */
	public abstract <T> T getRef(String agentId, TypedKey<T> key);
	
	/**
	 * Can transport services do a local invokation if the URL designates a
	 * local agent?.
	 * 
	 * @return true, if is does shortcut
	 */
	public abstract boolean isDoesShortcut();
	
	/**
	 * If true, transport services can do a local invokation if the URL
	 * designates a local agent.
	 * 
	 * @param doesShortcut
	 *            the new does shortcut
	 * @default true
	 */
	public abstract void setDoesShortcut(boolean doesShortcut);
	
	/**
	 * Load a state factory from config.
	 * 
	 * @param config
	 *            the new state factory
	 */
	public abstract void setStateFactory(Config config);
	
	/**
	 * Create agents from a config (only when they do not yet exist). Agents
	 * will be read from the configuration path bootstrap.agents, which must
	 * contain a map where the keys are agentId's and the values are the agent
	 * types (full java class path).
	 * 
	 * @param config
	 *            the config
	 */
	// TODO: private?
	public abstract void addAgents(Config config);
	
	/**
	 * Set a state factory. The state factory is used to get/create/delete an
	 * agents state.
	 * 
	 * @param stateFactory
	 *            the new state factory
	 */
	public abstract void setStateFactory(StateFactory stateFactory);
	
	/**
	 * Get the configured state factory.
	 * 
	 * @return stateFactory
	 */
	public abstract StateFactory getStateFactory();
	
	/**
	 * Load a scheduler factory from a config file.
	 * 
	 * @param config
	 *            the new scheduler factory
	 */
	// TODO: private?
	public abstract void setSchedulerFactory(Config config);
	
	/**
	 * Load transport services for incoming and outgoing messages from a config
	 * (for example http and xmpp services).
	 * 
	 * @param config
	 *            the config
	 */
	// TODO: Private?
	public abstract void addTransportServices(Config config);
	
	/**
	 * Add a new transport service.
	 * 
	 * @param transportService
	 *            the transport service
	 */
	public abstract void addTransportService(TransportService transportService);
	
	/**
	 * Remove a registered a transport service.
	 * 
	 * @param transportService
	 *            the transport service
	 */
	public abstract void removeTransportService(
			TransportService transportService);
	
	/**
	 * Get all registered transport services.
	 * 
	 * @return transportService
	 */
	public abstract List<TransportService> getTransportServices();
	
	/**
	 * Get all registered transport services which can handle given protocol.
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return transportService
	 */
	public abstract List<TransportService> getTransportServices(String protocol);
	
	/**
	 * Get the first registered transport service which supports given protocol.
	 * Returns null when none of the registered transport services can handle
	 * the protocol.
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return service
	 */
	public abstract TransportService getTransportService(String protocol);
	
	/**
	 * Set a scheduler factory. The scheduler factory is used to
	 * get/create/delete an agents scheduler.
	 * 
	 * @param schedulerFactory
	 *            the new scheduler factory
	 */
	public abstract void setSchedulerFactory(SchedulerFactory schedulerFactory);
	
	/**
	 * create a scheduler for an agent.
	 * 
	 * @param agent
	 *            the agent
	 * @return scheduler
	 */
	public abstract Scheduler getScheduler(Agent agent);
	
	/**
	 * Get a callback storage service. This service keeps AsyncCallbacks in a
	 * global accessible in-memory store.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param id
	 *            the id
	 * @param clazz
	 *            the clazz
	 * @return the callback queue
	 */
	public abstract <T> AsyncCallbackQueue<T> getCallbackQueue(String id,
			Class<T> clazz);
	
	/**
	 * Gets the result monitor factory.
	 * 
	 * @param agent
	 *            the agent
	 * @return the result monitor factory
	 */
	public abstract ResultMonitorFactoryInterface getResultMonitorFactory(
			AgentInterface agent);
	
	/**
	 * Gets the events factory.
	 * 
	 * @param agent
	 *            the agent
	 * @return the events factory
	 */
	public abstract EventsInterface getEventsFactory(AgentInterface agent);
	
	/**
	 * Utility method to get a single global threadpool, for efficient thread
	 * sharing.
	 * 
	 * @return the pool
	 */
	public abstract ExecutorService getPool();
	
	/**
	 * Gets the state factory from config.
	 * 
	 * @param config
	 *            the config
	 * @param configName
	 *            the config name
	 * @return the state factory from config
	 */
	public abstract StateFactory getStateFactoryFromConfig(Config config,
			String configName);
	
}
