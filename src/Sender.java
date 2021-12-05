import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Sender {

    int timer;
    private static final int MAX_SEQ = 7;
    private static final int TIME_OUT_TIME = 4;
    private boolean networkLayer = false;
    private int timeoutFlag = 0;
    private int[] timer_buffer = {0, 0, 0, 0, 0, 0, 0, 0};
    private int[] buffer_flag = {0, 0, 0, 0, 0, 0, 0, 0};

    public Sender() throws IOException {
    }

    enum event_type {frame_arrival, cksum_err, timeout, network_layer_ready}

    private event_type event;

    public boolean between(int a, int b, int c) {
        /* Return true if a <= b < c circularly; false otherwise. */
        if (((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a)))
            return (true);
        else
            return (false);
    }

    static int i = 0;

    public void start_timer(int seq_nr) {
        System.out.println("timer "+seq_nr+ " started");
        timer_buffer[seq_nr] = i;
        buffer_flag[seq_nr] = 1;
        i++;
        for (int j = 0; j < 8; j++) {
            if (buffer_flag[j] == 1) {
                if (i - timer_buffer[j] > TIME_OUT_TIME) {
                    timeoutFlag = 1;
                    event = event_type.timeout;
                }
            }
        }
    }

    public void to_physical_layer(Frame s, ObjectOutputStream dos) throws IOException {
        dos.writeObject(s);
    }

    public void send_data(int frame_nr, int frame_expected, ArrayList<Packet> buffer, ObjectOutputStream dos) throws IOException {
        /*Construct and send a data frame. */
        Frame s = new Frame(Frame.frame_kind.data, frame_nr, (frame_expected + MAX_SEQ) % (MAX_SEQ + 1), buffer.get(frame_nr));
        //user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("Do you want to send a valid frame? <y/n>");
        String input = scanner.nextLine();
        if(input.equalsIgnoreCase("N") || input.equalsIgnoreCase("NO")){
            s.setSeq(5000);
        }

        /* scratch variable */
        to_physical_layer(s, dos);  /*transmit the frame*/
        //FrameTimer sendTimer = new FrameTimer(frame_nr);
        start_timer(frame_nr);  /*start the timer running*/
        //return sendTimer;
    }

    public int inc(int k) {
        return (k+1)%MAX_SEQ;
    }

    public void enable_network_layer() {
        networkLayer = !networkLayer;
    }

    public void disable_network_layer() {
        networkLayer = false;
    }

    public Packet from_network_layer(int next_frame_to_send) {
        ArrayList<Packet> data = new ArrayList<Packet>(7);
        data.add(new Packet("packet0".toCharArray()));
        data.add(new Packet("packet1".toCharArray()));
        data.add(new Packet("packet2".toCharArray()));
        data.add(new Packet("packet3".toCharArray()));
        data.add(new Packet("packet4".toCharArray()));
        data.add(new Packet("packet5".toCharArray()));
        data.add(new Packet("packet6".toCharArray()));
        data.add(new Packet("packet7".toCharArray()));
//        data.set(7, new Packet("packet7".toCharArray()));

        return data.get(next_frame_to_send);
    }
//    ServerSocket server = new ServerSocket(1235);
    public Frame from_physical_layer(ObjectInputStream dis) throws IOException, ClassNotFoundException {
        Frame r = (Frame) dis.readObject();
        return r;
    }

    public void to_network_layer(Packet info) {
        //TODO
    }

    public event_type wait_for_event() {
        if (networkLayer && timeoutFlag !=1) {
            event = event_type.network_layer_ready;
        } else if (timeoutFlag == 1) {
            event = event_type.timeout;
        } else {
            event = event_type.frame_arrival;
        }
        return event;
    }

    public void stop_timer(int ack_expected) {
        System.out.println("timer "+ack_expected+" stopped");
        buffer_flag[ack_expected] = 0;
        //TODO
    }
    public static ArrayList<Packet> createPrefilledList(int size, Packet item) {
        ArrayList<Packet> list = new ArrayList<Packet>(size);
        for (int i = 0; i < size; i++) {
            list.add(item);
        }
        return list;
    }

    public void protocol5() throws IOException, ClassNotFoundException {
        int next_frame_to_send; /* MAX SEQ > 1; used for outbound stream */
        int ack_expected; /* oldest frame as yet unacknowledged */
        int frame_expected; /* next frame expected on inbound stream */
        Frame r; /* scratch variable */
//        ArrayList<Packet> buffer = new ArrayList<Packet>(MAX_SEQ + 1);
        ArrayList<Packet> buffer = createPrefilledList(MAX_SEQ+1, new Packet());

        int nBuffered; /* number of output buffers currently in use */
        int i; /* used to index into the buffer array */
        event_type event;
        enable_network_layer(); /* allow network layer ready events */
        ack_expected = 0; /* next ack expected inbound */
        next_frame_to_send = 0; /* next frame going out */
        frame_expected = 0; /* number of frame expected inbound */
        nBuffered = 0; /* initially no packets are buffered */
        Socket receiver = new Socket("localhost", 1234);
        ObjectOutputStream dos = new ObjectOutputStream(receiver.getOutputStream());
        ObjectInputStream dis = new ObjectInputStream(receiver.getInputStream());

        while (true) {
            event = wait_for_event(); /* four possibilities: see event type above */
            switch (event) {
                case network_layer_ready: /* the network layer has a packet to send */
                    /*Accept, save, and transmit a new frame. */
                    buffer.set(next_frame_to_send, from_network_layer(next_frame_to_send)); /* fetch new packet */
                    nBuffered = nBuffered + 1; /* expand the sender’s window */
                    send_data(next_frame_to_send, frame_expected, buffer, dos);/* transmit the frame */
                    System.out.println("next frame to send: "+ next_frame_to_send);
                    next_frame_to_send = inc(next_frame_to_send); /* advance sender’s upper window edge */
                    break;
                case frame_arrival: /* a data or control frame has arrived */
                    System.out.println("frame arrival");
                    r = from_physical_layer(dis); /* get incoming frame from physical layer */
                    if (r.getAck() == ack_expected) {
                        String s = new String(r.getInfo().getData());
                        System.out.println(s);
                        /*Frames are accepted only in order. */
                        to_network_layer(r.getInfo()); /* pass packet to network layer */
                        stop_timer(ack_expected);
                        ack_expected = inc(ack_expected); /* advance lower edge of receiver’s window */
                    }
                    break;
                case cksum_err:
                    break; /* just ignore bad frames */
                case timeout: /* trouble; retransmit all outstanding frames */
                    System.out.println("timeout");

//                    r = from_physical_layer(dis); /* get incoming frame from physical layer */
//                    while (r.getSeq() == ack_expected && ack_expected<=next_frame_to_send) {
//                        String s = new String(r.getInfo().getData());
//                        System.out.println(s);
//                        /*Frames are accepted only in order. */
//                        to_network_layer(r.getInfo()); /* pass packet to network layer */
//                        stop_timer(ack_expected);
//                        nBuffered--;
//                        ack_expected = inc(ack_expected); /* advance lower edge of receiver’s window */
//                        if(ack_expected == next_frame_to_send){
//
//                        }
//                        else{
//                            r = from_physical_layer(dis);
//                        }
//                    }
//                    System.out.println("ack expected = "+ ack_expected);
                    next_frame_to_send = ack_expected; /* start retransmitting here */
                    for (i = ack_expected; i < nBuffered; i++) {
                    send_data(next_frame_to_send, frame_expected, buffer, dos);/* resend frame */
                    next_frame_to_send = inc(next_frame_to_send); /* prepare to send the next one */
                    }
                    enable_network_layer();
                    timeoutFlag = 0;
            }
//            if (nBuffered < MAX_SEQ) {
                enable_network_layer();
//            } else {
//                disable_network_layer();
//            }

        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Sender sender = new Sender();
        sender.protocol5();

    }

}
