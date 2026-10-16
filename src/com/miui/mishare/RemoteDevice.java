package local.codex.xiaominfcprobe.wire;
import android.os.*;
public final class RemoteDevice implements Parcelable {
    public final String id;
    public final Bundle extras;
    public RemoteDevice(String id, Bundle extras) {this.id=id; this.extras=extras;}
    private RemoteDevice(Parcel p) {id=p.readString(); extras=p.readBundle(getClass().getClassLoader());}
    public void writeToParcel(Parcel p,int flags) {p.writeString(id);p.writeBundle(extras);}
    public int describeContents(){return 0;}
    public static final Creator<RemoteDevice> CREATOR=new Creator<RemoteDevice>() {
        public RemoteDevice createFromParcel(Parcel p){return new RemoteDevice(p);}
        public RemoteDevice[] newArray(int size){return new RemoteDevice[size];}
    };
}
