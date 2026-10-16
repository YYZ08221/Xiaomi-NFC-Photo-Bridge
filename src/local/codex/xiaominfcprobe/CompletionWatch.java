package local.codex.xiaominfcprobe;
/** Accept one success from the same native chooser window after our device click. */
public final class CompletionWatch {
    private boolean active;private int window;private long deadline;
    public void start(long now,int window){this.window=window;deadline=now+120000;active=true;}
    public boolean active(long now){if(now>=deadline)active=false;return active;}
    public void clear(){active=false;}
    public boolean observe(long now,String pkg,int window,CharSequence status){
        if(!active(now)||this.window!=window||!"com.android.intentresolver".equals(pkg)||status==null)return false;
        String value=status.toString().trim();
        if(!"已完成".equals(value)&&!"发送成功".equals(value)&&!"传输完成".equals(value)&&!"Sent successfully".equals(value))return false;
        active=false;return true;
    }
}
