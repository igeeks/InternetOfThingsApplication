package ro.sensor_networks.xbee.protocols;

import ro.sensor_networks.protocol.NetworkCommunicationProtocol;
import ro.sensor_networks.protocol.ProtocolPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the functionality for a simple data transmission protocol. The data is sequenced and an sequence number is added header is added
 */
public class SimpleDataTransferProtocol extends NetworkCommunicationProtocol {
    @Override
    public List<ProtocolPayload> getProtocolPackets(byte[] data, int maxPayloadLength) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getMessage(List<ProtocolPayload> packets) throws IncompleteMessageException{
        return new byte[0];
    }

    @Override
    public boolean isMessageComplete(ArrayList<ProtocolPayload> protocolPacketList) {



        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private class ProtocolPayloadImplementation implements  ProtocolPayload{

        @Override
        public byte[] getContent() {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
