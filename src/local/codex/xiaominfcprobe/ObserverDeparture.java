package local.codex.xiaominfcprobe;
/** Release only the UI observer after leaving its window; never reports transfer success. */
public final class ObserverDeparture {
    private long since=-1;
    public void clear(){since=-1;}
    public boolean release(long now,boolean otherStableWindow){
        if(!otherStableWindow){clear();return false;}
        if(since<0)since=now;
        return now-since>=500;
    }
}
