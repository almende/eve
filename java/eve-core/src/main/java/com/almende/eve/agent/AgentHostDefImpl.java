package com.almende.eve.agent;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.CallbackService;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.config.Config;
import com.almende.eve.event.EventsFactory;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactory;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;
import com.almende.util.ClassUtil;
import com.almende.util.ObjectCache;
import com.almende.util.TypeUtil;

public final class AgentHostDefImpl extends AgentHost {
	
	private static final Logger																	LOG					= Logger.getLogger(AgentHostDefImpl.class
																															.getSimpleName());
	private final ConcurrentHashMap<String, TransportService>									transportServices	= new ConcurrentHashMap<String, TransportService>();
	private final ConcurrentHashMap<String, CallbackInterface<?>>								callbacks			= new ConcurrentHashMap<String, CallbackInterface<?>>();
	private StateFactory																		stateFactory		= null;
	private SchedulerFactory																	schedulerFactory	= null;
	private Config																				config				= null;
	private final EventLogger																	eventLogger			= new EventLogger(
																															this);
	private boolean																				doesShortcut		= true;
	
	private static final ExecutorService														POOL				= Executors
																															.newCachedThreadPool();
	
	private final ConcurrentHashMap<String, ConcurrentHashMap<TypedKey<?>, WeakReference<?>>>	refStore			= new ConcurrentHashMap<String, ConcurrentHashMap<TypedKey<?>, WeakReference<?>>>();
	
	private static final String																	AGENTS				= "agents";
	
	@Override
	public ExecutorService getPool() {
		return POOL;
	}
	
	@Override
	public void loadConfig(final Config config) {
		HOST.setConfig(config);
		if (config != null) {
			ObjectCache.get(AGENTS).configCache(config);
			// initialize all factories for state, transport, and scheduler
			// important to initialize in the correct order: cache first,
			// then the state and transport services, and lastly scheduler.
			HOST.setStateFactory(config);
			HOST.addTransportServices(config);
			HOST.setSchedulerFactory(config);
			HOST.addAgents(config);
		}
	}
	
	@Override
	public void signalAgents(final AgentSignal<?> event) {
		if (stateFactory != null) {
			final Iterator<String> iter = stateFactory.getAllAgentIds();
			if (iter != null) {
				while (iter.hasNext()) {
					try {
						final Agent agent = getAgent(iter.next());
						if (agent != null) {
							agent.signalAgent(event);
						}
					} catch (final Exception e) {
						LOG.log(Level.WARNING, "Couldn't signal agent.", e);
					}
				}
			}
		}
	}
	
