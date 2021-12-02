import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class Sender {

    int timer;
    private static final int MAX_SEQ = 7;
    private static final int TIME_OUT_TIME = 4;

    enum event_type {frame_arrival, cksum_err, timeout, network_layer_ready}

    event_type event;

    public static boolean between(int a, int b, int c) {
        /* Return true if a <= b < c circularly; false otherwise. */
        if (((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a)))
            return (true);
        else
            return (false);
    }


    public static void send_data(int frame_nr, int frame_expected, ArrayList<Packet> buffer) {
        /*Construct and send a data frame. */
        Frame s = new Frame(Frame.frame_kind.data, frame_nr, (frame_expected + MAX_SEQ) % (MAX_SEQ + 1), buffer.get(frame_nr)); /* scratch variable */
        /*to_physical_layer(&s); *//* transmit the frame *//*
        start_timer(frame_nr); *//* start the timer running */
    }

    public int inc(int k) {
        if (k < MAX_SEQ)
            k = k + 1;
        else
            k = 0;
        return k;
    }

    public void enable_network_layer() {
        //TODO
    }

    public void disable_network_layer() {

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
        Socket receive = new Socket("127.0.0.1", 1234);
        ObjectInputStream dis = new ObjectInputStream(receive.getInputStream());
        Frame r = (Frame) dis.readObject();
        return r;
    }

    public void to_network_layer(Packet info) {
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
        event = event_type.network_layer_ready;
        while (true) {
            //wait_for_event(&event); /* four possibilities: see event type above */
            switch (event) {
                case network_layer_ready: /* the network layer has a packet to send */
                    /*Accept, save, and transmit a new frame. */
                    buffer.set(next_frame_to_send, from_network_layer(next_frame_to_send)); /* fetch new packet */
                    nBuffered = nBuffered + 1; /* expand the sender’s window */
                    send_data(next_frame_to_send, frame_expected, buffer);/* transmit the frame */
                    next_frame_to_send = inc(next_frame_to_send); /* advance sender’s upper window edge */
                    break;
                case frame_arrival: /* a data or control frame has arrived */
                    r = from_physical_layer(); /* get incoming frame from physical layer */
                    if (r.getSeq() == frame_expected) {
                        /*Frames are accepted only in order. */
                        to_network_layer(r.getInfo()); /* pass packet to network layer */
                        frame_expected = inc(frame_expected); /* advance lower edge of receiver’s window */
                    }
                    /*Ack n implies n −1, n −2, etc.Check for this. */
                    while (between(ack_expected, r.getAck(), next_frame_to_send)) {
                        /*Handle piggybacked ack. */
                        nBuffered = nBuffered - 1; /* one frame fewer buffered */
                        //stop_timer (ack_expected); /* frame arrived intact; stop timer */
                        ack_expected = inc(ack_expected); /* contract sender’s window */
                    }
                    break;
                case cksum_err:
                    break; /* just ignore bad frames */
                case timeout: /* trouble; retransmit all outstanding frames */
                    next_frame_to_send = ack_expected; /* start retransmitting here */
                    for (i = 1; i <= nBuffered; i++) {
                        send_data(next_frame_to_send, frame_expected, buffer);/* resend frame */
                        next_frame_to_send = inc(next_frame_to_send); /* prepare to send the next one */
                    }
            }
            if (nBuffered < MAX_SEQ) {
                enable_network_layer();
            } else {
                disable_network_layer();
            }

        }
    }
}
