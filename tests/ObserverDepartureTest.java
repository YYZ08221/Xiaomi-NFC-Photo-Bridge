package local.codex.xiaominfcprobe;
public final class ObserverDepartureTest {
    public static void main(String[] args){
        ObserverDeparture d=new ObserverDeparture();
        if(d.release(0,true)||d.release(499,true))throw new AssertionError("transient departure");
        if(!d.release(500,true))throw new AssertionError("stable departure leaves stale busy state");
        d.clear();d.release(1000,true);d.release(1300,false);
        if(d.release(1600,true)||d.release(2000,true))throw new AssertionError("returned chooser must reset debounce");
        if(!d.release(2100,true))throw new AssertionError("new stable departure");
        System.out.println("ObserverDepartureTest passed");
    }
}
