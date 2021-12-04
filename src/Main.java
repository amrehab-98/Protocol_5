import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("receiver created");
        Receiver receiver = new Receiver();
        System.out.println("sender created");
        Sender sender = new Sender();


        receiver.protocol5();
        sender.protocol5();

    }
}
