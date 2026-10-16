package local.codex.xiaominfcprobe;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.view.Display;
import java.io.*;

/** Capture only after an explicit invocation, before opening any share UI. */
public final class ScreenCapture {
    public interface Result { void ready(File file); void failed(String reason); }
    public static void capture(AccessibilityService service,Result result) {
        if(Build.VERSION.SDK_INT<30){result.failed("截图功能需要 Android 11 或以上");return;}
        service.takeScreenshot(Display.DEFAULT_DISPLAY,service.getMainExecutor(),new AccessibilityService.TakeScreenshotCallback(){
            public void onFailure(int code){result.failed("系统拒绝截图（错误码 "+code+"），已停止发送");}
            public void onSuccess(AccessibilityService.ScreenshotResult shot){
                HardwareBuffer buffer=shot.getHardwareBuffer();Bitmap hardware=null;Bitmap copy;
                try {
                    hardware=Bitmap.wrapHardwareBuffer(buffer,shot.getColorSpace());
                    if(hardware==null)throw new IOException("截图缓冲区为空");
                    copy=hardware.copy(Bitmap.Config.ARGB_8888,false);
                    if(copy==null)throw new IOException("截图转换失败");
                }catch(Exception e){result.failed("截图失败："+e.getMessage());return;}
                finally{if(hardware!=null)hardware.recycle();buffer.close();}
                new Thread(()->{
                    try {
                        File dir=new File(service.getFilesDir(),"shared");
                        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法保存截图");
                        File file=new File(dir,"NFC_Screenshot_"+System.currentTimeMillis()+".png");
                        try(FileOutputStream out=new FileOutputStream(file)){
                            if(!copy.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("截图编码失败");
                        }
                        service.getMainExecutor().execute(()->result.ready(file));
                    }catch(Exception e){service.getMainExecutor().execute(()->result.failed("截图保存失败："+e.getMessage()));}
                    finally{copy.recycle();}
                },"nfc-screenshot-save").start();
            }
        });
    }
}
