import java.util.ArrayList;

public class Packet {
    private static final int MAX_PKT = 1024;  /* determines packet size in bytes */
    char[] data;

    public Packet(char[] data) {
        this.data = data;
    }

    public void setData(char[] data) {
        this.data = data;
    }

    public char[] getData() {
        return data;
    }
}
