package io.github.ayxqn.salestart;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** All session, order and attempt data is encrypted in the app's private storage. */
public final class SecureStore {
    private final String alias;
    private static final Object KEY_LOCK=new Object();
    private final SharedPreferences prefs;
    public SecureStore(Context context) {this(context,"");}
    public SecureStore(Context context,String namespace) {if(!namespace.matches("[a-z0-9_-]{0,30}"))throw new IllegalArgumentException();alias="salestart-private-v2"+namespace;prefs=context.getSharedPreferences("secure_v2"+namespace,Context.MODE_PRIVATE);}
    private SecretKey key() throws Exception {
        synchronized(KEY_LOCK){
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(alias)) {
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());kg.generateKey();
        }
        return (SecretKey)ks.getKey(alias,null);
        }
    }
    public synchronized JSONObject read(String slot) throws Exception {
        String encoded=prefs.getString(slot,null);if(encoded==null)return null;
        JSONObject envelope=new JSONObject(encoded);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(envelope.getString("iv"),Base64.NO_WRAP)));
        c.updateAAD(slot.getBytes(StandardCharsets.UTF_8));
        return new JSONObject(new String(c.doFinal(Base64.decode(envelope.getString("data"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
    }
    public synchronized void write(String slot, JSONObject value) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD(slot.getBytes(StandardCharsets.UTF_8));
        JSONObject e=new JSONObject().put("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(c.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP));
        if(!prefs.edit().putString(slot,e.toString()).commit())throw new Exception("本地安全记录保存失败，不能继续下单。");
    }
    public synchronized void logout() throws Exception { if(!prefs.edit().remove("session").remove("payment").commit())throw new Exception("退出记录保存失败，请重试。"); }
    public synchronized void clear() throws Exception {
        if(!prefs.edit().clear().commit())throw new Exception("未能清空本地数据。");
        synchronized(KEY_LOCK){KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias(alias))ks.deleteEntry(alias);}
    }
}
