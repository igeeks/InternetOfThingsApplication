package ro.sensor_networks;

/**
 * Describes the methods necessary to initiate a communication session
 * with a remote network node
 */
public interface NetworkClientConnection {
	boolean connect(); 				   // connects to a network node
	boolean isConnected();			   // returns true if the client is connected, false otherwise
	void write(byte[] data);		   // sends data to a connected node
	int available();			       // returns the number of bytes available if any data is available as response for a previous message
	int read(byte[] buffer);		   // obtains the data available
	void stop();					   // disconnects from the node 
}
