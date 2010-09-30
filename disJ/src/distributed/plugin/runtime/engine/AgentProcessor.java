package distributed.plugin.runtime.engine;

import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import distributed.plugin.core.Agent;
import distributed.plugin.core.DisJException;
import distributed.plugin.core.Edge;
import distributed.plugin.core.IConstants;
import distributed.plugin.core.Logger;
import distributed.plugin.core.Node;
import distributed.plugin.core.Logger.logTag;
import distributed.plugin.random.IRandom;
import distributed.plugin.runtime.AgentEvent;
import distributed.plugin.runtime.Event;
import distributed.plugin.runtime.Graph;
import distributed.plugin.runtime.GraphLoader;
import distributed.plugin.runtime.IMessage;
import distributed.plugin.runtime.IProcessor;
import distributed.plugin.runtime.engine.AgentModel.NotifyType;
import distributed.plugin.ui.IGraphEditorConstants;

public abstract class AgentProcessor implements IProcessor {

	private boolean stop;

	private boolean pause;

	private boolean stepForward;

	private int speed;

	private String procName;

	private EventQueue queue;

	Graph graph;

	private TimeGenerator timeGen;

	/*
	 * Logger to log every activities of this process
	 * for replay action
	 */
	private Logger log;
	
	/*
	 * Client random Class file (blueprint)
	 */
	private Class<IRandom> clientRandom;

	/*
	 * Global tracking list of all created agents
	 */
	private Map<String, Agent> allAgents;	

	/*
	 * A mapping table of every possible state name and value 
	 * of agent which is defined in user algorithm
	 */	
	Map<Integer, String> stateFields;

	/*
	 * System out delegate to Eclipse plug-in console
	 */
	MessageConsoleStream systemOut;

	/**
	 * Constructor
	 * 
	 * @param graph A graph model that used by the processor
	 * @param clientRandom A client IRandom object the hold algorithm
	 * @param out A URL to a location of log files directory
	 */
	AgentProcessor(Graph graph, Class<IRandom> clientRandom, URL out) {
		
		if (graph == null){
			throw new NullPointerException(IConstants.RUNTIME_ERROR_0);
		}
	
		this.speed = IConstants.SPEED_DEFAULT_RATE;
		this.stop = false;
		this.pause = false;		
		
		this.graph = graph;
		this.procName = graph.getId();
		this.clientRandom = clientRandom;
		this.queue = new EventQueue();
		this.stateFields = new HashMap<Integer, String>();
		this.allAgents = new HashMap<String, Agent>();
		
		this.timeGen = TimeGenerator.getTimeGenerator();
		this.timeGen.addGraph(this.graph.getId());		
		
		this.log = new Logger(this.graph.getId(), out, this.timeGen);		

		this.setSystemOutConsole();
		
		if (this.clientRandom != null) {
			this.initClientRandomStateVariables();
		}
	}
	
	/*
	 * Configure system output to Eclipse Plug-in console
	 */
	private void setSystemOutConsole(){
		MessageConsole console = SimulatorEngine.findConsole(IGraphEditorConstants.DISJ_CONSOLE);
		this.systemOut = console.newMessageStream();
		System.setOut(new PrintStream(this.systemOut));
		System.setErr(new PrintStream(this.systemOut));
	}

	// FIXME need to do something here!!!
	private void initClientRandomStateVariables() {
		
	}
	
	/**
	 * Get a disJ plug-in console
	 * 
	 * @return
	 */
	public MessageConsoleStream getSystemOut() {
		return this.systemOut;
	}

	/*
	 * Write exception message to Eclipse plug-in console 
	 * then throw RuntimeException
	 * 
	 * @param e an exception that has been thrown
	 */
	private void throwException(Throwable e, String msg){		
		this.systemOut.println(msg + " [Critical] " + e.toString());
		throw new RuntimeException(e);
	}
	
