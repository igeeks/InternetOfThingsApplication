package ro.sensor_networks.xbee.streams;

import com.rapplogic.xbee.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.sensor_networks.protocol.NetworkCommunicationProtocol;
import ro.sensor_networks.protocol.ProtocolPayload;
import ro.sensor_networks.ro.sensor_networks.utils.Utils;
import ro.sensor_networks.xbee.XBeeNetworkNode;
import ro.sensor_networks.xbee.protocols.IncompleteMessageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 */
public class XBeeSeries2InputStream extends InputStream {
    private static Logger logger = LoggerFactory.getLogger(XBeeSeries2InputStream.class);

    public static final int DEFAULT_TIMEOUT = 500;
    public static final int DEFAULT_MAX_PAYLOAD = 72;

    private XBee xbeeDevice;
    private XBeeNetworkNode destinationNode;
    private NetworkCommunicationProtocol communicationProtocol;

    private int timeOutValue;
    private int maxPayload;

    private ArrayList<ProtocolPayload> protocolPacketList;
    private ByteArrayInputStream messageStream;


    public XBeeSeries2InputStream(XBee xbee, XBeeNetworkNode destinationNetworkNode, NetworkCommunicationProtocol communicationProtocol) {
       this.xbeeDevice = xbee;
       this.destinationNode = destinationNetworkNode;
       this.communicationProtocol = communicationProtocol;

       protocolPacketList = new ArrayList<ProtocolPayload>();
       messageStream = new ByteArrayInputStream(new byte[0]);
       addListeners();
    }

    /**
     * Adds the xbee packet listener for incoming packets
     */
    private void addListeners() {
        xbeeDevice.addPacketListener(new PacketListener() {
            @Override
            public void processResponse(XBeeResponse response) {

                if (response.getApiId() == ApiId.ZNET_RX_RESPONSE){
                    int[] intPacket = response.getProcessedPacketBytes();

                    final byte[] bytePacket = Utils.intsToBytes(intPacket);

                    protocolPacketList.add(new ProtocolPayload() {
                        @Override
                        public byte[] getContent() {
                            return bytePacket;
                        }
                    });

                    if(communicationProtocol.isComplete(protocolPacketList)){
                        try{
                            messageStream = new ByteArrayInputStream(communicationProtocol.getMessage(protocolPacketList));
                        } catch (IncompleteMessageException e){
                            logger.debug("This should not be happening!");
                        }
                    }

                }
                else{
                    logger.debug("Ignoring received packet with id:{}", response.getApiId().toString());
                }
            }
        });
    }

    @Override
    public int read() throws IOException {
        return messageStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return messageStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return messageStream.read(b,off,len);
    }

    @Override
    public long skip(long n) throws IOException {
        return messageStream.skip(n);
    }

    @Override
    public int available(){
        return messageStream.available();
    }

    @Override
    public synchronized void mark(int readLimit) {
        messageStream.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return messageStream.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        messageStream.reset();
    }
}
