package ro.sensor_networks;

import java.util.List;

public interface MonitorableNetworkDriver extends NetworkDriver{

	List<NetworkNode> getNodes();
}