	/*
	 * Load and initialize all nodes in the network
	 */
	private void loadNode() throws DisJException {
		Map<String, Node> nodes = this.graph.getNodes();
		
		Iterator<String> it = nodes.keySet().iterator();
		for(String id = null; it.hasNext();){
			id = it.next();
			Node node = nodes.get(id);
			node.setLogger(this.log);
			node.setStateNames(this.stateFields);
			nodes.put(id, node);
		}
	}
	
	protected abstract AgentModel createClientAgent() throws Exception;
	
	/*
	 * Load every agent(s) into host nodes and initialize event(s)
	 */
	private void loadAgent() throws DisJException {

		int agentId = 0;
		String[] suitcase;
		List<Node> hosts = GraphLoader.getInitNodes(this.graph);
		try {
			// load host nodes that contain agent(s)
			for (int i = 0; i < hosts.size(); i++) {
				Node host = hosts.get(i);
				int numAgent = host.getNumInitAgent();
				for(int j = 0; j < numAgent; j++){
					// create and initialize new agent instance that 
					// belong to each host
					Agent agent = new Agent(agentId +"", host.getNodeId());
					agent.setCurNode(host);
					agent.setLogger(this.log);
					agent.setStateNames(this.stateFields);
					
					// initialize memory suitcase for agent
					suitcase = new String[agent.getMaxSlot()];
					agent.setInfo(suitcase);
					
					AgentModel clientAgent = this.createClientAgent();				
					agent.setClientEntity(clientAgent);					
					clientAgent.initAgent(agent, this);
										
					// add to home host
					host.addAgent(agent);
					
					// add to global/graphUI tracking list
					this.allAgents.put(agentId+"", agent);
					this.graph.addAgent(agentId+"", agent);
					
					agentId++;
					this.systemOut.println("@loadAgent() " + agent.getAgentId());
				}
			}
			
			// create a set of init events
			int execTime = 0;
			IMessage msg;
			Random r = new Random(System.currentTimeMillis());
			List<Event> events = new ArrayList<Event>();			
			for (int i = 0; i < hosts.size(); i++) {
				Node host = hosts.get(i);
				List<Agent> list = host.getAllAgents();
				int eventId;
				for(int j = 0; j < list.size(); j++){
					Agent agent = list.get(j);					
					eventId = this.getNextId();
					msg = new Message("Initialized", new Integer(execTime));
					
					// it is not a starter, set the starting time to random
					// OR TODO get a start time from user input
					if (!agent.isStarter()){
						execTime = r.nextInt(IConstants.MAX_RANDOM_RANGE);
					}
					// add an event into a queue
					Event e = new AgentEvent(IConstants.EVENT_INITIATE_TYPE,
							eventId, execTime, host.getNodeId(),agent.getAgentId(), msg);
	
					events.add(e);
				}
			}

			// add to the queue
			this.queue.pushEvents(events);

			// update time's lower bound
			this.setCurrentTime(this.queue.topEvent().getExecTime());
			
		} catch (Exception e) {
			throw new DisJException(IConstants.ERROR_8, e.toString());
		}
	}
	
	@Override
	public void processReqeust(String sender, List<String> receivers,
			IMessage message){

		String msgLabel = message.getLabel();
		if(msgLabel.equals(IConstants.MESSAGE_SET_ALARM_CLOCK)){			
			int[] msg = (int[])message.getContent();
			this.processAlarmClock(sender, msg[0]+"", msg[1]);
			
		}else if (msgLabel.equals(IConstants.MESSAGE_EVENT_MOVE_TO)){
			for(int i =0; i < receivers.size(); i++){
				String port = receivers.get(i);
				String agentId = (String)message.getContent();
				this.processMove(sender, port, agentId);
			}
		}else if (msgLabel.equals(IConstants.MESSAGE_EVENT_NOTIFY)){
			NotifyType type = (NotifyType)message.getContent();
			this.processNotify(sender, type);
			
		}else{
			
		}						
	}
	
