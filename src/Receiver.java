import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Receiver {
    private static final int MAX_SEQ = 7;
    private boolean networkLayer = false;

    public Receiver() {
    }

    enum event_type {frame_arrival, cksum_err, timeout, network_layer_ready}

    private event_type event;

    public void to_physical_layer(Frame s, ObjectOutputStream dos) throws IOException {
        dos.writeObject(s);
    }

    public void send_data(int frame_nr, ObjectOutputStream dos) throws IOException {
        /*Construct and send a data frame. */
        String data = "Ack." + frame_nr;
        Packet p = new Packet(data.toCharArray());
        Frame s = new Frame(Frame.frame_kind.ack, frame_nr, frame_nr, p); /* scratch variable */
        to_physical_layer(s, dos);  /*transmit the frame*/
    }

    public int inc(int k) {
        return (k + 1) % MAX_SEQ;
    }

    public void toggle_network_layer() {
        networkLayer = !networkLayer;
    }

    public void disable_network_layer() {
        networkLayer = false;
    }

    public Frame from_physical_layer(ObjectInputStream dis) throws IOException, ClassNotFoundException {
        Frame r = (Frame) dis.readObject();
        String s = new String(r.getInfo().getData());
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

    public void protocol5() throws IOException, ClassNotFoundException {
        int next_frame_to_send; /* MAX SEQ > 1; used for outbound stream */
        int frame_expected; /* next frame expected on inbound stream */
        Frame r; /* scratch variable */
        ArrayList<Packet> buffer = new ArrayList<Packet>(MAX_SEQ + 1); /* buffers for the outbound stream */
        event_type event;
        disable_network_layer(); /* allow network layer ready events */
        next_frame_to_send = 0; /* next frame going out */
        frame_expected = 0; /* number of frame expected inbound */
        ServerSocket server = new ServerSocket(1234);
        Socket receive = server.accept();
        System.out.println("connected to sender");
        ObjectOutputStream dos = new ObjectOutputStream(receive.getOutputStream());
        ObjectInputStream dis = new ObjectInputStream(receive.getInputStream());


        while (true) {
            event = wait_for_event(); /* four possibilities: see event type above */
            switch (event) {
                case network_layer_ready: /* the network layer has a packet to send */
                    send_data(next_frame_to_send, dos);
                    System.out.println("Ack." + next_frame_to_send + " sent");
                    next_frame_to_send = inc(next_frame_to_send);
                    disable_network_layer();
                    break;
                case frame_arrival: /* a data or control frame has arrived */
                    r = from_physical_layer(dis); /* get incoming frame from physical layer */
                    if (r.getSeq() == frame_expected) {
                        String data = new String(r.getInfo().getData());
                        System.out.println("Correct (seq_num: " + r.getSeq() + ", data: " + data + ")");
                        toggle_network_layer();
                        /*Frames are accepted only in order. */
                        to_network_layer(r.getInfo()); /* pass packet to network layer */
                        frame_expected = inc(frame_expected); /* advance lower edge of receiver’s window */
                    } else {
                        System.out.println("Incorrect frame (will be discarded)");
                        send_data(5000, dos);
                    }
                    break;
                case cksum_err:
                    break; /* just ignore bad frames */
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Receiver receiver = new Receiver();
        receiver.protocol5();
    }
}
