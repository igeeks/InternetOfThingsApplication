package ro.sensor_networks;

import java.util.EventListener;

public interface NetworkEventListener extends EventListener{

	void nodeAdded(NetworkEvent event);
	void nodeRemoved(NetworkEvent event);
	void networkUpdated(NetworkEvent event);
	
}
