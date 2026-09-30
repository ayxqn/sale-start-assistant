package io.github.ayxqn.salestart;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import org.json.JSONObject;

/** Separate, signed test APK only. Never reads the production session or performs network I/O. */
public final class DeviceSelfTest extends Instrumentation {
    private int checks;
    private void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();SecureStore store=null;
        try{
            PurchaseTest.main(new String[0]);
            Context c=getTargetContext();store=new SecureStore(c,"_device_test");store.clear();
            store.write("a",new JSONObject().put("secret","synthetic-only"));
            check("synthetic-only".equals(store.read("a").getString("secret")),"Keystore round trip");
            android.content.SharedPreferences p=c.getSharedPreferences("secure_v2_device_test",Context.MODE_PRIVATE);
            String cipher=p.getString("a","");check(!cipher.contains("synthetic-only"),"no plaintext in encrypted storage");
            store.write("a",new JSONObject().put("secret","synthetic-only"));check(!cipher.equals(p.getString("a","")),"random IV per write");
            p.edit().putString("b",p.getString("a","")).commit();boolean rejected=false;try{store.read("b");}catch(Exception e){rejected=true;}check(rejected,"AAD blocks slot swapping");
            JSONObject envelope=new JSONObject(p.getString("a",""));String data=envelope.getString("data");envelope.put("data",(data.charAt(0)=='A'?"B":"A")+data.substring(1));p.edit().putString("a",envelope.toString()).commit();rejected=false;try{store.read("a");}catch(Exception e){rejected=true;}check(rejected,"tamper rejected");
            store.clear();check(store.read("a")==null,"clear removes isolated data");
            store.write("session",new JSONObject().put("fixture",true));store.write("payment",new JSONObject());store.write("attempt-fixture",new JSONObject());store.logout();
            check(store.read("session")==null&&store.read("payment")==null&&store.read("attempt-fixture")!=null,"logout retains duplicate protection");
            store.clear();
            Scripted transport=new Scripted();BiliClient client=new BiliClient(transport,PurchaseTest.COOKIE);
            final int[] created={0};PurchaseRunner.Listener l=new PurchaseRunner.Listener(){public void status(String a,String b,long t,double o){}public void created(){created[0]++;}};
            new PurchaseRunner(client,store,l).run();check(transport.posts==1&&created[0]==1,"one synthetic order success");check(store.read("payment").getString("uid").equals("12345"),"synthetic order bound to fixture account");
            boolean duplicate=false;try{new PurchaseRunner(client,store,l).run();}catch(BiliClient.Problem e){duplicate=true;}check(duplicate&&transport.posts==1,"same-session restart never resubmits");
            store.clear();transport=new Scripted();transport.timeout=true;client=new BiliClient(transport,PurchaseTest.COOKIE);
            boolean unknown=false;try{new PurchaseRunner(client,store,l).run();}catch(BiliClient.Problem e){unknown=true;}check(unknown&&transport.posts==1,"timeout no replay");
            try{new PurchaseRunner(client,store,l).run();}catch(BiliClient.Problem expected){}check(transport.posts==1,"uncertain durable ledger blocks resubmission");
            result.putString("stream","PASS: 77 contract checks + "+checks+" Android Keystore/runner checks. Isolated synthetic data; no network, no real orders, production session untouched.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+e.getClass().getSimpleName()+" "+e.getMessage()+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{if(store!=null)try{store.clear();}catch(Exception ignored){}}
    }
    static final class Scripted implements BiliClient.Transport {
        int posts;boolean timeout;
        public BiliClient.Reply request(String method,String url,JSONObject form,String cookie,int limit)throws Exception{
            double t=OfficialTransport.mono();JSONObject body;
            if(method.equals("POST")){posts++;if(timeout)throw new java.net.SocketTimeoutException("synthetic");body=PurchaseTest.reply(0,PurchaseTest.payment());}
            else if(url.contains("/nav"))body=PurchaseTest.reply(0,new JSONObject().put("isLogin",true).put("mid",12345).put("uname","Fixture"));
            else body=PurchaseTest.offerBody(PurchaseTest.sku());
            return new BiliClient.Reply(200,body,t,OfficialTransport.mono(),0);
        }
        public void cancel(){}
    }
}
