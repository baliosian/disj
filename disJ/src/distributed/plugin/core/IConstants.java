/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.core;

/**
 * @author Me
 *
 * A collection for all constance values that used in this project
 */
public interface IConstants {

	// message direction support for port
	public static final short IN_DIRECTION = 0;
	public static final short OUT_DIRECTION = 1;
	public static final short BI_DIRECTION = 2;
	public static final short UNI_DIRECTION = 3;
	
	// message flow supported types
	public static final short FIFO_TYPE = 0;
	public static final short NO_ORDER_TYPE = 1;
	
	// message delay time supported types
	public static final short GLOBAL_SYNCHRONOUS = 0;
	public static final short GLOBAL_RANDOM_UNIFORM = 1;
	public static final short GLOBAL_RANDOM_POISSON = 2;
	public static final short GLOBAL_RANDOM_CUSTOMS= 3;
	public static final short GLOBAL_CUSTOMS = 4;
	
	public static final short LOCAL_FIXED = 0;
	public static final short LOCAL_RANDOM_UNIFORM = 1;
	public static final short LOCAL_RANDOM_POISSON = 2;
	public static final short LOCAL_RANDOM_CUSTOMS= 3;
	
	// the priority order of execution	
	public static final short INITIATE_TYPE = 0;
	public static final short ALARM_RING_TYPE = 1;
	public static final short RECEIVE_MSG_TYPE = 2;
		
	// Catched exception messages
	public static final short ERROR_0 = 0;
	public static final short ERROR_1 = 1;
	public static final short ERROR_2 = 2;
	public static final short ERROR_3 = 3;
	public static final short ERROR_4 = 4;
	public static final short ERROR_5 = 5;
	public static final short ERROR_6 = 6;
	public static final short ERROR_7 = 7;
	public static final short ERROR_8 = 8;
	public static final short ERROR_9 = 9;
	public static final short ERROR_10 = 10;
	public static final short ERROR_11 = 11;
	public static final short ERROR_12 = 12;
	public static final short ERROR_13 = 13;
	public static final short ERROR_14 = 14;
	public static final short ERROR_15 = 15;
	public static final short ERROR_16 = 16;
	public static final short ERROR_17 = 17;
	public static final short ERROR_18 = 18;
	public static final short ERROR_19 = 19;
	public static final short ERROR_20 = 20;
	public static final short ERROR_21 = 21;
	public static final short ERROR_22 = 22;
	public static final short ERROR_23 = 23;
	public static final short ERROR_24 = 24;
	public static final short ERROR_25 = 25;
	
	
	// Non catch exception message
	public static final short JAVA_BASE_ERROR = -1;
	public static final String RUNTIME_ERROR_0 = "Parameter cannot be null";

	// general constants
	public static final int SPEED_MAX_RATE = 100;
	public static final int SPEED_MIN_RATE = 10000;
	public static final int SPEED_DEFAULT_RATE = 1000;
	public static final long SERIALIZE_VERSION = 1;
	public static final int MAX_RANDOM_RANGE = 32;
	public static final String DELIMETER = "[=]";
	public static final String MAIN_DELIMETER = "{%}";
	public static final String SUB_DELIMETER = "(#)";
	public static final String SET_ALARM_CLOCK = "ALARM_CLOCK";
	public static final String SET_BLOCK_MSG = "BLOCK_MSG";
	public static final String SEND_TAG = "<send>";
	public static final String RECV_TAG = "<recv>";
	public static final String EDGE_TAG = "<edge>";
	public static final String STATE_NOT_FOUND = "State Not found";
	
	public static final String PROPERTY_CHANGE_NODE_STATE = "node_state_changed";
    public static final String PROPERTY_CHANGE_NODE = "node";
    public static final String PROPERTY_CHANGE_EDGE = "edge";
    public static final String PROPERTY_CHANGE_BOUND = "bound";
    public static final String PROPERTY_CHANGE_INPUT = "input";
    public static final String PROPERTY_CHANGE_OUTPUT = "output";
    public static final String PROPERTY_CHANGE_SIZE = "size";
    public static final String PROPERTY_CHANGE_LOCATION = "location";
    public static final String PROPERTY_CHANGE_BENDPOINT = "bendpoint";
    public static final String PROPERTY_CHANGE__LINK_INVISIBLE = "link_invisible";
    public static final String PROPERTY_CHANGE_NAME = "name";
	
	
	
	
	
}