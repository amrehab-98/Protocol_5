import java.io.Serializable;

public class Frame implements Serializable {
    public Frame(frame_kind kind, int seq, int ack, Packet info) {
        this.kind = kind;
        this.seq = seq;
        this.ack = ack;
        this.info = info;
    }

    enum frame_kind {
        data,
        ack,
        nak
    }
    private frame_kind kind;
    private int seq;
    private int ack;

    public frame_kind getKind() {
        return kind;
    }

    public void setKind(frame_kind kind) {
        this.kind = kind;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public Packet getInfo() {
        return info;
    }

    public void setInfo(Packet info) {
        this.info = info;
    }

    private Packet info;
}
