package local.codex.xiaominfcprobe;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;
import android.provider.OpenableColumns;import android.webkit.MimeTypeMap;import java.io.*;
public final class BridgeImageProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File resolve(Uri uri)throws IOException{
        File base=new File(getContext().getFilesDir(),"shared").getCanonicalFile();
        File file=new File(base,uri.getPath().substring(1)).getCanonicalFile();
        if(!file.getPath().startsWith(base.getPath()+File.separator)||!file.isFile())throw new FileNotFoundException();return file;
    }
    public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        try{return ParcelFileDescriptor.open(resolve(u),ParcelFileDescriptor.MODE_READ_ONLY);}catch(IOException e){throw new FileNotFoundException();}
    }
    public String getType(Uri u){String n=u.getLastPathSegment();int at=n==null?-1:n.lastIndexOf('.');String t=at<0?null:MimeTypeMap.getSingleton().getMimeTypeFromExtension(n.substring(at+1).toLowerCase(java.util.Locale.ROOT));return t==null?"image/*":t;}
    public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){
        try{File f=resolve(u);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];
            for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(row);return c;
        }catch(IOException e){return null;}
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
