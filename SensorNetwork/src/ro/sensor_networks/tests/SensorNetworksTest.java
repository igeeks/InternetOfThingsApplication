package ro.sensor_networks.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ro.sensor_networks.NetworkClientConnection;
import ro.sensor_networks.NetworkEvent;
import ro.sensor_networks.NetworkEventListener;
import ro.sensor_networks.NetworkNode;
import ro.sensor_networks.exceptions.NetworkInitializationException;
import ro.sensor_networks.xbee.XbeeNetworkDriver;

public class SensorNetworksTest {
	private static final Logger logger = LoggerFactory.getLogger(SensorNetworksTest.class);
	private static final String DEFAULT_SERIAL_PORT = "/dev/ttyAMA0";

    private static final String DEFAULT_LONG_MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
	private static final String TEST_MESSAGE = "{test:true}";
    private static final int DEFAULT_SPEED = 115200;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		logger.info("--Starting the SensorNetworksTests -- ");

//		testNetworkDiscovery();
        testNetworkClient();
//        testNetworkServer();
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
            public void networkUpdated(NetworkEvent event) {
                logger.info("Network updated!");

				List<NetworkNode> nodes = xbeeDriver.getNetworkNodes();

				int i= 1;
				for(NetworkNode node: nodes){
					logger.info("{}) {}", i++, node);
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

            @Override
			public void nodeAdded(NetworkEvent event) {
				logger.info("Node dicovered:{}", event.getSource());

			}

		});

		try{
			xbeeDriver.begin();
		}catch(NetworkInitializationException nie){
			logger.warn("Could not start the sensor network:", nie);
		}
	}

    private static void testNetworkClient() {
        final XbeeNetworkDriver xbeeDriver = new XbeeNetworkDriver(DEFAULT_SERIAL_PORT, DEFAULT_SPEED);

        try{
            // start the xbee network driver
            xbeeDriver.begin();

            // wait for discovery 10s
            Thread.sleep(10000);

            // list of the discovered network nodes
            List<NetworkNode> nodes = xbeeDriver.getNetworkNodes();

            logger.debug("{} network nodes discovered", nodes.size());

            // send test message to network clients
            for(NetworkNode node: nodes){
                // init a connection to the destination node
                NetworkClientConnection clientConnection = xbeeDriver.getClientConnection(node);

                // starts the connection to the destination node
                clientConnection.connect();

                // if the connection is enabled
                if(clientConnection.isConnected()){
                    String message = node.getNetworkAddressAsBytes() + " -> " + DEFAULT_LONG_MESSAGE;
                    byte[] payloadMessage = message.getBytes("UTF-8");
                    logger.debug("Send message ({} bytes):{} ", payloadMessage.length, message);

                    // writes the message
                    clientConnection.write(payloadMessage);

                    // Wait until a response is available
                    // poll method
                    // sleep until something is available for read
                    int i=0;
                    while(clientConnection.available()== 0){
                        Thread.sleep(100);
                        i++;
                        if(i > 5){
                            logger.debug("Timeout during waiting for response");
                            break;
                        }
                    }

                    ByteArrayOutputStream outputStream  = new ByteArrayOutputStream();

                    while(clientConnection.available()!=0)
                    {
                       byte[] buffer = new byte[clientConnection.available()];

                       // read the data
                       clientConnection.read(buffer);

                       outputStream.write(buffer);
                    }

                    String responseMessage = new String(outputStream.toByteArray());

                    logger.debug("Receive message ({} bytes):{}", outputStream.size(), responseMessage);

                }

            }
        }catch(NetworkInitializationException nie){
            logger.warn("Could not start the sensor network:", nie);
        }
        catch(UnsupportedEncodingException uee){
            logger.warn("Unsupported encoding:{}", uee);
        } catch (InterruptedException e) {
            logger.warn("Unsupported encoding:{}", e);
        }
        catch (IOException ioe){
            logger.warn("IOE exception:{}", ioe);
        }
        finally {
            xbeeDriver.close();
        }
    }


}
