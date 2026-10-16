package local.codex.xiaominfcprobe;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.widget.*;
import local.codex.xiaominfcprobe.wire.*;
import java.io.*;
import java.util.UUID;
public final class ProbeActivity extends Activity {
    private static final String TAG="XiaomiNfcProbe",SVC="com.miui.mishare.IMiShareService";
    private static final String CB="com.miui.mishare.IMiShareDiscoverCallback";
    private static final String TARGET="CONFIGURE_PC_DEVICE_ID";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView status;private IBinder remote;private boolean bound,sent,discovering;
    private void note(String s){Log.i(TAG,s);handler.post(()->status.append(s+"\n"));}
    private final Binder callback=new Binder(){
        protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException {
            if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(CB);return true;}
            data.enforceInterface(CB);
            if(code==1){
                RemoteDevice d=data.readInt()!=0?RemoteDevice.CREATOR.createFromParcel(data):null;
                if(d!=null){note("DISCOVERED targetMatch="+TARGET.equals(d.id));
                    if(TARGET.equals(d.id))handler.post(()->sendProbe(d));}
                return true;
            }
            if(code==2){data.readString();return true;}return false;
        }
    };
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,IBinder b){remote=b;note("BOUND descriptor="+safeDescriptor(b));discover();}
        public void onServiceDisconnected(ComponentName n){remote=null;note("DISCONNECTED");}
    };
    private String safeDescriptor(IBinder b){try{return b.getInterfaceDescriptor();}catch(Exception e){return e.toString();}}
    public void onCreate(Bundle state){
        super.onCreate(state);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(32,48,32,32);
        TextView title=new TextView(this);title.setText("一碰传 · 原生发送测试");title.setTextSize(22);box.addView(title);
        TextView info=new TextView(this);info.setText("相册勾选图片 → 碰 NFC → 发送原图。\n没有勾选图片 → 碰 NFC → 截取当前屏幕并发送。\n发送成功后自动关闭分享面板，回到原页面，并删除本次临时截图。电脑收到的文件和相册原图保留。失败或超时不删除截图。\n升级后请关闭再开启本工具的无障碍服务。需要允许后台弹出界面。\n");box.addView(info);
        Button access=new Button(this);access.setText("打开无障碍设置");access.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));box.addView(access);
        Button bridge=new Button(this);bridge.setText("测试：选图发原图，否则截屏发送");bridge.setOnClickListener(v->{if(GalleryBridgeService.arm())finish();else Toast.makeText(this,"请启用无障碍服务，或等待上次任务结束",Toast.LENGTH_LONG).show();});box.addView(bridge);
        Button writer=new Button(this);writer.setText("写入桥接 NFC 贴纸");writer.setOnClickListener(v->startActivity(new Intent(this,BridgeTagWriterActivity.class)));box.addView(writer);
        Button test=new Button(this);test.setText("旧版诊断：发送生成的测试图");box.addView(test);
        status=new TextView(this);ScrollView scroll=new ScrollView(this);scroll.addView(status);box.addView(scroll);setContentView(box);
        test.setOnClickListener(v->startProbe());
        if("local.codex.xiaominfcprobe.PROBE".equals(getIntent().getAction()))handler.post(this::startProbe);
    }
    private void startProbe(){
        if(discovering){note("Already running");return;}
        try{selfTest();}catch(Exception e){note("SELF_TEST_FAILED "+e);return;}
        sent=false;discovering=true;
        Intent i=new Intent().setComponent(new ComponentName("com.miui.mishare.connectivity","com.miui.mishare.connectivity.MiShareService"));
        try{bound=bindService(i,connection,Context.BIND_AUTO_CREATE);note("BIND_REQUEST accepted="+bound);}
        catch(Exception e){note("BIND_FAILED "+e);}
        handler.postDelayed(()->{if(!sent){note("NO_SEND: configured PC not discovered within 25 seconds");release();}},25000);
    }
    private void tx(int code,IBinder cb,MiShareTask task)throws Exception {
        Parcel p=Parcel.obtain();try{p.writeInterfaceToken(SVC);
            if(cb!=null)p.writeStrongBinder(cb);
            if(task!=null){p.writeInt(1);task.writeToParcel(p,0);}
            if(!remote.transact(code,p,null,IBinder.FLAG_ONEWAY))throw new IOException("Unknown transaction "+code);
        }finally{p.recycle();}
    }
    private void discover(){try{tx(4,null,null);tx(6,callback,null);note("DISCOVERY_STARTED");}catch(Exception e){note("DISCOVERY_FAILED "+e);release();}}
    private MiShareTask task(RemoteDevice device,Uri uri){
        MiShareTask t=new MiShareTask();t.taskId=UUID.randomUUID().toString();t.count=1;t.device=device;
        t.clipData=ClipData.newRawUri("Generated test image",uri);t.mimeType="image/png";t.tbWidth=640;t.tbHeight=360;return t;
    }
    private void selfTest(){
        Bundle b=new Bundle();b.putBoolean("is_pc",true);
        MiShareTask original=task(new RemoteDevice(TARGET,b),Uri.parse("content://local.codex.xiaominfcprobe.files/XiaomiBridge_probe_0.png"));
        Parcel p=Parcel.obtain();try{original.writeToParcel(p,0);p.setDataPosition(0);MiShareTask read=MiShareTask.CREATOR.createFromParcel(p);
            if(!read.send||read.count!=1||!TARGET.equals(read.device.id)||!read.device.extras.getBoolean("is_pc")||!original.clipData.getItemAt(0).getUri().equals(read.clipData.getItemAt(0).getUri())||p.dataAvail()!=0)throw new IllegalStateException("Parcel round trip mismatch");
        }finally{p.recycle();}note("PARCEL_ROUND_TRIP_OK");
    }
    private void sendProbe(RemoteDevice d){
        if(sent)return;sent=true;
        try{
            String name="XiaomiBridge_probe_"+System.currentTimeMillis()+".png";File f=new File(getFilesDir(),name);
            Bitmap image=Bitmap.createBitmap(640,360,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);c.drawColor(Color.rgb(238,244,250));
            Paint paint=new Paint(3);paint.setColor(Color.rgb(32,70,110));paint.setTextSize(30);c.drawText("Xiaomi Bridge - generated test",36,150,paint);paint.setTextSize(18);c.drawText(name,36,198,paint);
            try(FileOutputStream out=new FileOutputStream(f)){if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG encode failed");}image.recycle();
            Uri uri=Uri.parse("content://local.codex.xiaominfcprobe.files/"+name);
            grantUriPermission("com.miui.mishare.connectivity",uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            tx(9,null,task(d,uri));note("SEND_SUBMITTED file="+name+" bytes="+f.length());note("提交成功尚不代表传输完成，需核对电脑收到的文件与系统日志。");
            handler.postDelayed(this::release,20000);
        }catch(Exception e){note("SEND_FAILED "+e);release();}
    }
    private void release(){
        if(remote!=null){try{tx(8,callback,null);}catch(Exception e){Log.w(TAG,"Stop discovery",e);}}
        if(bound){try{unbindService(connection);}catch(Exception ignored){}bound=false;}
        remote=null;discovering=false;
    }
    protected void onDestroy(){release();super.onDestroy();}
}
