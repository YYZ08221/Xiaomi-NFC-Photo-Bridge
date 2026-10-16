package local.codex.xiaominfcprobe;
public final class BridgeGateTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        BridgeGate g=new BridgeGate();
        check(!g.consume(1));check(!g.targetClicked(1));
        g.arm(100);check(!g.consume(101));check(!g.targetClicked(102));
        check(g.shareClicked(103));check(!g.consume(104));check(g.targetClicked(105));
        check(g.consume(106));check(!g.consume(107));
        g.arm(1000);check(!g.shareClicked(21000));
        g.arm(30000);check(g.shareClicked(30001));check(g.targetClicked(30002));check(!g.consume(35002));
        g.arm(40000);g.clear();check(!g.shareClicked(40001));
        System.out.println("PASS: no unarmed handoff, no skipped stages, no replay, expiry and cancellation enforced");
    }
}
