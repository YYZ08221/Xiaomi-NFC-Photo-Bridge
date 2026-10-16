package local.codex.xiaominfcprobe;
public final class GallerySelection {
    public static boolean matches(String pkg,CharSequence label) {
        return "com.miui.gallery".equals(pkg) && label!=null
            && label.toString().trim().matches("^已(?:选择|选中|选)\\s*[1-9][0-9]*\\s*(?:项|张)$");
    }
}
