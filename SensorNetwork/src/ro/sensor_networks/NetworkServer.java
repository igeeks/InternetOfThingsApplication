package ro.sensor_networks;

public interface NetworkServer {
	boolean begin();		 	// tells the server to start listening for connections
	NetworkClient available();	// returns the client currently connected to the server 
	void broadcast(byte[] data);// broadcast a message to all the clients connected	
}
