package ro.sensor_networks.xbee;

import javax.swing.DefaultListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.zigbee.NodeDiscover;

import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.xbee.XBeeNetworkNode.NetworkStatus;

public class XbeeNetworkModel {
	public static final String NODE_LIST="NODE_LIST";
	private DefaultListModel<XBeeNetworkNode> xbeeNodeList;
	
	private int refreshPeriod; // the network refresh period
	private String serialPortId;
	private int speed;
	
	private EventListenerList changeSupport;
		
	public XbeeNetworkModel() {

		xbeeNodeList = new DefaultListModel<XBeeNetworkNode>();
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
			xbeeNodeList.addElement(node);
			fireNodeAddedEvent(node);
		}
		 
	}
	
	public void removeXbeeNode(XBeeNetworkNode node){
		
		XBeeNetworkNode toRemoveNode = findNodeWith64Address(node.getNodeInfo().getNodeAddress64());
		
		if(toRemoveNode == null){
			return;
		}
		
		if(xbeeNodeList.removeElement(toRemoveNode)){
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



	public DefaultListModel<XBeeNetworkNode> getXbeeNodeList() {
		return xbeeNodeList;
	}

	public void setXbeeNodeList(DefaultListModel<XBeeNetworkNode> xbeeNodeList) {
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



	public void changeNetworkNodeStatus(NetworkStatus status) {
		for(int index = 0; index < xbeeNodeList.size(); index++){
			XBeeNetworkNode node = xbeeNodeList.get(index);
			node.setNodeStatus(status);
		}
		
	}

	
}
