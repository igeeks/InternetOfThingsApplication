package ro.sensor_networks.xbee;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.rapplogic.xbee.api.zigbee.NodeDiscover;
import com.rapplogic.xbee.api.zigbee.NodeDiscover.DeviceType;

import ro.sensor_networks.NetworkNode;

public class XBeeNetworkNode implements NetworkNode {
	
	public enum NetworkStatus {
		UP (0),
		DOWN (1);
		
		
		private static final Map<Integer,NetworkStatus> lookup = new HashMap<Integer,NetworkStatus>();
		
		static {
			for(NetworkStatus s : EnumSet.allOf(NetworkStatus.class)) {
				lookup.put(s.getValue(), s);
			}
		}
		
		public static NetworkStatus get(int value) { 
			return lookup.get(value); 
		}
		
	    private final int value;
	    
	    NetworkStatus(int value) {
	        this.value = value;
	    }

		public int getValue() {
			return value;
		}
	}
	
	
	private int id;
	
	private NodeDiscover nodeInfo;
	
	private NetworkStatus nodeStatus;
	
	private long lastTimestamp;
	
	public XBeeNetworkNode(){
		this(0,new NodeDiscover());
	}
	
	public XBeeNetworkNode(NodeDiscover nodeInfo) {
		this(0,nodeInfo);
	}

	public XBeeNetworkNode(int id, NodeDiscover nodeInfo) {
		super();
		this.id = id;
		this.nodeInfo = nodeInfo;
		nodeStatus = NetworkStatus.DOWN;
		lastTimestamp = 0;
	}

	
	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public NodeDiscover getNodeInfo() {
		return nodeInfo;
	}


	public void setNodeInfo(NodeDiscover nodeInfo) {
		this.nodeInfo = nodeInfo;
	}

	public NetworkStatus getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeStatus(NetworkStatus nodeStatus) {
		this.nodeStatus = nodeStatus;
	}

	public long getTimestamp() {
		return lastTimestamp;
	}

	public void setTimestamp(long timestamp) {
		this.lastTimestamp = timestamp;
	}

	@Override
	public String toString() {
		return "XBeeNetworkNode [id=" + id + ", nodeInfo=" + nodeInfo
				+ ", nodeStatus=" + nodeStatus + ", lastTimestamp="
				+ lastTimestamp + "]";
	}

	
}
