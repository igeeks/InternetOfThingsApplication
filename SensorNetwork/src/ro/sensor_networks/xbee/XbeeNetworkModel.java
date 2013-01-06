package ro.sensor_networks.xbee;

import javax.swing.DefaultListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.zigbee.NodeDiscover;

import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.xbee.XBeeNetworkNode.NetworkStatus;

import java.util.ArrayList;

public class XbeeNetworkModel {
	public static final String NODE_LIST="NODE_LIST";
	private ArrayList<XBeeNetworkNode> xbeeNodeList;
	
	private int refreshPeriod; // the network refresh period
	private String serialPortId;
	private int speed;
	
	private EventListenerList changeSupport;

    private XBeeNetworkNode localNode;
    private int maximumPayloadValue;
	public XbeeNetworkModel() {

		xbeeNodeList = new ArrayList<XBeeNetworkNode>();
		changeSupport = new EventListenerList();
	}

	
	
	public int getRefreshPeriod() {
		return refreshPeriod;
	}



	public void setRefreshPeriod(int refreshPeriod) {
		this.refreshPeriod = refreshPeriod;
	}



	public String getSerialPortId() {
		return serialPortId;
	}



	public void setSerialPortId(String serialPortId) {
		this.serialPortId = serialPortId;
	}



	public int getSpeed() {
		return speed;
	}



	public void setSpeed(int speed) {
		this.speed = speed;
	}



	public void addNetworkModelListener(ListDataListener listener){
		changeSupport.add(ListDataListener.class, listener);
	}
	
	public void removeNetworkModelListener(ListDataListener listener){
		changeSupport.remove(ListDataListener.class, listener);
	}
	
	public void addNetworkListener(NetworkEventListener listener){
		changeSupport.add(NetworkEventListener.class, listener);
	}
	
	public void removeNetworkListener(NetworkEventListener listener){
		changeSupport.remove(NetworkEventListener.class, listener);
	}
	
	public NetworkEventListener[] getNetworkListeners(){
		return changeSupport.getListeners(NetworkEventListener.class);
	}
	
	public void addXbeeNode(XBeeNetworkNode node){
		// id  the network nodes by the 64bit unique address
		if(node != null && findNodeWith64Address(node.getNodeInfo().getNodeAddress64()) == null)
		{
			xbeeNodeList.add(node);
			fireNodeAddedEvent(node);
		}
		 
	}
	
	public void removeXbeeNode(XBeeNetworkNode node){
		
		XBeeNetworkNode toRemoveNode = findNodeWith64Address(node.getNodeInfo().getNodeAddress64());
		
		if(toRemoveNode == null){
			return;
		}
		
		if(xbeeNodeList.remove(toRemoveNode)){
			fireNodeRemovedEvent(toRemoveNode);
		}
	}
	
	private XBeeNetworkNode findNodeWith64Address(XBeeAddress64 nodeAddress64) {
		XBeeNetworkNode result =null;
		for(int index = 0; index < xbeeNodeList.size(); index++){
			XBeeNetworkNode node = xbeeNodeList.get(index);
			if(node.getNodeInfo().getNodeAddress64().equals(nodeAddress64))
			{
				result = node;
				break;
			}
		}
		return result;
	}



	public ArrayList<XBeeNetworkNode> getXbeeNodeList() {
		return xbeeNodeList;
	}

	public void setXbeeNodeList(ArrayList<XBeeNetworkNode> xbeeNodeList) {
		this.xbeeNodeList = xbeeNodeList;
	}
	
	
	private void fireNodeRemovedEvent(XBeeNetworkNode node) {
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, -1, -1);
		for(ListDataListener listener:changeSupport.getListeners(ListDataListener.class)){
			listener.intervalRemoved(event);
		}
	}
	

	private void fireNodeAddedEvent(XBeeNetworkNode node) {
		int index = xbeeNodeList.indexOf(node);
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index , index);
		for(ListDataListener listener:changeSupport.getListeners(ListDataListener.class)){
			listener.intervalAdded(event);
		}
	}



	public void addXbeeNode(NodeDiscover nd, long timestamp, NetworkStatus status) {
		XBeeNetworkNode node = new XBeeNetworkNode(nd);
		
		node.setTimestamp(timestamp);
		node.setNodeStatus(status);
		
		addXbeeNode(node);
	}



	public XBeeNetworkNode getXbeeNode(NodeDiscover nd) {
		return findNodeWith64Address(nd.getNodeAddress64());
		
	}


	public boolean containsNode(NodeDiscover nd) {
		if(findNodeWith64Address(nd.getNodeAddress64()) != null)
			return true;
		else
			return false;
	}


    /**
     * Changes the network status for the discovered nodes
     * @param status - new node status
     */
	public void changeNetworkNodeStatus(NetworkStatus status) {
		for(int index = 0; index < xbeeNodeList.size(); index++){
			XBeeNetworkNode node = xbeeNodeList.get(index);
			node.setNodeStatus(status);
		}
		
	}

    public XBeeNetworkNode getLocalNode() {
        return localNode;
    }

    public void setLocalNode(XBeeNetworkNode localNode) {
        this.localNode = localNode;
    }

    public void setMaximumPayloadValue(int maximumPayloadValue) {
        this.maximumPayloadValue = maximumPayloadValue;
    }

    public int getMaximumPayloadValue() {
        return maximumPayloadValue;
    }

    @Override
    public String toString() {
        return "XbeeNetworkModel{" +
                "refreshPeriod=" + refreshPeriod +
                ", serialPortId='" + serialPortId + '\'' +
                ", speed=" + speed +
                ", localNode=" + localNode +
                ", maximumPayloadValue=" + maximumPayloadValue +
                '}';
    }
}
