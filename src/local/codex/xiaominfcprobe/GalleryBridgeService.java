package local.codex.xiaominfcprobe;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import android.content.*;
import android.app.KeyguardManager;
import android.net.Uri;
import java.io.File;
import java.util.*;

public final class GalleryBridgeService extends AccessibilityService {
    private static GalleryBridgeService live;
    private final BridgeGate gate=new BridgeGate();private final Handler handler=new Handler(Looper.getMainLooper());
    private final CompletionWatch completion=new CompletionWatch();
    private final ObserverDeparture departure=new ObserverDeparture();
    private File screenshotFile;private boolean waitingForCompletion;
    private AccessibilityNodeInfo trackedDeviceRow;
    private final PollDeadline pollDeadline=new PollDeadline();
    private final Runnable tick=()->{pollDeadline.fired();step();};
    private void schedule(long delay){if(pollDeadline.request(now()+delay)){handler.removeCallbacks(tick);handler.postDelayed(tick,delay);}}
    private boolean stepping,capturing;private int invocation;private String lastDiagnostic="";
    private static long now(){return SystemClock.elapsedRealtime();}
    protected void onServiceConnected(){live=this;Log.i("XiaomiNfcBridge","ACCESSIBILITY_CONNECTED");}
    public static boolean enabled(){return live!=null;}
    public static boolean arm(){
        if(live==null)return false;
        if(live.waitingForCompletion&&!live.completion.active(now()))live.releaseObserver("deadline before new trigger");
        if(live.capturing||live.waitingForCompletion||live.gate.stage(now())!=BridgeGate.Stage.IDLE)return false;
        live.invocation++;
        live.screenshotFile=null;live.trackedDeviceRow=null;live.completion.clear();live.lastCompletionDiagnostic="";
        live.gate.arm(now());live.lastDiagnostic="";
        Log.i("XiaomiNfcBridge","BRIDGE_ARMED timeout=20s");live.schedule(700);return true;
    }
    public static boolean armChooserForDebug(){
        if(live==null)return false;
        AccessibilityNodeInfo root=live.getRootInActiveWindow();
        if(root==null||!"com.android.intentresolver".contentEquals(root.getPackageName()))return false;
        live.gate.arm(now());live.gate.shareClicked(now());live.lastDiagnostic="";
        Log.i("XiaomiNfcBridge","NATIVE_CHOOSER_ARMED");live.schedule(100);return true;
    }
    public static boolean consumeHandoff(){return live!=null&&live.gate.consume(now());}
    public void onAccessibilityEvent(AccessibilityEvent event){
        if(waitingForCompletion){
            AccessibilityNodeInfo source=event.getSource();
            if(source!=null)inspectCompletion(source);
        }
        if(waitingForCompletion||gate.stage(now())!=BridgeGate.Stage.IDLE)schedule(100);
    }
    public void onInterrupt(){Log.w("XiaomiNfcBridge","ACCESSIBILITY_INTERRUPTED");invocation++;capturing=false;waitingForCompletion=false;completion.clear();gate.clear();}
    public void onDestroy(){invocation++;completion.clear();gate.clear();handler.removeCallbacks(tick);if(live==this)live=null;super.onDestroy();}
    private void stop(String reason){invocation++;capturing=false;waitingForCompletion=false;completion.clear();gate.clear();Log.w("XiaomiNfcBridge",reason);Toast.makeText(this,reason,Toast.LENGTH_LONG).show();}
    private void releaseObserver(String reason){
        // This cancels UI observation only, not the system transfer. Unconfirmed files remain.
        invocation++;waitingForCompletion=false;trackedDeviceRow=null;completion.clear();departure.clear();gate.clear();
        handler.removeCallbacks(tick);pollDeadline.fired();
        Log.i("XiaomiNfcBridge","OLD_OBSERVER_RELEASED reason="+reason+" temporaryScreenshotPreserved="+(screenshotFile!=null));
        screenshotFile=null;
    }
    private static String str(CharSequence c){return c==null?"":c.toString().trim();}
    private static boolean shareLabel(AccessibilityNodeInfo n){
        String t=str(n.getText()),d=str(n.getContentDescription());
        return t.matches("^(分享|发送)([（(][0-9]+[）)])?$")||d.matches("^(分享|发送)([（(][0-9]+[）)])?$");
    }
    private static boolean targetLabel(AccessibilityNodeInfo n){return NativeTarget.matches(n.getViewIdResourceName(),n.getText());}
    private static AccessibilityNodeInfo clickable(AccessibilityNodeInfo n){
        for(int i=0;i<4&&n!=null;i++,n=n.getParent())if(n.isEnabled()&&n.isVisibleToUser()&&n.isClickable())return n;
        return null;
    }
    private AccessibilityNodeInfo deviceRow(AccessibilityNodeInfo n){
        for(int i=0;i<7&&n!=null;i++,n=n.getParent()){
            if("android.miui:id/ll_device".equals(n.getViewIdResourceName()))return n;
        }
        return null;
    }
    private String lastCompletionDiagnostic="";
    private void inspectCompletion(AccessibilityNodeInfo source){
        if(!waitingForCompletion)return;
        AccessibilityNodeInfo row=deviceRow(source);if(row==null)return;
        ArrayDeque<AccessibilityNodeInfo> q=new ArrayDeque<>();q.add(row);
        boolean ownDevice=trackedDeviceRow!=null&&trackedDeviceRow.equals(row);AccessibilityNodeInfo status=null;int count=0;
        while(!q.isEmpty()&&count++<100){
            AccessibilityNodeInfo n=q.remove();
            if(targetLabel(n))ownDevice=true;
            if("android.miui:id/tv_device_model_name".equals(n.getViewIdResourceName()))status=n;
            for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)q.add(child);}
        }
        if(!ownDevice||status==null||!status.isVisibleToUser())return;
        String value=str(status.getText());
        String diagnostic="COMPLETION_STATUS window="+status.getWindowId()+" package="+status.getPackageName()+" label="+value;
        if(!diagnostic.equals(lastCompletionDiagnostic)){Log.i("XiaomiNfcBridge",diagnostic);lastCompletionDiagnostic=diagnostic;}
        if(completion.observe(now(),str(status.getPackageName()),status.getWindowId(),value)
            ||completion.observe(now(),str(status.getPackageName()),status.getWindowId(),status.getContentDescription())){
            finishTransfer(status.getWindowId());
        }else if(value.contains("失败"))stop("发送失败，已保留截图供重试");
    }
    private void finishTransfer(int window){
        waitingForCompletion=false;trackedDeviceRow=null;gate.clear();handler.removeCallbacks(tick);pollDeadline.fired();
        Log.i("XiaomiNfcBridge","TRANSFER_SUCCESS_CONFIRMED");
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root!=null&&root.getWindowId()==window&&"com.android.intentresolver".contentEquals(root.getPackageName())){
            boolean back=performGlobalAction(GLOBAL_ACTION_BACK);
            Log.i("XiaomiNfcBridge","RETURN_TO_PREVIOUS requested="+back);
        }
        File file=screenshotFile;screenshotFile=null;
        if(file!=null){
            try{
                File base=new File(getFilesDir(),"shared").getCanonicalFile();
                File exact=file.getCanonicalFile();
                if(exact.getParentFile().equals(base)&&exact.getName().startsWith("NFC_Screenshot_")&&exact.getName().endsWith(".png")){
                    Uri uri=Uri.parse("content://local.codex.xiaominfcprobe.shared/"+exact.getName());
                    revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    boolean deleted=exact.delete();Log.i("XiaomiNfcBridge","TEMP_SCREENSHOT_DELETED success="+deleted);
                }
            }catch(Exception e){Log.e("XiaomiNfcBridge","SCREENSHOT_CLEANUP_FAILED",e);}
        }
    }
    private boolean hasSelection(AccessibilityNodeInfo root){
        if(!"com.miui.gallery".contentEquals(root.getPackageName()))return false;
        // Flutter exposes an empty [0,0][0,0] root on this Gallery version.
        // Derive the actual viewport from the nonempty descendants instead.
        android.graphics.Rect screen=new android.graphics.Rect();
        ArrayList<android.graphics.Rect> markers=new ArrayList<>();
        ArrayDeque<AccessibilityNodeInfo> q=new ArrayDeque<>();q.add(root);int count=0;
        while(!q.isEmpty()&&count++<3000){
            AccessibilityNodeInfo n=q.remove();android.graphics.Rect b=new android.graphics.Rect();n.getBoundsInScreen(b);
            if(n.isVisibleToUser()&&!b.isEmpty()){
                screen.union(b);
                if(GallerySelection.matches("com.miui.gallery",n.getText())||GallerySelection.matches("com.miui.gallery",n.getContentDescription()))markers.add(new android.graphics.Rect(b));
            }
            for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null)q.add(c);}
        }
        for(android.graphics.Rect b:markers)if(b.top>=screen.top&&b.bottom<=screen.top+screen.height()/4)return true;
        return false;
    }
    private void screenshot(){
        KeyguardManager lock=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(lock!=null&&lock.isKeyguardLocked()){stop("请解锁手机后再碰贴纸");return;}
        if(!gate.shareClicked(now()))return;
        capturing=true;final int token=invocation;
        Log.i("XiaomiNfcBridge","SCREENSHOT_REQUESTED");
        ScreenCapture.capture(this,new ScreenCapture.Result(){
            public void failed(String reason){if(token==invocation)stop(reason);}
            public void ready(File file){
                if(token!=invocation)return;capturing=false;
                screenshotFile=file;
                if(gate.stage(now())!=BridgeGate.Stage.TARGET){stop("截图操作超时，请重新碰贴纸");return;}
                try{
                    Uri uri=Uri.parse("content://local.codex.xiaominfcprobe.shared/"+file.getName());
                    Intent send=new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri);
                    send.setClipData(ClipData.newRawUri("Screenshot",uri));send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Intent chooser=Intent.createChooser(send,"发送截图到电脑");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    chooser.setClipData(send.getClipData());startActivity(chooser);
                    Log.i("XiaomiNfcBridge","SCREENSHOT_SHARE_REQUESTED bytes="+file.length());schedule(350);
                }catch(Exception e){Log.e("XiaomiNfcBridge","SCREENSHOT_SHARE_FAILED",e);stop("未能打开截图分享面板");}
            }
        });
    }
    private AccessibilityNodeInfo unique(AccessibilityNodeInfo root,boolean target){
        ArrayDeque<AccessibilityNodeInfo> q=new ArrayDeque<>();q.add(root);
        Map<String,AccessibilityNodeInfo> matches=new LinkedHashMap<>();int count=0;
        while(!q.isEmpty()&&count++<3000){AccessibilityNodeInfo n=q.remove();
            if(n.isVisibleToUser()&&(target?targetLabel(n):shareLabel(n))){
                AccessibilityNodeInfo c=clickable(n);if(c!=null){android.graphics.Rect bounds=new android.graphics.Rect();c.getBoundsInScreen(bounds);matches.put(bounds.toString(),c);}
            }
            for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null)q.add(c);}
        }
        String diagnostic="stage="+gate.stage(now())+" package="+root.getPackageName()+" scanned="+count+" matches="+matches.size();
        if(!diagnostic.equals(lastDiagnostic)){Log.i("XiaomiNfcBridge",diagnostic);lastDiagnostic=diagnostic;}
        if(matches.size()>1){stop("发现多个相同目标，已停止自动操作");return null;}
        return matches.size()==1?matches.values().iterator().next():null;
    }
    private void step(){
        if(waitingForCompletion){
            if(!completion.active(now())){stop("未确认发送完成，已保留截图");return;}
            AccessibilityNodeInfo root=getRootInActiveWindow();
            if(root!=null&&"com.android.intentresolver".contentEquals(root.getPackageName())){
                if(trackedDeviceRow!=null&&trackedDeviceRow.refresh())inspectCompletion(trackedDeviceRow);
                for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("android.miui:id/tv_device_name"))if(targetLabel(n))inspectCompletion(n);
            }
            if(waitingForCompletion&&root!=null){
                String pkg=str(root.getPackageName());
                boolean transientWindow=Arrays.asList("local.codex.xiaominfcprobe","com.android.nfc","com.android.systemui","com.android.permissioncontroller").contains(pkg);
                boolean left=!transientWindow&&!completion.sameWindow(root.getWindowId());
                if(departure.release(now(),left)){releaseObserver("left original share window");return;}
            }
            if(waitingForCompletion)schedule(150);return;
        }
        if(stepping||capturing)return;BridgeGate.Stage stage=gate.stage(now());if(stage==BridgeGate.Stage.IDLE)return;
        stepping=true;
        try{
            AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
            String pkg=str(root.getPackageName());
            if(stage==BridgeGate.Stage.GALLERY){
                // Wait for the NFC NoDisplay activity or launcher test activity to finish.
                if("local.codex.xiaominfcprobe".equals(pkg))return;
                if(!hasSelection(root)){screenshot();return;}
                Log.i("XiaomiNfcBridge","SELECTED_ORIGINALS_BRANCH");
                AccessibilityNodeInfo n=unique(root,false);
                if(n!=null&&gate.shareClicked(now())){
                    if(!n.performAction(AccessibilityNodeInfo.ACTION_CLICK)){stop("未能点击相册分享按钮，已停止");return;}
                    Log.i("XiaomiNfcBridge","SHARE_CLICKED");
                }
            }else if(stage==BridgeGate.Stage.TARGET&&("com.android.intentresolver".equals(pkg)||"android".equals(pkg))){
                AccessibilityNodeInfo n=unique(root,true);
                if(n!=null&&gate.targetClicked(now())){
                    // Clear before clicking: events must never click the device a second time.
                    gate.clear();
                    AccessibilityNodeInfo row=deviceRow(n);
                    trackedDeviceRow=row==null?null:AccessibilityNodeInfo.obtain(row);
                    waitingForCompletion=true;completion.start(now(),n.getWindowId());
                    departure.clear();
                    if(!n.performAction(AccessibilityNodeInfo.ACTION_CLICK)){stop("未能点击我的笔记本，已停止");return;}
                    Log.i("XiaomiNfcBridge","NATIVE_PC_CLICKED; completion must be confirmed on PC");
                }
            }
        }catch(Exception e){Log.e("XiaomiNfcBridge","AUTOMATION_FAILED",e);stop("自动分享未完成，已停止");}
        finally{stepping=false;
            if(waitingForCompletion)schedule(150);
            else if(!capturing&&gate.stage(now())!=BridgeGate.Stage.IDLE)schedule(350);
        }
    }
}
