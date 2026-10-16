package local.codex.xiaominfcprobe;
import android.content.*;import android.net.Uri;import android.os.*;import android.util.Log;
import local.codex.xiaominfcprobe.wire.*;import java.io.IOException;import java.util.*;
public final class NormalImageSender {
    public interface Result{void submitted();void failed(String message);}
    private static final Set<NormalImageSender> ACTIVE=new HashSet<>();
    private final Context context;private final ArrayList<Uri> uris;private final Result result;
    private final Handler main=new Handler(Looper.getMainLooper());private IBinder remote;private boolean bound,sent,closed;
    private final Binder callback=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
        String desc="com.miui.mishare.IMiShareDiscoverCallback";
        if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(desc);return true;}data.enforceInterface(desc);
        if(code==1){RemoteDevice d=data.readInt()!=0?RemoteDevice.CREATOR.createFromParcel(data):null;if(d!=null&&"CONFIGURE_PC_DEVICE_ID".equals(d.id))main.post(()->send(d));return true;}
        if(code==2){data.readString();return true;}return false;
    }};
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){remote=binder;try{tx(4,null,null);tx(6,callback,null);}catch(Exception e){fail(e);}}
        public void onServiceDisconnected(ComponentName n){remote=null;}
    };
    public NormalImageSender(Context c,ArrayList<Uri> u,Result r){context=c.getApplicationContext();uris=u;result=r;}
    public void start(){
        ACTIVE.add(this);
        try{Intent i=new Intent().setComponent(new ComponentName("com.miui.mishare.connectivity","com.miui.mishare.connectivity.MiShareService"));
            bound=context.bindService(i,connection,Context.BIND_AUTO_CREATE);if(!bound)throw new IOException("MiShare bind rejected");
            main.postDelayed(()->{if(!sent)fail(new IOException("指定电脑未发现"));},25000);
        }catch(Exception e){fail(e);}
    }
    private void tx(int code,IBinder cb,MiShareTask task)throws Exception{
        Parcel p=Parcel.obtain();try{p.writeInterfaceToken("com.miui.mishare.IMiShareService");if(cb!=null)p.writeStrongBinder(cb);if(task!=null){p.writeInt(1);task.writeToParcel(p,0);}if(!remote.transact(code,p,null,IBinder.FLAG_ONEWAY))throw new IOException("Unknown MiShare transaction");}finally{p.recycle();}
    }
    private void send(RemoteDevice device){
        if(sent||closed)return;sent=true;
        try{
            for(Uri uri:uris)context.grantUriPermission("com.miui.mishare.connectivity",uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ClipData clip=new ClipData("Selected images",new String[]{"image/*"},new ClipData.Item(uris.get(0)));
            for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));
            MiShareTask t=new MiShareTask();t.taskId=UUID.randomUUID().toString();t.device=device;t.count=uris.size();t.clipData=clip;t.mimeType="image/*";
            tx(9,null,t);Log.i("XiaomiNfcBridge","SELECTED_SEND_SUBMITTED count="+uris.size()+" taskId="+t.taskId);
            result.submitted();main.postDelayed(this::release,120000);
        }catch(Exception e){fail(e);}
    }
    private void fail(Exception e){Log.e("XiaomiNfcBridge","NORMAL_SEND_FAILED",e);result.failed(e.getMessage());release();}
    public void cancel(){closed=true;release();}
    private void release(){closed=true;if(remote!=null){try{tx(8,callback,null);}catch(Exception ignored){}}
        if(bound){try{context.unbindService(connection);}catch(Exception ignored){}bound=false;}remote=null;ACTIVE.remove(this);
    }
}
