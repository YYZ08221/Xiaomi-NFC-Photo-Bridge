package local.codex.xiaominfcprobe;
public final class CompletionWatchTest {
    public static void main(String[] args){
        CompletionWatch w=new CompletionWatch();
        check(false,w.observe(0,"com.android.intentresolver",4,"发送成功"));
        w.start(10,4);
        check(false,w.observe(11,"other",4,"发送成功"));
        check(false,w.observe(12,"com.android.intentresolver",5,"发送成功"));
        check(false,w.observe(13,"com.android.intentresolver",4,"正在发送"));
        check(true,w.observe(14,"com.android.intentresolver",4,"发送成功"));
        check(false,w.observe(15,"com.android.intentresolver",4,"发送成功"));
        w.start(20,4);
        check(false,w.observe(120020,"com.android.intentresolver",4,"发送成功"));
        w.start(30,4);
        w.clear();
        check(false,w.observe(31,"com.android.intentresolver",4,"发送成功"));
        w.start(40,4);
        check(true,w.observe(41,"com.android.intentresolver",4,"已完成"));
        System.out.println("CompletionWatchTest passed");
    }
    private static void check(boolean a,boolean b){if(a!=b)throw new AssertionError("completion authorization");}
}
