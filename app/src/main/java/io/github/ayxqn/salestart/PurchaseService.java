package io.github.ayxqn.salestart;

import android.app.*;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.*;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** User-started foreground service; no boot receiver or automatic recovery purchase. */
public final class PurchaseService extends Service {
    public static final String START="start",STOP="stop",DEMO="demo",CHANNEL="sale-task-v2",RESULT_CHANNEL="sale-result-v2";
    public static final AtomicBoolean RUNNING=new AtomicBoolean();
    private Thread worker;
    private volatile PurchaseRunner runner;private volatile boolean cancelled;private PowerManager.WakeLock wake;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){super.onCreate();channels(this);Diagnostics.init(this);}
    public static void channels(Context c){NotificationManager m=c.getSystemService(NotificationManager.class);
        NotificationChannel a=new NotificationChannel(CHANNEL,"正在等待开售",NotificationManager.IMPORTANCE_LOW);a.setDescription("后台等待、校时和提交进度");m.createNotificationChannel(a);
        NotificationChannel b=new NotificationChannel(RESULT_CHANNEL,"订单与任务结果",NotificationManager.IMPORTANCE_HIGH);b.setDescription("订单创建后点通知进入官方收银台");b.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);m.createNotificationChannel(b);
    }
    private PendingIntent open(boolean cashier){Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);if(cashier)i.setAction("open_cashier");return PendingIntent.getActivity(this,cashier?21:20,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notice(String text,boolean ongoing,boolean cashier){Notification.Builder b=new Notification.Builder(this,ongoing?CHANNEL:RESULT_CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("开售助手").setContentText(text)
        .setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open(cashier)).setOngoing(ongoing).setAutoCancel(!ongoing).setOnlyAlertOnce(ongoing);
        if(ongoing){PendingIntent stop=PendingIntent.getService(this,22,new Intent(this,PurchaseService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);b.addAction(new Notification.Action.Builder(null,"停止",stop).build());}
        return b.build();
    }
    public static void state(Context c,String state,String message,long target,double offset){c.getSharedPreferences("task_v2",MODE_PRIVATE).edit().putString("state",state).putString("message",message).putLong("target",target).putLong("offset",Math.round(offset)).putLong("updated",System.currentTimeMillis()).commit();}
    private void progress(String state,String text,long target,double offset){state(this,state,text,target,offset);Diagnostics.event("task",state,text);getSystemService(NotificationManager.class).notify(20,notice(text,true,false));}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null)return START_NOT_STICKY;
        if(STOP.equals(intent.getAction())){cancelled=true;PurchaseRunner r=runner;if(r!=null)r.cancel();if(worker!=null)worker.interrupt();state(this,"stopped","已停止。已发送的订单请求无法撤回，请核对官方订单。",0,0);Diagnostics.event("task","USER_STOP","用户停止任务；已发送的请求不能撤回。");if(worker==null)finishTask();return START_NOT_STICKY;}
        if(!START.equals(intent.getAction())&&!DEMO.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(!RUNNING.compareAndSet(false,true))return START_NOT_STICKY;
        Notification n=notice("正在准备后台任务",true,false);
        try{if(Build.VERSION.SDK_INT>=34)startForeground(20,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(20,n);
            wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SaleStart:scheduled-order");wake.setReferenceCounted(false);wake.acquire(24*3600000L+180000L);
        }catch(Exception e){state(this,"failed","后台任务未能启动，请检查系统的后台运行限制。",0,0);Diagnostics.event("service","START_DENIED","系统没有允许启动后台服务。");finishTask();return START_NOT_STICKY;}
        final boolean demo=DEMO.equals(intent.getAction());
        worker=new Thread(()->{
            try{
                if(demo){long end=SystemClock.elapsedRealtime()+20000;progress("demo","20 秒后台演练中，可以返回桌面或锁屏；不会联网下单",System.currentTimeMillis()+20000,0);
                    while(SystemClock.elapsedRealtime()<end){if(cancelled)throw new InterruptedException();Thread.sleep(100);}state(this,"demo_done","后台演练完成，没有联网或创建订单。",0,0);Diagnostics.event("demo","COMPLETED","后台演练完成；未联网、未下单。");getSystemService(NotificationManager.class).notify(21,notice("后台演练完成，没有下单",false,false));
                }else{
                    SecureStore vault=new SecureStore(this);JSONObject session=vault.read("session");if(session==null)throw new BiliClient.Problem("请先在应用内登录自己的 B 站账号。");
                    BiliClient api=new BiliClient(new OfficialTransport(),session.getString("cookie"));
                    if(!api.uid().equals(session.getString("uid")))throw new BiliClient.Problem("本机账号记录不一致，请重新登录。");
                    runner=new PurchaseRunner(api,vault,new PurchaseRunner.Listener(){
                        public void status(String s,String t,long target,double offset){progress(s,t,target,offset);}
                        public void created(){getSystemService(NotificationManager.class).notify(21,notice("178 元订单已创建，点这里进入官方收银台",false,true));}
                    });runner.run();
                }
            }catch(InterruptedException e){if(!cancelled)state(this,"stopped","任务已中断，请核对官方订单后再操作。",0,0);Diagnostics.event("task","INTERRUPTED","等待被中断，任务已停止。");}
            catch(Exception e){String text=e instanceof BiliClient.Problem?e.getMessage():Diagnostics.failure(e)+"。若已到开售时间，请先核对官方订单。";state(this,"failed",text,0,0);Diagnostics.event("task",e instanceof BiliClient.Problem?"STOP_REASON":"LOCAL_OR_NETWORK_ERROR",text);getSystemService(NotificationManager.class).notify(21,notice(text,false,false));}
            finally{PurchaseRunner r=runner;if(r!=null)r.cancel();finishTask();}
        },"sale-task");worker.start();return START_NOT_STICKY;
    }
    private void finishTask(){new Handler(Looper.getMainLooper()).post(()->{if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();});}
    @Override public void onTaskRemoved(Intent root){super.onTaskRemoved(root);}
    @Override public void onDestroy(){cancelled=true;PurchaseRunner r=runner;if(r!=null)r.cancel();if(worker!=null)worker.interrupt();if(wake!=null&&wake.isHeld())wake.release();RUNNING.set(false);super.onDestroy();}
}
