import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Receiver {
    private static final int MAX_SEQ = 7;
    private boolean networkLayer = false;

    enum event_type {frame_arrival, cksum_err, timeout, network_layer_ready}

    private event_type event;

    public boolean between(int a, int b, int c) {
        /* Return true if a <= b < c circularly; false otherwise. */
        if (((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a)))
            return (true);
        else
            return (false);
    }


    public void to_physical_layer(Frame s) throws IOException {
        Socket send = new Socket("127.0.0.1", 1234);
        ObjectOutputStream dos = new ObjectOutputStream(send.getOutputStream());
        dos.writeObject(s);
    }

    public void send_data(int frame_nr, int frame_expected, ArrayList<Packet> buffer) throws IOException {
        /*Construct and send a data frame. */
        Frame s = new Frame(Frame.frame_kind.ack, frame_nr, (frame_expected + MAX_SEQ) % (MAX_SEQ + 1), buffer.get(frame_nr)); /* scratch variable */
        to_physical_layer(s);  /*transmit the frame*/
        //FrameTimer sendTimer = new FrameTimer(frame_nr);
        //return sendTimer;
    }

    public int inc(int k) {
        if (k < MAX_SEQ)
            k = k + 1;
        else
            k = 0;
        return k;
    }

    public void enable_network_layer() {
        networkLayer = true;
    }

    public void disable_network_layer() {
        networkLayer = false;
    }

    public Packet from_network_layer(int next_frame_to_send) {
        ArrayList<Packet> data = new ArrayList<Packet>(7);
        data.set(0, new Packet("packet0".toCharArray()));
        data.set(1, new Packet("packet1".toCharArray()));
        data.set(2, new Packet("packet2".toCharArray()));
        data.set(3, new Packet("packet3".toCharArray()));
        data.set(4, new Packet("packet4".toCharArray()));
        data.set(5, new Packet("packet5".toCharArray()));
        data.set(6, new Packet("packet6".toCharArray()));
        data.set(7, new Packet("packet7".toCharArray()));

        return data.get(next_frame_to_send);
    }

    public Frame from_physical_layer() throws IOException, ClassNotFoundException {
        ServerSocket server = new ServerSocket(1235);
        Socket receive = server.accept();
        ObjectInputStream dis = new ObjectInputStream(receive.getInputStream());
        Frame r = (Frame) dis.readObject();
        return r;
    }

    public void to_network_layer(Packet info) {
        //TODO
    }

    public event_type wait_for_event() {
        if (networkLayer) {
            event = event_type.network_layer_ready;
        } else {
            event = event_type.frame_arrival;
        }
        return event;
    }

    public void stop_timer(int ack_expected) {
        //TODO
    }


    public void protocol5() throws IOException, ClassNotFoundException {
        int next_frame_to_send; /* MAX SEQ > 1; used for outbound stream */
        int ack_expected; /* oldest frame as yet unacknowledged */
        int frame_expected; /* next frame expected on inbound stream */
        Frame r; /* scratch variable */
        ArrayList<Packet> buffer = new ArrayList<Packet>(MAX_SEQ + 1); /* buffers for the outbound stream */
        int nBuffered; /* number of output buffers currently in use */
        int i; /* used to index into the buffer array */
        event_type event;
        enable_network_layer(); /* allow network layer ready events */
        ack_expected = 0; /* next ack expected inbound */
        next_frame_to_send = 0; /* next frame going out */
        frame_expected = 0; /* number of frame expected inbound */
        nBuffered = 0; /* initially no packets are buffered */
        while (true) {
            event = wait_for_event(); /* four possibilities: see event type above */
            switch (event) {
                case network_layer_ready: /* the network layer has a packet to send */
                    /*Accept, save, and transmit a new frame. */
                    buffer.set(next_frame_to_send, from_network_layer(next_frame_to_send)); /* fetch new packet */
                    nBuffered = nBuffered + 1; /* expand the sender’s window */
                    send_data(next_frame_to_send, frame_expected, buffer);/* transmit the frame */
                    next_frame_to_send = inc(next_frame_to_send); /* advance sender’s upper window edge */
                    disable_network_layer();
                    break;
                case frame_arrival: /* a data or control frame has arrived */
                    r = from_physical_layer(); /* get incoming frame from physical layer */
                    if (r.getSeq() == frame_expected) {
                        enable_network_layer();
                        /*Frames are accepted only in order. */
                        to_network_layer(r.getInfo()); /* pass packet to network layer */
                        frame_expected = inc(frame_expected); /* advance lower edge of receiver’s window */
                    }
                    break;
                case cksum_err:
                    break; /* just ignore bad frames */
            }
            if (nBuffered < MAX_SEQ) {
                enable_network_layer();
            } else {
                disable_network_layer();
            }

        }
    }
}
