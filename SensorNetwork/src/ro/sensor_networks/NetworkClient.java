package ro.sensor_networks;

public interface NetworkClient {
	boolean connect(); 				   // connects to a network node
	boolean isConnected();			   // returns true if the client is connected, false otherwise
	boolean write(byte[] data);		   // sends data to a connected node
	int available();			       // returns the number of bytes available if any data is available as response for a previous message
	byte[] read();					   // obtains the data available
	void stop();					   // disconnects from the node 
}
