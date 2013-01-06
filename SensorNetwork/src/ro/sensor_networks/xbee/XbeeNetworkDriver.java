package ro.sensor_networks.xbee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.rapplogic.xbee.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sensor_networks.NetworkClientConnection;
import ro.sensor_networks.NetworkDriver;
import ro.sensor_networks.NetworkEvent;
import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.NetworkNode;
import ro.sensor_networks.NetworkServer;
import ro.sensor_networks.exceptions.NetworkInitializationException;
import ro.sensor_networks.ro.sensor_networks.utils.Utils;
import ro.sensor_networks.xbee.XBeeNetworkNode.NetworkStatus;

import com.rapplogic.xbee.api.zigbee.NodeDiscover;
import com.rapplogic.xbee.util.ByteUtils;
import ro.sensor_networks.xbee.protocols.SimpleDataTransferProtocol;
import ro.sensor_networks.xbee.streams.XBeeSeries2InputStream;
import ro.sensor_networks.xbee.streams.XBeeSeries2OutputStream;

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

    // network objects
    private XbeeNetworkServer networkServer;

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

            // Read the local xbee configuration and stores it in the networkModel.localNode
            readLocalXbeeConfiguration();

            startNetworkServices();

//            if(networkModel.getLocalNode().getNodeInfo().getDeviceType() !=  NodeDiscover.DeviceType.DEV_TYPE_COORDINATOR){
//                throw new NetworkInitializationException("The Xbee device is not set as a coordinator!");
//            }

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

	public List<NetworkNode> getNetworkNodes(){
		return new ArrayList<NetworkNode>(networkModel.getXbeeNodeList());
	}

    @Override
    public NetworkServer getServer() {
        if(networkServer == null){
            networkServer = new XbeeNetworkServer();
        }

        return networkServer;
    }

    @Override
    public NetworkClientConnection getClientConnection(NetworkNode remoteNode) {
        XBeeNetworkNode node = null;

        if(remoteNode instanceof XBeeNetworkNode)
        {
            node = (XBeeNetworkNode)remoteNode;
        }
        else
        {
            throw new IllegalArgumentException("A Xbee network driver can connect only to Xbee devices");
        }

        return new XbeeNetworkClientConnection(node);
    }

    @Override
    public NetworkNode getLocal() {

        if(networkModel.getLocalNode() == null){
            try {
                begin();
            } catch (NetworkInitializationException e) {
                logger.warn("Could not get the network coordinator data");
            }
        }

        return networkModel.getLocalNode();
    }


	/**
	 * Starts the xbee communication device
	 * @throws XBeeException
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
		logger.debug("Stopped the timed Executor");
		shutdownExecutor(xbeeTransmissionService);
		logger.debug("Stopped the xbeeTransmissionService Executor");
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

    private boolean updateNetworkNodes(){
        boolean result = false;

        try {
            // get the Node discovery timeout
            networkModel.changeNetworkNodeStatus(NetworkStatus.DOWN);

            xbee.sendAsynchronous(new AtCommand("NT"));

            AtCommandResponse nodeTimeout = (AtCommandResponse) xbee.getResponse(5000);

            // default is 6 seconds
            long nodeDiscoveryTimeout = ByteUtils.convertMultiByteToInt(nodeTimeout.getValue()) * 100;

            logger.debug("Node discovery timeout is {}  milliseconds" ,nodeDiscoveryTimeout );

            PacketListener pl = new PacketListener() {

                public void processResponse(XBeeResponse response) {
                    if (response.getApiId() == ApiId.AT_RESPONSE) {
                        try{
                            NodeDiscover nd = NodeDiscover.parse((AtCommandResponse)response);

                            if(!networkModel.containsNode(nd)){
                                networkModel.addXbeeNode(nd, System.currentTimeMillis(), NetworkStatus.UP);
                            }else
                            {
                                networkModel.getXbeeNode(nd).setNodeStatus(NetworkStatus.UP);
                                networkModel.getXbeeNode(nd).setTimestamp(System.currentTimeMillis());
                            }
                            logger.debug("Node discover response is:{} ", nd);
                        }
                        catch(RuntimeException re){
                             logger.debug("Could not parse the response:", re);
                        }


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

    private void readLocalXbeeConfiguration() {
         try{
             Thread.sleep(100);
             xbee.sendSynchronous(new AtCommand("MY"));

             AtCommandResponse localNetworkAddress = (AtCommandResponse) xbee.getResponse();

//             logger.debug("Local Network Address:{}", Utils.intsToHex(localNetworkAddress.getValue()));

             xbee.sendSynchronous(new AtCommand("SH"));

             AtCommandResponse highNumberAddress = (AtCommandResponse) xbee.getResponse();

//             logger.debug("Unique High Network Address:{}", Utils.intsToHex(highNumberAddress.getValue()));

             xbee.sendSynchronous(new AtCommand("SL"));

             AtCommandResponse lowNumberAddress = (AtCommandResponse) xbee.getResponse();

//             logger.debug("Unique Low Network Address:{}", Utils.intsToHex(lowNumberAddress.getValue()));

             xbee.sendSynchronous(new AtCommand("DD"));

             AtCommandResponse deviceType = (AtCommandResponse) xbee.getResponse();

//             logger.debug("Device type:{}", Utils.intsToHex(deviceType.getValue()));

             xbee.sendSynchronous(new AtCommand("NP"));

             AtCommandResponse byteNumber = (AtCommandResponse) xbee.getResponse();

             logger.debug("Payload Byte size:{}", Utils.intsToHex(byteNumber.getValue()));

             NodeDiscover localNodeData = new NodeDiscover();

             localNodeData.setNodeAddress16(new XBeeAddress16(localNetworkAddress.getValue()));
             int[] networkAddress = new int[64];

             System.arraycopy(highNumberAddress.getValue(),0, networkAddress, 0,highNumberAddress.getValue().length);
             System.arraycopy(lowNumberAddress.getValue(),0,networkAddress, highNumberAddress.getValue().length, lowNumberAddress.getValue().length);

             localNodeData.setNodeAddress64(new XBeeAddress64(networkAddress));
             localNodeData.setDeviceType(NodeDiscover.DeviceType.DEV_TYPE_COORDINATOR);

             byte[] payloadByteArray = new byte[4];

             logger.debug("payload length array:{}", byteNumber.getValue().length);


             payloadByteArray[2] = (byte)(0xFF & byteNumber.getValue()[0]);
             payloadByteArray[3] = (byte)(0xFF & byteNumber.getValue()[1]);

             ByteBuffer payloadWrapper = ByteBuffer.wrap(payloadByteArray);
             networkModel.setMaximumPayloadValue(payloadWrapper.getInt());
             logger.debug("Network Model:{}", networkModel);
         }
         catch(Exception e){
             logger.warn("Could not read the local node configuration:", e);
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




    }

    private class XbeeNetworkServer implements NetworkServer{

        @Override
        public boolean begin() {
            return false;
        }

        @Override
        public NetworkClientConnection available() {
            return null;
        }

        @Override
        public void broadcast(byte[] data) {

        }
    }

    private class XbeeNetworkClientConnection implements NetworkClientConnection {
        private XBeeNetworkNode destinationNetworkNode;
        private boolean connectionStarted;
        private SimpleDataTransferProtocol communicationProtocol;
        private XBeeSeries2InputStream clientInputStream;
        private XBeeSeries2OutputStream clientOutputStream;

        private XbeeNetworkClientConnection(XBeeNetworkNode destinationNetworkNode) {
            this.destinationNetworkNode = destinationNetworkNode;

            // TODO: add possibility of selecting the protocol used for communication
            // initialize the protocol
            communicationProtocol = new SimpleDataTransferProtocol();

            // create XBee streams
            // input stream
            clientInputStream= new XBeeSeries2InputStream(xbee, destinationNetworkNode, communicationProtocol);

            // output stream
            clientOutputStream = new XBeeSeries2OutputStream(xbee, destinationNetworkNode, communicationProtocol);

        }

        @Override
        public boolean connect() {
            connectionStarted = false;

            // protocol initialization

            connectionStarted = true;

            return connectionStarted;
        }

        @Override
        public boolean isConnected() {
            return connectionStarted;
        }

        @Override
        public void write(byte[] data) {
            try{
                clientOutputStream.write(data);
            }
            catch(IOException ioe){
                logger.warn("Could not send the data:", ioe);
            }
        }

        @Override
        public int available() {
            return clientInputStream.available();
        }

        @Override
        public int read(byte[] buffer) {

            try{
               return clientInputStream.read(buffer);

            }
            catch(IOException ioe){
               logger.warn("Could not read data from stream:", ioe);
               return 0;
            }
        }

        @Override
        public void stop() {
             // close the protocol

        }
    }
}
