package local.codex.xiaominfcprobe;
import android.app.Activity;import android.os.Bundle;import android.nfc.*;import android.nfc.tech.*;import android.net.Uri;import android.widget.*;
public final class BridgeTagWriterActivity extends Activity implements NfcAdapter.ReaderCallback {
    private TextView status;private NfcAdapter nfc;private volatile boolean armed;
    public void onCreate(Bundle state){
        super.onCreate(state);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(32,60,32,32);
        status=new TextView(this);status.setText("写入桥接 NFC 贴纸\n\n此操作会覆盖贴纸现有的 NDEF 内容。请使用你准备给电脑用的可写贴纸。\n\n按下开始后，将贴纸靠近手机 NFC 区域。");box.addView(status);
        Button start=new Button(this);start.setText("开始写入（覆盖贴纸内容）");start.setOnClickListener(v->{armed=true;status.setText("请将准备好的可写贴纸靠近手机，保持不动。");});box.addView(start);setContentView(box);nfc=NfcAdapter.getDefaultAdapter(this);
    }
    protected void onResume(){super.onResume();if(nfc!=null)nfc.enableReaderMode(this,this,NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V,null);else status.setText("此手机没有可用的 NFC 适配器。");}
    protected void onPause(){armed=false;if(nfc!=null)nfc.disableReaderMode(this);super.onPause();}
    public void onTagDiscovered(Tag tag){
        if(!armed)return;armed=false;
        NdefMessage message=new NdefMessage(new NdefRecord[]{
            NdefRecord.createUri(Uri.parse("xiaomi-photo-bridge://send/desk-1")),
            NdefRecord.createApplicationRecord(getPackageName())
        });
        try{
            Ndef tech=Ndef.get(tag);
            if(tech!=null){try{tech.connect();if(!tech.isWritable())throw new Exception("贴纸不可写");if(tech.getMaxSize()<message.toByteArray().length)throw new Exception("贴纸容量不足");tech.writeNdefMessage(message);NdefMessage readback=tech.getNdefMessage();if(readback==null||!java.util.Arrays.equals(message.toByteArray(),readback.toByteArray()))throw new Exception("写入后读回校验不一致，请重试");}finally{tech.close();}}
            else{NdefFormatable format=NdefFormatable.get(tag);if(format==null)throw new Exception("贴纸不支持 NDEF 写入");try{format.connect();format.format(message);}finally{format.close();}}
            runOnUiThread(()->status.setText("写入成功（网址 + Android 应用记录）。\n请退出此写入页面，返回相册选图，再碰贴纸。\n当前目标为系统分享中的“我的笔记本”。"));
        }catch(Exception e){runOnUiThread(()->status.setText("未完成写入："+e.getMessage()+"\n可重新按开始后再试。"));}
    }
}
