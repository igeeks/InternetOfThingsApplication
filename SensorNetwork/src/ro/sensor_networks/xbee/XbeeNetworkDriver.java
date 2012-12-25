package ro.sensor_networks.xbee;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sensor_networks.NetworkClient;
import ro.sensor_networks.NetworkDriver;
import ro.sensor_networks.NetworkEvent;
import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.NetworkNode;
import ro.sensor_networks.NetworkRequest;
import ro.sensor_networks.NetworkResponse;
import ro.sensor_networks.NetworkServer;
import ro.sensor_networks.exceptions.NetworkInitializationException;
import ro.sensor_networks.xbee.XBeeNetworkNode.NetworkStatus;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeConfiguration;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.NodeDiscover;
import com.rapplogic.xbee.util.ByteUtils;

public class XbeeNetworkDriver implements NetworkDriver {

	private static final Logger logger = LoggerFactory.getLogger(XbeeNetworkDriver.class);
	private static final int DEFAULT_NETWORK_REFRESH_PERIOD=30; // ten minutes refresh period in seconds
	private static final int DEFAULT_UPDATE_RETRIES=5;
	
	private XBee xbee; // xbee device
	private ExecutorService xbeeTransmissionService; // single thread executor used to execute transmission tasks for the xbee
	private ScheduledExecutorService  timedExecutor; // executor used to call for periodic execution of certain network tasks
	private XbeeNetworkModel networkModel;
	
	// networkServices
	private NetworkUpdateService networkUpdateService;
	
	/**
	 * 
	 * @param serialPortId
	 * @param speed
	 * @param refreshPeriod - in seconds
	 */
	public XbeeNetworkDriver(String serialPortId, int speed, int refreshPeriod) {
		super();
	
		networkModel = new XbeeNetworkModel();
		networkModel.setSerialPortId(serialPortId);
		networkModel.setSpeed(speed);
		networkModel.setRefreshPeriod(refreshPeriod);

	}

	public XbeeNetworkDriver(String serialPortId, int speed) {
		this(serialPortId, speed, DEFAULT_NETWORK_REFRESH_PERIOD);
	}
	
	public void begin() throws NetworkInitializationException{
		logger.debug("Start Xbee Network Driver");
		try{
			startXbee();
			startNetworkServices();
		}catch(XBeeException xbe){
			logger.debug("-->",xbe);
			throw new NetworkInitializationException("Could not initliaze the xbee network");
		}
	}
	
	public void close(){
		logger.debug("Stop Xbee Network Driver");
		stopNetworkServices();
		stopXbee();
	}

	
	public void addNetworkListener(NetworkEventListener listener){
		networkModel.addNetworkListener(listener);
	}
	
	public void removeNetworkListener(NetworkEventListener listener){
		networkModel.removeNetworkListener(listener);
	}
	
	public void setRefreshPeriod(int refreshPeriod) {
		networkModel.setRefreshPeriod(refreshPeriod);
		stopNetworkServices();
		startNetworkServices();
	}

	public Enumeration<XBeeNetworkNode> getNetworkNodes(){

		return networkModel.getXbeeNodeList().elements();

	}

//	/**
//	 * Send data to a node 
//	 * @param data
//	 * @return
//	 */
//	public NetworkResponse sendSynchronous(XBeeNetworkNode destinationNode, final NetworkRequest request) throws IllegalArgumentException{
//		if(destinationNode == null || request == null){
//			throw new IllegalArgumentException("sendSynchronous parameters are illegal");
//		}
//		
//		NetworkResponse result = null;
//		
//		pushToXbeeService(new Runnable() {
//			
//			@Override
//			public void run() {
//				try {
//					
//					// convert Data
//					
//					int[] payloadData = ByteUtils.stringToIntArray(request.getRequestData());
//					
//					
//										
//					// send Packet Data 
//					
//					
//					
//					XBeePacket packet = new XBeePacket();
//					
//					xbee.sendPacket(packet);
//				    
//				} catch (IOException e) {
//					
//					logger.warn("Could convert the data to byte array");
//				}
//				
//				
//				
//		}});
//		
//		return result;
//		
//	}
	

	/**
	 * Starts the xbee communication device
	 * @param serialPortId
	 * @param speed
	 * @throws NetworkInitializationException
	 */
	private void startXbee() throws XBeeException{
		
			this.xbee = new XBee(new XBeeConfiguration().withStartupChecks(false));
            
			Runtime.getRuntime().addShutdownHook(new Thread() {
			    public void run() { 
			    	if (xbee.isConnected()) {
			    		logger.info("ShutdownHook is closing connection");
			    		xbee.close();
			    	}
			    }
			});
			
			xbee.open(networkModel.getSerialPortId(), networkModel.getSpeed());
            
            logger.debug("Started the xbee device at {} with {}", networkModel.getSerialPortId(), networkModel.getSpeed());
	}
	
