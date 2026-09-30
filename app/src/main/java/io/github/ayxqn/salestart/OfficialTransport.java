package io.github.ayxqn.salestart;

import android.os.SystemClock;
import okhttp3.*;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** HTTPS allowlist, bounded response, no redirects or implicit request replay. */
public final class OfficialTransport implements BiliClient.Transport {
    private final OkHttpClient base=new OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
        .connectTimeout(3,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).writeTimeout(3,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).build();
    private volatile Call active;private volatile boolean cancelled;
    public static double mono(){return SystemClock.elapsedRealtimeNanos()/1000000.0;}
    @Override public BiliClient.Reply request(String method,String url,JSONObject form,String cookie,int timeout)throws Exception {
        return prepare(method,url,form,cookie,timeout).executeAt(Double.NaN);
    }
    @Override public BiliClient.Pending prepare(String method,String url,JSONObject form,String cookie,int timeout)throws Exception {
        if(cancelled)throw new InterruptedException();
        if(!PurchaseRules.apiDestination(url))throw new BiliClient.Problem("网络地址不在官方接口范围内，已停止。");
        Request.Builder rb=new Request.Builder().url(url).header("Cookie",cookie).header("User-Agent","Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Mobile Safari/537.36")
            .header("Referer",BiliClient.PAGE);
        if(url.contains("/activity/sale/"))rb.header("Cache-Control","no-cache");
        if("POST".equals(method)){
            FormBody.Builder f=new FormBody.Builder();java.util.Iterator<String> names=form.keys();while(names.hasNext()){String k=names.next();f.add(k,String.valueOf(form.get(k)));}rb.post(f.build()).header("Origin","https://www.bilibili.com");
        }else if(!"GET".equals(method))throw new BiliClient.Problem("请求类型不受支持。");
        AtomicInteger wires=new AtomicInteger();
        OkHttpClient client=base.newBuilder().callTimeout(timeout,TimeUnit.MILLISECONDS).addNetworkInterceptor(chain->{if(wires.incrementAndGet()>1)throw new IOException("AUTOMATIC_REPLAY_BLOCKED");return chain.proceed(chain.request());}).build();
        Call call=client.newCall(rb.build());
        return deadline->{
        if(cancelled)throw new InterruptedException();active=call;
        if(Double.isFinite(deadline))waitDeadline(deadline);
        if(cancelled){call.cancel();throw new InterruptedException();}double sent=mono();
        try(Response response=call.execute()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();ResponseBody body=response.body();
            if(body!=null)try(InputStream in=body.byteStream()) {byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>524288)throw new IOException("RESPONSE_TOO_LARGE");out.write(buffer,0,n);}}
            double received=mono();JSONObject json;try{json=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}catch(Exception invalid){json=new JSONObject();}
            long age;try{age=Long.parseLong(response.header("Age","0"));}catch(Exception e){age=Long.MAX_VALUE;}
            String phase=url.contains("/order/create/")?"order":url.contains("/nav")?"account":"activity";
            if(!"order".equals(phase))Diagnostics.network(phase,response.code(),json.optInt("code",Integer.MIN_VALUE));
            return new BiliClient.Reply(response.code(),json,sent,received,age);
        }finally{active=null;}
        };
    }
    private void waitDeadline(double end)throws InterruptedException{double left;while((left=end-mono())>0){if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedException();if(left>30)Thread.sleep((long)Math.max(1,Math.min(250,left-20)));else if(left>0.75)java.util.concurrent.locks.LockSupport.parkNanos((long)((left-0.6)*1000000));else {while(mono()<end){if(cancelled)throw new InterruptedException();}break;}}}
    @Override public void cancel(){cancelled=true;Call call=active;if(call!=null)call.cancel();base.connectionPool().evictAll();}
}
