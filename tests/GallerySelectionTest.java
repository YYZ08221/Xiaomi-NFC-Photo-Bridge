package local.codex.xiaominfcprobe;
public final class GallerySelectionTest {
    public static void main(String[] args) {
        check(true, "com.miui.gallery", "已选择1项");
        check(true, "com.miui.gallery", "已选择 12 项");
        check(true, "com.miui.gallery", "已选中2张");
        check(false, "com.miui.gallery", "已选择0项");
        check(false, "com.miui.gallery", "分享");
        check(false, "com.miui.gallery", "照片里写着已选择1项");
        check(false, "com.other.app", "已选择1项");
        check(false, "com.miui.gallery", null);
        System.out.println("GallerySelectionTest passed");
    }
    private static void check(boolean expected,String pkg,String label) {
        if(GallerySelection.matches(pkg,label)!=expected)throw new AssertionError("selection routing: "+label);
    }
}