	private void processNotify(String nodeId, NotifyType type){
		// Notify agent who registered the event of this node
		Node sNode = this.graph.getNode(nodeId);
		Map<String, List<NotifyType>> list = sNode.getRegistees();
		Iterator<String> its = list.keySet().iterator();
		String agentId;
		// create notify event to every agent who registered
		// a give type to a given node
		for(List<NotifyType> tmp = null;its.hasNext();){
			agentId = its.next();
			tmp = list.get(agentId);
			if(tmp.contains(type)){
				// generate notify event to be executed now (at simulation time)		
				int eventId = this.getNextId();		
				IMessage msg = new Message(IConstants.MESSAGE_EVENT_NOTIFY, type);			 
				Event e = new AgentEvent(IConstants.EVENT_NOTIFY_TYPE, eventId,
						this.getCurrentTime(), nodeId, agentId, msg);
				
				this.queue.pushEvent(e);
			}
		}		
	}
	
	private void processMove(String fromNodeId, String toPort, String agentId){
		Node sNode = this.graph.getNode(fromNodeId);
		Agent agent = this.allAgents.get(agentId);
		try {			
			Edge link = sNode.getEdge(toPort);
			Node target = link.getOthereEnd(sNode);
			
			// validate an outgoing port given by user
			List<String> ports = sNode.getOutgoingPorts();
			if(!ports.contains(toPort)){
				// Notify to user
				this.systemOut.println("@processMove() There is no outgoing port " + toPort
						+ " for node " + fromNodeId);
				return;
			}			
			// remove agent from a node			
			sNode.removeAgent(agent);
			
			AgentModel entity = agent.getClientEntity();
			
			// notify node of departure
			entity.notifyEvent(NotifyType.AGENT_DEPARTURE);
			
			this.systemOut.println("@processMove() " + agent.getAgentId() + " from " 
					+ fromNodeId + " to " + target.getNodeId());
			
			// update log
			String[] value = {sNode.getNodeId(), toPort};
			this.log.logAgent(logTag.AGENT_LEAVE, agentId, value);
			
			// validate the probability of failure
			if (!link.isReliable()) {
				Random ran = new Random(System.currentTimeMillis());
				int prob = ran.nextInt(100) + 1;
				if (prob <= link.getProbOfFailure()) {
					this.systemOut.println("Agent " + agentId + " lost in a link " + link.getEdgeId()
							+ " while travelling from " + fromNodeId);
					
					// flag that agent is dead on a link
					Agent g = this.allAgents.get(agentId);
					g.setAlive(false);
					
					this.allAgents.put(agentId, g);
					this.graph.removeAgent(agentId);
					value = new String[2];
					value[0] = fromNodeId;
					value[1] = toPort;
					this.log.logAgent(logTag.AGENT_DIE, agentId, value);
					return;
				}
			}
			// generate arrival event at a target
			int execTime = link.getDelayTime(sNode, this.getCurrentTime());
			int eventId = this.getNextId();		
			IMessage msg = new Message(IConstants.MESSAGE_EVENT_ARRIVE_AT, link.getEdgeId());			 
			Event e = new AgentEvent(IConstants.EVENT_ARRIVAL_TYPE, eventId,
					 execTime, target.getNodeId(), agentId, msg);
			
			this.queue.pushEvent(e);
			
		}catch(Exception e){
			this.throwException(e, "@processMove()");
			
		}
	}

	private void processAlarmClock(String nodeId, String agentId, int delay){
		try{
			int eventId = this.getNextId();
			int execTime = this.getCurrentTime()+ delay;
	
			IMessage msg = new Message(IConstants.MESSAGE_EVENT_ARRIVE_AT, agentId);
			
			// create and add event into a queue
			Event e = new AgentEvent(IConstants.EVENT_ALARM_RING_TYPE, 
					eventId, execTime, nodeId, agentId, msg);
			
			// update log
			String[] value = {nodeId, execTime+""};
			this.log.logAgent(logTag.AGENT_SLEEP, agentId, value);
			
			// add to event queue
			this.queue.pushEvent(e);
	
			// update time's lower bound, the alarm ringing may be is a smallest
			this.setCurrentTime(this.queue.topEvent().getExecTime());
			
		}catch(Exception e){
			this.throwException(e, "@processAlarmClock()");
		}
	}
	
