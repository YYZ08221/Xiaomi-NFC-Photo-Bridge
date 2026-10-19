package local.codex.xiaominfcprobe;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.widget.Toast;
public final class BridgeTriggerActivity extends Activity {
    public void onCreate(Bundle state){
        super.onCreate(state);
        boolean valid=NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())
            &&"xiaomi-photo-bridge://send/desk-1".equals(getIntent().getDataString())
            &&getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG)!=null;
        android.util.Log.i("XiaomiNfcBridge","PHYSICAL_NFC_ENTRY valid="+valid);
        if(!valid)Toast.makeText(this,"此入口需要扫描本工具的 NFC 标签",Toast.LENGTH_SHORT).show();
        else {boolean accepted=GalleryBridgeService.arm();android.util.Log.i("XiaomiNfcBridge","PHYSICAL_NFC_ARM accepted="+accepted);if(!accepted)Toast.makeText(this,"请启用碰传桥接无障碍服务，或等待当前任务结束",Toast.LENGTH_LONG).show();}
        finish();
    }
}
