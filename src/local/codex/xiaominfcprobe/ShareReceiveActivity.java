package local.codex.xiaominfcprobe;
import android.app.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.*;
import android.provider.OpenableColumns;import android.util.Log;import android.widget.*;import java.io.*;import java.security.*;import java.util.*;
public final class ShareReceiveActivity extends Activity {
    private TextView message;private volatile boolean cancelled;private boolean delegated;private NormalImageSender sender;
    public void onCreate(Bundle state){
        super.onCreate(state);message=new TextView(this);message.setPadding(32,70,32,32);message.setText("正在准备选中的图片…");setContentView(message);
        if(!GalleryBridgeService.consumeHandoff()){message.setText("请先从相册碰贴纸启动桥接。");Log.w("XiaomiNfcBridge","UNARMED_SHARE_REJECTED");return;}
        Intent i=getIntent();String action=i.getAction();LinkedHashSet<Uri> selected=new LinkedHashSet<>();
        if(Intent.ACTION_SEND.equals(action)){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u!=null)selected.add(u);}
        else if(Intent.ACTION_SEND_MULTIPLE.equals(action)){ArrayList<Uri> list=i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(list!=null)selected.addAll(list);}
        if(selected.isEmpty()&&i.getClipData()!=null)for(int n=0;n<i.getClipData().getItemCount();n++){Uri u=i.getClipData().getItemAt(n).getUri();if(u!=null)selected.add(u);}
        if(selected.isEmpty()){message.setText("没有收到可发送的图片，已停止。");return;}
        Log.i("XiaomiNfcBridge","SHARE_URIS_RECEIVED count="+selected.size());
        new Thread(()->copyAndSend(new ArrayList<>(selected)),"bridge-copy").start();
    }
    private void copyAndSend(ArrayList<Uri> selected){
        try{
            String session=UUID.randomUUID().toString();ArrayList<Uri> copies=new ArrayList<>();int index=0;
            for(Uri source:selected){
                if(!"content".equals(source.getScheme()))throw new IOException("图片来源不是授权的内容 URI");
                String name="image-"+index+".jpg";
                try(Cursor c=getContentResolver().query(source,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst()&&!c.isNull(0))name=c.getString(0);}
                name=name.replace('/','_').replace('\\','_');if(name.isEmpty()||name.equals(".")||name.equals(".."))throw new IOException("无效文件名");
                File dir=new File(getFilesDir(),"shared/"+session+"/"+index);if(!dir.mkdirs())throw new IOException("无法建立图片缓存");
                File file=new File(dir,name);MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;
                try(InputStream in=getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(file)){
                    if(in==null)throw new IOException("无法读取选中图片");byte[] buf=new byte[131072];int n;while((n=in.read(buf))!=-1){if(cancelled)throw new IOException("已取消");out.write(buf,0,n);digest.update(buf,0,n);bytes+=n;}
                }
                StringBuilder hex=new StringBuilder();for(byte v:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
                Log.i("XiaomiNfcBridge","SOURCE_HASH index="+index+" bytes="+bytes+" sha256="+hex);
                Uri uri=new Uri.Builder().scheme("content").authority("local.codex.xiaominfcprobe.shared").appendPath(session).appendPath(Integer.toString(index)).appendPath(name).build();copies.add(uri);index++;
            }
            runOnUiThread(()->{if(cancelled)return;sender=new NormalImageSender(this,copies,new NormalImageSender.Result(){
                public void submitted(){delegated=true;Toast.makeText(ShareReceiveActivity.this,"已交给小米互传发送",Toast.LENGTH_SHORT).show();finish();}
                public void failed(String error){message.setText("发送未完成："+error);}
            });sender.start();});
        }catch(Exception e){Log.e("XiaomiNfcBridge","SHARED_IMAGE_PREPARE_FAILED",e);runOnUiThread(()->message.setText("图片准备失败，未发送。"));}
    }
    protected void onDestroy(){if(!delegated){cancelled=true;if(sender!=null)sender.cancel();}super.onDestroy();}
}
