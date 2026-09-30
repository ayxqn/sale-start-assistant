package io.github.ayxqn.salestart;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Only one explicitly exported, already sanitized report can be shared by a temporary URI grant. */
public final class DiagnosticsProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File file(Uri u)throws FileNotFoundException{if(!"io.github.ayxqn.salestart.diagnostics".equals(u.getAuthority())||!"/diagnostics.txt".equals(u.getPath()))throw new FileNotFoundException();return new File(getContext().getFilesDir(),"diagnostics-export.txt");}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public String getType(Uri u){return "text/plain";}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});try{File f=file(u);c.addRow(new Object[]{"开售助手诊断日志.txt",f.length()});}catch(Exception ignored){}return c;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
