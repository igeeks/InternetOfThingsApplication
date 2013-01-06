package ro.sensor_networks;

/**
 * @startuml
 * interface NetworkAddress
 * NetworkAddress : byte[] getValue
 * @enduml
 */
public interface NetworkAddress {
     byte[] getValue();
}
