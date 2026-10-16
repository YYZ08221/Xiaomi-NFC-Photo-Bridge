package local.codex.xiaominfcprobe;
import android.content.*;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Disposable background-only relay. External entry is restricted to DUMP holders (ADB/system). */
public final class BackgroundNfcProbeReceiver extends BroadcastReceiver {
    public void onReceive(Context context,Intent request){
        try {
            if("native-chooser".equals(request.getStringExtra("mode"))){
                Log.i("XiaomiNfcBridge","DEBUG_CHOOSER accepted="+GalleryBridgeService.armChooserForDebug());return;
            }
            if("bridge".equals(request.getStringExtra("mode"))){
                Log.i("XiaomiNfcBridge","DEBUG_ARM accepted="+GalleryBridgeService.arm());return;
            }
            boolean lyra=request.getBooleanExtra("lyra",false);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] action="MIRROR".getBytes(StandardCharsets.US_ASCII);
            byte[] mac="00:00:00:00:00:00".getBytes(StandardCharsets.US_ASCII);
            bytes.write(0x65);bytes.write(action.length);bytes.write(action);
            bytes.write(0x01);bytes.write(mac.length);bytes.write(mac);
            if(lyra){bytes.write(0x79);bytes.write(1);bytes.write(1);}
            JSONObject value=new JSONObject();value.put("device_type_key",3);
            value.put("protocol_payload_key",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP));
            Intent i=new Intent("com.xiaomi.nfc.action.TAG_DISCOVERED");
            i.setPackage("com.milink.service");i.addFlags(0x11000020);
            i.putExtra("protocol_value_key",value.toString());
            context.sendBroadcast(i,"com.xiaomi.mi_connect_service.permission.RECEIVE_ENDPOINT");
            Log.i("XiaomiNfcProbe","BACKGROUND_NFC_REQUEST lyra="+lyra+"; no Activity launched");
        }catch(Exception e){Log.e("XiaomiNfcProbe","Background NFC relay failed",e);}
    }
}
