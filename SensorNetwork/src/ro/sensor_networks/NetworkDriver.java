package ro.sensor_networks;

import ro.sensor_networks.exceptions.NetworkInitializationException;

import java.util.List;

/**
 * @startuml
 * interface NetworkDriver{
 *      void begin() throws NetworkInitializationException
 *      void close()
 *      NetworkServer getServer()
 *      NetworkClientConnection getClientConnection(NetworkNode remoteNode)
 *      NetworkNode getLocal()
 * }
 *
 * @enduml
 */

public interface NetworkDriver {
	void begin() throws NetworkInitializationException;	// initializes the network resources
	void close();										// releases all resources
	NetworkServer getServer();							// returns a local server instance		
	NetworkClientConnection getClientConnection(NetworkNode remoteNode);	// returns a client object that can be used to connect to a remote server
	NetworkNode getLocal();								// returns the local node data
    List<NetworkNode> getNetworkNodes();
}