	/**
	 * Stops the xbee communication device
	 */
	private void stopXbee(){
		if(xbee != null){
			xbee.close();
		}
	}
	
	
	
	
	private void startNetworkServices() {
		xbeeTransmissionService = Executors.newSingleThreadExecutor();
		timedExecutor = Executors.newSingleThreadScheduledExecutor();
		
		networkUpdateService = new NetworkUpdateService(DEFAULT_UPDATE_RETRIES);
		
		timedExecutor.scheduleAtFixedRate(networkUpdateService, 0, networkModel.getRefreshPeriod(), TimeUnit.SECONDS);
	}
	
	private void stopNetworkServices() {
		shutdownExecutor(timedExecutor);
		logger.debug("Stoped the timed Executor");
		shutdownExecutor(xbeeTransmissionService);
		logger.debug("Stoped the xbeeTransmissionService Executor");
	}
			
	
	private void shutdownExecutor(ExecutorService executor) {
		executor.shutdown();
		
		try {
		     // Wait a while for existing tasks to terminate
		     if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
		    	 executor.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		         if (!executor.awaitTermination(60, TimeUnit.SECONDS))
		           logger.warn("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
			   executor.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		logger.debug("Shutdown executor finished");
	}

	private void pushToXbeeService(Runnable runnable) {

		xbeeTransmissionService.execute(runnable);
		
	}
	
	
	private void fireNetworkUpdatedEvent() {
		NetworkEvent updateEvent = new NetworkEvent(this);
		for(NetworkEventListener listener: networkModel.getNetworkListeners()){
			listener.networkUpdated(updateEvent);
		}
		
	}
	
	private class NetworkUpdateService implements Runnable{

		private int updateRetries;
		
		public NetworkUpdateService(int updateRetries) {
			
			this.updateRetries = updateRetries;
		}


		@Override
		public void run(){
			
			pushToXbeeService(new Runnable() {
				
				@Override
				public void run() {
					for(int i=0;i<updateRetries;i++){
						if(updateNetworkNodes()){
							fireNetworkUpdatedEvent();
							break;
						}
						logger.debug("Retrying the discovery");
					}
			}});				
		}
	

		private boolean updateNetworkNodes(){
			boolean result = false;
			
			try {
    			// get the Node discovery timeout
				changeNetworkNodeStatusToDown(NetworkStatus.DOWN);
				
				xbee.sendAsynchronous(new AtCommand("NT"));

				AtCommandResponse nodeTimeout = (AtCommandResponse) xbee.getResponse(5000);
				
				// default is 6 seconds
				long nodeDiscoveryTimeout = ByteUtils.convertMultiByteToInt(nodeTimeout.getValue()) * 100;
				
				logger.debug("Node discovery timeout is {}  milliseconds" ,nodeDiscoveryTimeout );
						
				PacketListener pl = new PacketListener() {
					
					public void processResponse(XBeeResponse response) {
						if (response.getApiId() == ApiId.AT_RESPONSE) {
							NodeDiscover nd = NodeDiscover.parse((AtCommandResponse)response);
							
							if(!networkModel.containsNode(nd)){
								networkModel.addXbeeNode(nd, System.currentTimeMillis(), NetworkStatus.UP);
							}else
							{
								networkModel.getXbeeNode(nd).setNodeStatus(NetworkStatus.UP);
								networkModel.getXbeeNode(nd).setTimestamp(System.currentTimeMillis());
							}
							
							
							logger.debug("Node discover response is:{} ", nd);
						} else {
							logger.debug("Ignoring unexpected response: {}" + response);	
						}					
					}
					
				};
				
				xbee.addPacketListener(pl);
							
				logger.debug("Sending node discover command");
				xbee.sendAsynchronous(new AtCommand("ND"));
				
				// wait for nodeDiscoveryTimeout milliseconds
				Thread.sleep(nodeDiscoveryTimeout);
				
				logger.debug("Time is up!  You should have heard back from all nodes by now.  If not make sure all nodes are associated and/or try increasing the node timeout (NT)");
				xbee.removePacketListener(pl);
				
				result = true;
			} //XBee
			catch(Exception e){			
				logger.debug("XBeeException during discovery updateNetworkNodes:", e);
				return false;
			}

			return result;
		}

	}

	public void changeNetworkNodeStatusToDown(NetworkStatus status) {
		
		networkModel.changeNetworkNodeStatus(status);
		
	}

	@Override
	public NetworkServer getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkClient getClient(NetworkNode remoteNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkNode getLocal() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
