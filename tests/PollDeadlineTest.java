package local.codex.xiaominfcprobe;
public final class PollDeadlineTest {
    public static void main(String[] args){
        PollDeadline timer=new PollDeadline();
        if(!timer.request(100))throw new AssertionError();
        for(long due=110;due<1000;due+=10)if(timer.request(due))throw new AssertionError("content events postponed polling");
        if(!timer.request(90))throw new AssertionError("earlier event should accelerate");
        timer.fired();
        if(!timer.request(200))throw new AssertionError("schedule next poll");
        System.out.println("PollDeadlineTest passed");
    }
}