	@Override
	public Agent getAgent(final String agentId) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException {
		
		if (agentId == null) {
			return null;
		}
		
		if (getStateFactory() == null) {
			return null;
		}
		
		// Check if agent is instantiated already, returning if it is:
		Agent agent = ObjectCache.get(AGENTS).get(agentId, Agent.class);
		if (agent != null) {
			return agent;
		}
		// No agent found, normal initialization:
		
		// load the State
		final State state = getStateFactory().get(agentId);
		if (state == null) {
			// agent does not exist
			return null;
		}
		state.init();
		
		// read the agents class name from state
		final Class<?> agentType = state.getAgentType();
		if (agentType == null) {
			LOG.warning("Cannot instantiate agent. "
					+ "Class information missing in the agents state "
					+ "(agentId='" + agentId + "')");
			return null;
		}
		
		if (!Agent.class.isAssignableFrom(agentType)) {
			// Found state info not representing an Agent, like e.g. TokenStore
			// or CookieStore.
			return null;
		}
		
		// instantiate the agent
		agent = (Agent) agentType.getConstructor().newInstance();
		agent.constr(this, state);
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.INIT));
		
		// If allowed, cache agent:
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			ObjectCache.get(AGENTS).put(agentId, agent);
		}
		
		return agent;
	}
	
	@Override
	public <T extends AgentInterface> T createAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			final Class<T> agentInterface) {
		
		// TODO: In the new model the proxy agents need to have an adres as
		// well! This will enforce usage of the agentCache!
		final String proxyId = "proxy_"
				+ (sender != null ? sender.getId() + "_" : "")
				+ agentInterface.getCanonicalName().replace(' ', '_');
		
		T proxy = ObjectCache.get(AGENTS).get(proxyId, agentInterface);
		if (proxy != null) {
			return proxy;
		}
		final AgentProxyFactory pf = new AgentProxyFactory(this);
		proxy = pf.genProxy(sender, receiverUrl, agentInterface, proxyId);
		
		ObjectCache.get(AGENTS).put(proxyId, proxy);
		
		return proxy;
	}
	
	@Override
	public <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			final Class<T> agentInterface) {
		return new AsyncProxy<T>(createAgentProxy(sender, receiverUrl,
				agentInterface));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Agent> T createAgent(final String agentType, final String agentId)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, IOException {
		return createAgent((Class<T>) Class.forName(agentType), agentId);
	}
	
	@Override
	public <T extends Agent> T createAgent(final Class<T> agentType, final String agentId)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException {
		
		// create the state
		final State state = getStateFactory().create(agentId);
		state.setAgentType(agentType);
		state.init();
		
		// instantiate the agent
		final T agent = agentType.getConstructor().newInstance();
		agent.constr(this, state);
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.CREATE));
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.INIT));
		
		// Cache agent if allowed
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			ObjectCache.get(AGENTS).put(agentId, agent);
		}
		
		return agent;
	}
	
	@Override
	public <T> AspectAgent<T> createAspectAgent(final Class<? extends T> aspect,
			final String agentId) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		@SuppressWarnings("unchecked")
		final
		AspectAgent<T> result = createAgent(AspectAgent.class, agentId);
		result.init(aspect);
		return result;
	}
	
	@Override
	public void deleteAgent(final String agentId) {
		if (agentId == null) {
			return;
		}
		Agent agent = null;
		try {
			agent = getAgent(agentId);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't get agent to delete.", e);
		}
		if (agent != null) {
			if (getScheduler(agent) != null) {
				schedulerFactory.destroyScheduler(agentId);
			}
			try {
				// get the agent and execute the delete method
				agent.signalAgent(new AgentSignal<Void>(AgentSignal.DESTROY));
				agent.signalAgent(new AgentSignal<Void>(AgentSignal.DELETE));
				ObjectCache.get(AGENTS).delete(agentId);
				agent = null;
			} catch (final Exception e) {
				LOG.log(Level.WARNING, "Error deleting agent:" + agentId, e);
			}
		}
		// delete the state, even if the agent.destroy or agent.delete
		// failed.
		getStateFactory().delete(agentId);
	}
	
	@Override
	public boolean hasAgent(final String agentId) {
		return getStateFactory().exists(agentId);
	}
	
	@Override
	public EventLogger getEventLogger() {
		return eventLogger;
	}
	
	@Override
	public void receive(final String receiverId, final Object message, final String senderUrl,
			final String tag) throws IOException {
		URI senderUri = null;
		if (senderUrl != null) {
			try {
				senderUri = URI.create(senderUrl);
			} catch (final Exception e) {
				LOG.warning("Incorrect senderUrl given:" + senderUrl);
			}
		}
		receive(receiverId, message, senderUri, tag);
	}
	
	@Override
	public void receive(final String receiverId, final Object message, final URI senderUri,
			final String tag) throws IOException {
		AgentInterface receiver = null;
		try {
			receiver = getAgent(receiverId);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't getAgent(" + receiverId + ")", e);
			throw new IOException(e);
		}
		if (receiver == null) {
			// Check if there might be a proxy in the objectcache:
			receiver = ObjectCache.get(AGENTS).get(receiverId,
					AgentInterface.class);
		}
		if (receiver != null) {
			receiver.receive(message, senderUri, tag);
		}
	}
	
	@Override
	public void sendAsync(final URI receiverUrl, final Object message,
			final AgentInterface sender, final String tag) throws IOException {
		final String receiverId = getAgentId(receiverUrl.toASCIIString());
		final String protocol = receiverUrl.getScheme();
		if (("local".equals(protocol)) || (doesShortcut && receiverId != null)) {
			// local shortcut
			URI senderUri = null;
			if (sender != null) {
				senderUri = getSenderUrl(sender.getId(), receiverUrl);
			}
			receive(receiverId, message, senderUri, tag);
		} else {
			TransportService service = null;
			String senderUrl = null;
			if (sender != null) {
				final URI senderUri = getSenderUrl(sender.getId(), receiverUrl);
				senderUrl = senderUri.toASCIIString();
			}
			service = getTransportService(protocol);
			if (service != null) {
				service.sendAsync(senderUrl, receiverUrl.toASCIIString(),
						message, tag);
			} else {
				throw new ProtocolException(
						"No transport service configured for protocol '"
								+ protocol + "'.");
			}
		}
	}
	
	// TODO: change to URI en create a protocol->transport map in agentHost
	@Override
	public String getAgentId(final String agentUrl) {
		if (agentUrl.startsWith("local:")) {
			return agentUrl.replaceFirst("local:/?/?", "");
		}
		for (final TransportService service : transportServices.values()) {
			final String agentId = service.getAgentId(agentUrl);
			if (agentId != null) {
				return agentId;
			}
		}
		return null;
	}
	
	@Override
	public URI getSenderUrl(final String agentId, final URI receiverUrl) {
		if (receiverUrl.getScheme().equals("local")) {
			return URI.create("local:" + agentId);
		}
		for (final TransportService service : transportServices.values()) {
			final List<String> protocols = service.getProtocols();
			for (final String protocol : protocols) {
				if (receiverUrl.getScheme().equals(protocol)) {
					final String senderUrl = service.getAgentUrl(agentId);
					if (senderUrl != null) {
						return URI.create(senderUrl);
					}
				}
			}
		}
		LOG.warning("Couldn't find sender URL for:" + agentId + " | "
				+ receiverUrl.toASCIIString());
		return null;
	}
	
	@Override
	public <T> T getRef(final String agentId, final TypedKey<T> key) {
		final ConcurrentHashMap<TypedKey<?>, WeakReference<?>> objects = refStore
				.get(agentId);
		if (objects != null) {
			return TypeUtil.inject(objects.get(key).get(), key.getType());
		}
		
		return null;
	}
	
	@Override
	public <T> void putRef(final String agentId, final TypedKey<T> key, final T value) {
		synchronized (refStore) {
			ConcurrentHashMap<TypedKey<?>, WeakReference<?>> objects = refStore
					.get(agentId);
			if (objects == null) {
				objects = new ConcurrentHashMap<TypedKey<?>, WeakReference<?>>();
			}
			objects.put(key, new WeakReference<Object>(value));
			refStore.put(agentId, objects);
		}
	}
	
	@Override
	public StateFactory getStateFactoryFromConfig(final Config config,
			String configName) {
		StateFactory result = null;
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		
		String className = config.get(configName, "class");
		if (className == null) {
			if (!configName.equals("state")) {
				// Provide fallback to state if other type doesn't exist;
				configName = "state";
				className = config.get(configName, "class");
			}
			if (className == null) {
				throw new IllegalArgumentException("Config parameter '"
						+ config + ".class' missing in Eve configuration.");
			}
		}
		
		try {
			// get the class
			final Class<?> stateClass = Class.forName(className);
			if (!ClassUtil.hasInterface(stateClass, StateFactory.class)) {
				throw new IllegalArgumentException("State factory class "
						+ stateClass.getName() + " must extend "
						+ State.class.getName());
			}
			
			// instantiate the state factory
			final Map<String, Object> params = config.get(configName);
			result = (StateFactory) stateClass.getConstructor(Map.class)
					.newInstance(params);
			
			LOG.info("Initialized state factory: " + result.toString());
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	@Override
	public void setStateFactory(final Config config) {
		if (stateFactory != null) {
			LOG.warning("Not loading statefactory from config, there is already a statefactory available.");
			return;
		}
		
		setStateFactory(getStateFactoryFromConfig(config, "state"));
	}
	
	@Override
	public void addAgents(final Config config) {
		final Map<String, String> agents = config.get("bootstrap", AGENTS);
		if (agents != null) {
			for (final Entry<String, String> entry : agents.entrySet()) {
				final String agentId = entry.getKey();
				final String agentType = entry.getValue();
				try {
					Agent agent = getAgent(agentId);
					if (agent == null) {
						// agent does not yet exist. create it
						agent = createAgent(agentType, agentId);
						LOG.info("Bootstrap created agent id=" + agentId
								+ ", type=" + agentType);
					}
				} catch (final Exception e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
		}
	}
	
	@Override
	public void setStateFactory(final StateFactory stateFactory) {
		if (this.stateFactory != null) {
			LOG.warning("Not setting new stateFactory, there is already a factory initialized.");
			return;
		}
		this.stateFactory = stateFactory;
		HOST.signalAgents(new AgentSignal<StateFactory>(
				AgentSignal.SETSTATEFACTORY, stateFactory));
		
	}
	
	@Override
	public StateFactory getStateFactory() {
		if (stateFactory == null) {
			LOG.warning("No state factory initialized.");
		}
		return stateFactory;
	}
	
	@Override
	public void setSchedulerFactory(final Config config) {
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		final String className = config.get("scheduler", "class");
		if (className == null) {
			throw new IllegalArgumentException(
					"Config parameter 'scheduler.class' missing in Eve configuration.");
		}
		
		// read all scheduler params (will be fed to the scheduler factory
		// on construction)
		final Map<String, Object> params = config.get("scheduler");
		
		try {
			// get the class
			final Class<?> schedulerClass = Class.forName(className);
			if (!ClassUtil.hasInterface(schedulerClass, SchedulerFactory.class)) {
				throw new IllegalArgumentException("Scheduler class "
						+ schedulerClass.getName() + " must implement "
						+ SchedulerFactory.class.getName());
			}
			
			// initialize the scheduler factory
			final SchedulerFactory sf = (SchedulerFactory) schedulerClass
					.getConstructor(AgentHost.class, Map.class).newInstance(
							this, params);
			
			setSchedulerFactory(sf);
			
			LOG.info("Initialized scheduler factory: "
					+ sf.getClass().getName());
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	@Override
	public void addTransportServices(final Config config) {
		if (config == null) {
			final Exception e = new Exception("Configuration uninitialized");
			LOG.log(Level.WARNING, "", e);
			return;
		}
		
		// read global service params
		final List<Map<String, Object>> allTransportParams = config
				.get("transport_services");
		if (allTransportParams != null) {
			int index = 0;
			for (final Map<String, Object> transportParams : allTransportParams) {
				String className = (String) transportParams.get("class");
				try {
					if (className != null) {
						
						// Recognize known classes by their short name,
						// and replace the short name for the full class path
						className = Config.map(className);
						// get class
						final Class<?> transportClass = Class.forName(className);
						if (!ClassUtil.hasInterface(transportClass,
								TransportService.class)) {
							throw new IllegalArgumentException(
									"TransportService class "
											+ transportClass.getName()
											+ " must implement "
											+ TransportService.class.getName());
						}
						
						// initialize the transport service
						final TransportService transport = (TransportService) transportClass
								.getConstructor(AgentHost.class, Map.class)
								.newInstance(this, transportParams);
						
						// register the service with the agent factory
						addTransportService(transport);
					} else {
						LOG.warning("Cannot load transport service at index "
								+ index + ": no class defined.");
					}
				} catch (final Exception e) {
					LOG.log(Level.WARNING, "Cannot load service at index "
							+ index, e);
				}
				index++;
			}
		}
	}
	
	@Override
	public void addTransportService(final TransportService transportService) {
		if (!transportServices.contains(transportService.getKey())) {
			transportServices.put(transportService.getKey(), transportService);
			LOG.info("Registered transport service: "
					+ transportService.toString());
			if (HOST != null) {
				HOST.signalAgents(new AgentSignal<TransportService>(
						AgentSignal.ADDTRANSPORTSERVICE, transportService));
			}
		} else {
			LOG.warning("Not adding transport service, as it already exists.");
		}
	}
	
	@Override
	public void removeTransportService(final TransportService transportService) {
		transportServices.remove(transportService);
		LOG.info("Unregistered transport service "
				+ transportService.toString());
		HOST.signalAgents(new AgentSignal<TransportService>(
				AgentSignal.DELTRANSPORTSERVICE, transportService));
		
	}
	
	@Override
	public List<TransportService> getTransportServices() {
		// TODO: check efficiency of this method, is there something simpler?
		return Collections.list(Collections.enumeration(transportServices
				.values()));
	}
	
	@Override
	public List<TransportService> getTransportServices(final String protocol) {
		final List<TransportService> filteredServices = new ArrayList<TransportService>();
		
		for (final TransportService service : transportServices.values()) {
			final List<String> protocols = service.getProtocols();
			if (protocols.contains(protocol)) {
				filteredServices.add(service);
			}
		}
		
		return filteredServices;
	}
	
	@Override
	public TransportService getTransportService(final String protocol) {
		final List<TransportService> services = getTransportServices(protocol);
		if (services.size() > 0) {
			return services.get(0);
		}
		return null;
	}
	
	@Override
	public void setSchedulerFactory(final SchedulerFactory schedulerFactory) {
		if (this.schedulerFactory != null) {
			LOG.warning("Replacing earlier schedulerFactory.");
		}
		this.schedulerFactory = schedulerFactory;
		HOST.signalAgents(new AgentSignal<SchedulerFactory>(
				AgentSignal.SETSCHEDULERFACTORY, schedulerFactory));
	}
	
	@Override
	public Scheduler getScheduler(final Agent agent) {
		if (schedulerFactory == null) {
			return null;
		}
		return schedulerFactory.getScheduler(agent);
	}
	
	@Override
	public synchronized <T> CallbackInterface<T> getCallbackService(final String id,
			final Class<T> clazz) {
		// TODO: make this better!
		final TypeUtil<CallbackInterface<T>> type = new TypeUtil<CallbackInterface<T>>() {
		};
		CallbackInterface<T> result = type.inject(callbacks.get(id));
		if (result == null) {
			result = new CallbackService<T>();
			callbacks.put(id, result);
		}
		return result;
	}
	
	@Override
	public void setConfig(final Config config) {
		this.config = config;
	}
	
	@Override
	public Config getConfig() {
		return config;
	}
	
	@Override
	public boolean isDoesShortcut() {
		return doesShortcut;
	}
	
	@Override
	public void setDoesShortcut(final boolean doesShortcut) {
		this.doesShortcut = doesShortcut;
	}
	
	@Override
	public ResultMonitorFactoryInterface getResultMonitorFactory(
			final AgentInterface agent) {
		return new ResultMonitorFactory(agent);
	}
	
	@Override
	public EventsInterface getEventsFactory(final AgentInterface agent) {
		return new EventsFactory(agent);
	}
}
