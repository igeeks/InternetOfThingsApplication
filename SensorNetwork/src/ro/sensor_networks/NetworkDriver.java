package ro.sensor_networks;

import ro.sensor_networks.exceptions.NetworkInitializationException;

public interface NetworkDriver {
	void begin() throws NetworkInitializationException;	// initializes the network resources
	void close();										// releases all resources
	NetworkServer getServer();							// returns a local server instance		
	NetworkClient getClient(NetworkNode remoteNode);	// returns a client object that can be used to connect to a remote server
	NetworkNode getLocal();								// returns the local node data
}
