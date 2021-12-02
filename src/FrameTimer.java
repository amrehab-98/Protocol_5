public class FrameTimer {
    private int frame_number;
    private long start;
    public FrameTimer(int frame_number) {
        this.frame_number = frame_number;
        start = System.currentTimeMillis();
    }
    public long currentTimer() {
        return (System.currentTimeMillis() - start);
    }
}
