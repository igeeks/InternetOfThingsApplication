package ro.sensor_networks.xbee.streams;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.wpan.TxRequest16;
import com.rapplogic.xbee.api.wpan.TxStatusResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.sensor_networks.protocol.NetworkCommunicationProtocol;
import ro.sensor_networks.protocol.ProtocolPayload;
import ro.sensor_networks.ro.sensor_networks.utils.Utils;
import ro.sensor_networks.xbee.XBeeNetworkNode;
import ro.sensor_networks.xbee.protocols.SimpleDataTransferProtocol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * The Stream is used to send data using the xbee xbeeDevice
 */
public class XBeeSeries2OutputStream extends OutputStream {
    private static Logger logger = LoggerFactory.getLogger(XBeeSeries2OutputStream.class);

    public static final int DEFAULT_TIMEOUT = 500;
    public static final int DEFAULT_XBEE_PAYLOAD = 72;

    private XBee xbeeDevice;
    private XBeeNetworkNode destinationNode;
    private int timeOutValue;
    private int payloadSize;
    private NetworkCommunicationProtocol communicationProtocol;

    public XBeeSeries2OutputStream(XBee xbee, XBeeNetworkNode destinationNetworkNode) {
      this(xbee, DEFAULT_TIMEOUT, DEFAULT_XBEE_PAYLOAD,destinationNetworkNode, new SimpleDataTransferProtocol());
    }

    public XBeeSeries2OutputStream(XBee xbee, XBeeNetworkNode destinationNetworkNode, NetworkCommunicationProtocol communicationProtocol) {
        this(xbee, DEFAULT_TIMEOUT, DEFAULT_XBEE_PAYLOAD, destinationNetworkNode, communicationProtocol);
    }

    public XBeeSeries2OutputStream(XBee xbeeDevice, int timeOutValue, int payloadSize, XBeeNetworkNode destinationNode, NetworkCommunicationProtocol communicationProtocol) {
        if(xbeeDevice == null || destinationNode == null)
        {
            throw new NullPointerException("Null arguments passed in the constructor");
        }

        this.xbeeDevice = xbeeDevice;
        this.destinationNode = destinationNode;
        this.timeOutValue = timeOutValue;
        this.payloadSize = payloadSize;
        this.communicationProtocol = communicationProtocol;
    }

    @Override
    /**
     * Writes the specified byte to the output stream
     */
    public void write(int b) throws IOException {
       byte temp = (byte)b;
       write(new byte[]{temp});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b,0,b.length);
    }

    /**
     * Sends a packet to the destination node. If the packet size is larger than the maximum acceptable payload an IO exception is  thrown
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        // Obtaing the list of packets for the input data
        List<ProtocolPayload> messagePackets = communicationProtocol.getProtocolPackets(b, payloadSize);

        // Send the packet synchronously
        for(ProtocolPayload packet:messagePackets){
            int[] payload = Utils.bytesToInts(packet.getContent());

            try{
                ZNetTxRequest request = new ZNetTxRequest(XBeeRequest.DEFAULT_FRAME_ID, destinationNode.getNodeAddress64(),destinationNode.getNodeAddress16(), ZNetTxRequest.DEFAULT_BROADCAST_RADIUS, ZNetTxRequest.Option.UNICAST, new int[1]);

                request.setMaxPayloadSize(payloadSize);
                request.setPayload(payload);

                ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbeeDevice.sendSynchronous(request, timeOutValue);

                if(!response.isSuccess()){
                    throw new IOException("Could not send package to destination");
                }
            }

            catch(XBeeTimeoutException xte){
                logger.debug("XbeeTimeOutException during the packet sending phase:", xte);
                throw new IOException("XbeeTimeOutException during the packet sending phase");
            }
            catch(XBeeException gxe){
                logger.debug("XBeeException during the packet sending phase:", gxe);
                throw new IOException("XBeeException during the packet sending phase");
            }
        }

    }

}
