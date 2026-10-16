package local.codex.xiaominfcprobe;
/** Small, monotonic-time authorization window for one explicit bridge invocation. */
public final class BridgeGate {
    public enum Stage {IDLE, GALLERY, TARGET, HANDOFF}
    private Stage stage=Stage.IDLE;private long deadline;
    public synchronized void arm(long now){stage=Stage.GALLERY;deadline=now+20000;}
    public synchronized Stage stage(long now){if(now>=deadline)stage=Stage.IDLE;return stage;}
    public synchronized boolean shareClicked(long now){if(stage(now)!=Stage.GALLERY)return false;stage=Stage.TARGET;return true;}
    public synchronized boolean targetClicked(long now){if(stage(now)!=Stage.TARGET)return false;stage=Stage.HANDOFF;deadline=now+5000;return true;}
    public synchronized boolean consume(long now){if(stage(now)!=Stage.HANDOFF)return false;stage=Stage.IDLE;return true;}
    public synchronized void clear(){stage=Stage.IDLE;}
}