	public Map<Integer, String> getStateFields() {
		return stateFields;
	}

	public boolean isPause() {
		return pause;
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public int getSpeed() {
		return this.speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void cleanUp() {
			
		// reset the flag the process is terminated
		this.stop = true;
		this.pause = false;
		
		// clear agents
		this.allAgents.clear();
		
		// clean up the memory
		this.graph.removeAllAgents();
		this.graph = null;

	}
	
	
	@Override
	public void run() {
		try {
			// init logger
			this.log.initLog();
		
			// load and init any necessary data
			this.loadNode();
			this.loadAgent();

			// start execute events
			this.executeEvents();

			this.systemOut.println("\n*****Simulation for " + this.procName
					+ " is successfully over.*****");

		} catch (Exception e) {
			e.printStackTrace();
			this.systemOut.println(e.toString());
			
		} finally {			
			// clean up logger
			this.log.cleanUp();

			// clean up necessary data
			this.cleanUp();
			
			this.systemOut.println("\n*****The simulation of " + this.procName
					+ " is terminated.*****");
		}
	}

	/*
	 * Start executing event in the queue
	 */
	private void executeEvents() throws DisJException {

		while (!this.queue.isEmpty()) {
			if (this.stop){
				break;
			}
			// To slow down the simulation speed
			try {
				Thread.sleep(this.speed);
			} catch (InterruptedException ignore) {
				this.systemOut.println("@AgentProcessor.executeEvent() " +
						"Slow down process with speed: "
						+ this.speed + " =>" + ignore);
			}

			// suspend the process and/or hit breakpoint
			if (!this.pause) {
				AgentEvent e = (AgentEvent) this.queue.topEvent();
				
/*			
				Node thisNode = this.graph.getNode(e.getNodeId());
				if (thisNode.getBreakpoint() == true) {
					this.pause = true;
					while (this.pause) {
						if (this.stop)
							break;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException ignore) {
							this.systemOut.println("@AgentProcessor.executeEvent()" +
									" breakpoint=true: " + ignore);
						}
					}
				}
*/
				
				if (this.stop){
					break;
				}
				if (e.getEventType() == IConstants.EVENT_ARRIVAL_TYPE) {
					this.invokeArrival(this.queue.popEvents());					

				} else if (e.getEventType() == IConstants.EVENT_ALARM_RING_TYPE) {
					this.invokeAlarmRing(this.queue.popEvents());

				} else if (e.getEventType() == IConstants.EVENT_NOTIFY_TYPE) {
					this.invokeNotify(this.queue.popEvents());

				} else {
					this.invokeInit(this.queue.popEvents());
				}

				if (this.stepForward) {
					this.pause = true;
				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {}
			}
		}
	}
	
	/*
	 * Invoke client's init()
	 */
	private void invokeInit(List<Event> events) throws DisJException {
		for (int i = 0; i < events.size(); i++) {
			AgentEvent e = (AgentEvent)events.get(i);
			Agent agent = this.allAgents.get(e.getAgentId());
			Node n = agent.getCurNode();
			
			// Will NOT execute a Fail node or agent
			if (!n.isAlive() || !agent.isAlive()){
				// node dies agent must die as well
				agent.setAlive(false);
				continue;
			}
			AgentModel entity = agent.getClientEntity();
			agent.setHasInitExec(true);
			entity.init();	
			this.systemOut.println("@invokeInit() " + agent.getAgentId());
		}
	}
	
	/*
	 * Internal clock ring, invoke client's alarmRing()
	 */
	private void invokeAlarmRing(List<Event> events) throws DisJException {
		for (int i = 0; i < events.size(); i++) {
			AgentEvent e = (AgentEvent)events.get(i);
			Agent agent = this.allAgents.get(e.getAgentId());
			Node n = agent.getCurNode();
			
			// Will NOT execute a Fail node or agent
			if(!agent.isAlive() || !n.isAlive()){
				// node dies agent must die as well
				agent.setAlive(false);
				continue;
			}

			AgentModel entity = agent.getClientEntity();
			if(agent.hasInitExec()){
				// update log
				String[] value = {n.getNodeId(), null};
				this.log.logAgent(logTag.AGENT_AWAKE, agent.getAgentId(), value);
				
				// execute action
				entity.alarmRing();
			}
			this.systemOut.println("@invokeAlarmRing() " + agent.getAgentId());						
		}
	}
	
	private void invokeArrival(List<Event> events)throws DisJException {
		
		for (int i = 0; i < events.size(); i++) {
			AgentEvent e = (AgentEvent)events.get(i);
			IMessage info = e.getInfo();
			String linkId = (String)info.getContent();
			Agent agent = this.allAgents.get(e.getAgentId());
			
			// retrieve node and port of arrival
			Edge link = this.graph.getEdge(linkId);
			Node node = this.graph.getNode(e.getNodeId());
			String port = node.getPortLabel(link);

			// Will NOT execute a Fail node or agent
			if (!node.isAlive() || !agent.isAlive()){
				// node dies agent must die as well
				agent.setAlive(false);
				continue;
			}

			// check if the port is blocked
			if (node.isBlocked(port) == true) {
				// add event to a block queue
				node.addEventToBlockedList(port, e);
				// this.updateBlockedLog(recv, port, e.getMessage().getLabel());

				// not allow to execute receive msg if it is init node and
				// it has not yet execute init()
			} else {
				// update log
				String[] value = new String[2];
				value[0] = node.getNodeId();
				value[1] = port;
				this.log.logAgent(logTag.AGENT_ARRIVE, e.getAgentId(), value);
				
				// track number of agent visit
				//this.graph.countMsgRecv(e.getMessage().getLabel());
				
				// add new arrival agent to a node				
				node.addAgent(agent);
				
				// set this node to be current residence of agent
				agent.setCurNode(node);
				agent.setLastPortEnter(port);
				
				AgentModel entity = agent.getClientEntity();	

				// notify node of arrival							
				entity.notifyEvent(NotifyType.AGENT_ARRIVAL);
				
				// execute client code
				if(agent.hasInitExec()){
					entity.arrive(port);
				}
				
				this.systemOut.println("@invokeArrival()" + agent.getAgentId() 
						+ " arrives " + node.getNodeId());				
			}
		}
	}

	private void invokeNotify(List<Event> events){
		for (int i = 0; i < events.size(); i++) {			
			// get each event
			AgentEvent e = (AgentEvent)events.get(i);
			
			// get notify type
			IMessage info = e.getInfo();
			NotifyType type = (NotifyType)info.getContent();

			// get a node
			Node node = this.graph.getNode(e.getNodeId());
			
			// get agent
			Agent agent = this.graph.getAgent(e.getAgentId());

			// Will NOT execute a Fail agent or node
			if (!agent.isAlive() || !node.isAlive()){
				// node dies agent must die as well
				agent.setAlive(false);
				continue;
			}
			
			AgentModel entity = agent.getClientEntity();
			if(agent.hasInitExec()){
				entity.notified(type);
			}
			
			// update log
			String[] value = {node.getNodeId(), type.toString()};
			this.log.logAgent(logTag.AGENT_NOTIFY, agent.getAgentId(), value);
		}
	}

	public int getCurrentTime() {
		return this.timeGen.getCurrentTime(this.graph.getId());
	}

	public void setCurrentTime(int time) {
		this.timeGen.setCurrentTime(this.graph.getId(), time);
	}
	
	public int getNextId(){
		int id = -1;
		try{ 
			id = this.timeGen.getNextNewId(this.graph.getId());
		}catch (DisJException e){
			this.systemOut.println("@AgentProcessor.getNextId() [critical]  " 
					+ e.toString());
		}
		return id;
	}

}
