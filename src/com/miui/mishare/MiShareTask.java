package local.codex.xiaominfcprobe.wire;
import android.os.*;
import android.content.ClipData;
public final class MiShareTask implements Parcelable {
    public boolean send=true;
    public String taskId,mimeType;
    public int count,deviceX,deviceY,tbWidth,tbHeight;
    public RemoteDevice device;
    public ClipData clipData;
    public MiShareTask(){}
    private MiShareTask(Parcel p){
        send=p.readByte()!=0;taskId=p.readString();count=p.readInt();
        String deviceClass=p.readString();
        if(!"com.miui.mishare.RemoteDevice".equals(deviceClass))throw new IllegalStateException("Unexpected remote Parcelable class");
        device=RemoteDevice.CREATOR.createFromParcel(p);
        deviceX=p.readInt();deviceY=p.readInt();
        clipData=p.readParcelable(ClipData.class.getClassLoader());
        mimeType=p.readString();tbWidth=p.readInt();tbHeight=p.readInt();
    }
    public void writeToParcel(Parcel p,int flags){
        p.writeByte((byte)(send?1:0));p.writeString(taskId);p.writeInt(count);
        p.writeString("com.miui.mishare.RemoteDevice");device.writeToParcel(p,flags);p.writeInt(deviceX);p.writeInt(deviceY);
        p.writeParcelable(clipData,flags);p.writeString(mimeType);p.writeInt(tbWidth);p.writeInt(tbHeight);
    }
    public int describeContents(){return 0;}
    public static final Creator<MiShareTask> CREATOR=new Creator<MiShareTask>(){
        public MiShareTask createFromParcel(Parcel p){return new MiShareTask(p);}
        public MiShareTask[] newArray(int size){return new MiShareTask[size];}
    };
}
