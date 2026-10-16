package local.codex.xiaominfcprobe;
public final class NativeTargetTest {
    public static void main(String[] args) {
        String id="android.miui:id/tv_device_name";
        if (!NativeTarget.matches(id,"我的\n笔记本")) throw new AssertionError("two-line label");
        if (!NativeTarget.matches(id,"我的笔记本")) throw new AssertionError("single-line label");
        if (NativeTarget.matches("android:id/text1","我的笔记本")) throw new AssertionError("wrong row");
        if (NativeTarget.matches(id,"我的笔记本备份")) throw new AssertionError("different device");
        if (NativeTarget.matches(id,null)) throw new AssertionError("missing label");
        System.out.println("NativeTargetTest passed");
    }
}
