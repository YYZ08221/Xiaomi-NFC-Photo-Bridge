package local.codex.xiaominfcprobe;
public final class PollDeadline {
    private long next;
    public boolean request(long due){if(next!=0&&next<=due)return false;next=due;return true;}
    public void fired(){next=0;}
}
