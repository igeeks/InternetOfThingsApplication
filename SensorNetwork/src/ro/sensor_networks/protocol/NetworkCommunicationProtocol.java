package ro.sensor_networks.protocol;

import ro.sensor_networks.xbee.protocols.IncompleteMessageException;

import java.util.ArrayList;
import java.util.List;

public abstract class NetworkCommunicationProtocol {
    /**
     *
     * @param data
     * @param maxPayloadLength
     * @return
     */
    public abstract List<ProtocolPayload> getProtocolPackets(byte[] data, int maxPayloadLength);

    /**
     *
     * @param packets
     * @return
     */
    //TODO: Add exceptions for cases when the message can not be obtained
    public abstract byte[] getMessage(List<ProtocolPayload> packets) throws IncompleteMessageException;

    public abstract boolean isMessageComplete(ArrayList<ProtocolPayload> protocolPacketList);
}
