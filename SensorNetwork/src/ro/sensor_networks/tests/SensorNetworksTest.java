package ro.sensor_networks.tests;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ro.sensor_networks.NetworkEvent;
import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.exceptions.NetworkInitializationException;
import ro.sensor_networks.xbee.XBeeNetworkNode;
import ro.sensor_networks.xbee.XbeeNetworkDriver;

public class SensorNetworksTest {
	private static final Logger logger = LoggerFactory.getLogger(SensorNetworksTest.class);
	private static final String DEFAULT_SERIAL_PORT = "/dev/ttyAMA0";
	private static final int DEFAULT_SPEED = 115200;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		logger.info("--Starting the SensorNetworksTests -- ");
		
//		testStartStopDiscovery();
		testNetworkDiscovery();
	}
	
	private static void testNetworkDiscovery() {
		logger.info("-Started the testNetworkDiscovery -- ");	
		final XbeeNetworkDriver xbeeDriver = new XbeeNetworkDriver(DEFAULT_SERIAL_PORT, DEFAULT_SPEED);
	
		
		xbeeDriver.addNetworkListener(new NetworkEventListener() {
			int counts = 5;
			@Override
			public void nodeRemoved(NetworkEvent event) {
				logger.info("Node removed:{}", event.getSource());
				
			}
			
			@Override
			public void nodeAdded(NetworkEvent event) {
				logger.info("Node dicovered:{}", event.getSource());
				
			}
			
			@Override
			public void networkUpdated() {
				logger.info("Network updated!");
				
				Enumeration<XBeeNetworkNode>  nodes = xbeeDriver.getNetworkNodes();
				
				int i= 1;
				while(nodes.hasMoreElements()){
					logger.info("{}) {}", i++, nodes.nextElement());
				}
				
				if(counts == 0)
				{
					xbeeDriver.close();
				}
				else{
					counts--;
				}
				
				logger.info("-Ended the testNetworkDiscovery test. Remaining {}:", counts);
			}
		});
		
		try{
			xbeeDriver.begin();
		}catch(NetworkInitializationException nie){
			logger.warn("Could not start the sensor network:", nie);
		}
			
	}
	
//	private static void testStartStopDiscovery() {
//
//
//		
//		
//	}

}
