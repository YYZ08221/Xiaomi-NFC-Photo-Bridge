package local.codex.xiaominfcprobe;

/** Exact native device-row match; whitespace includes Xiaomi's two-line label. */
public final class NativeTarget {
    public static boolean matches(String id, CharSequence label) {
        if (!"android.miui:id/tv_device_name".equals(id) || label == null) return false;
        return "我的笔记本".equals(label.toString().replaceAll("[\\s\\u00a0\\u3000]+", ""));
    }
}
