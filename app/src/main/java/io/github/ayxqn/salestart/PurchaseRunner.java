package io.github.ayxqn.salestart;

import org.json.JSONObject;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** One user-initiated run, serial requests, durable uncertainty before the first POST. */
public final class PurchaseRunner {
    public interface Listener {void status(String state,String text,long target,double offset);void created();}
    private final BiliClient api;private final SecureStore vault;private final Listener listener;
    private volatile boolean cancelled;
    private final List<double[]> clockSamples=new ArrayList<>();
    private final List<String[]> pendingLogs=new ArrayList<>();
    private void flush(){for(String[] row:pendingLogs)Diagnostics.event("order",row[0],row[1]);pendingLogs.clear();}
    public PurchaseRunner(BiliClient api,SecureStore vault,Listener listener){this.api=api;this.vault=vault;this.listener=listener;}
    public void cancel(){cancelled=true;api.cancel();}
    private void alive()throws InterruptedException{if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedException();}
    private void waitMono(double deadline)throws InterruptedException{while(true){alive();double left=deadline-OfficialTransport.mono();if(left<=0)return;if(left>20)Thread.sleep((long)Math.min(200,left-10));else java.util.concurrent.locks.LockSupport.parkNanos((long)(left*1000000));}}
    private void report(String state,String text,long target,double offset){listener.status(state,text,target,offset);}
    private String key(long target)throws Exception{
        SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        byte[] digest=MessageDigest.getInstance("SHA-256").digest((api.uid()+"|"+BiliClient.ACTIVITY+"|"+f.format(new Date(target))).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder("attempt-");for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();
    }
    private double[] calibrate(long target)throws Exception{
        List<double[]> samples=clockSamples;double[] fit=null;double next=OfficialTransport.mono();
        for(int i=0;i<12;i++){
            alive();if(target>0&&target-(fit==null?System.currentTimeMillis():OfficialTransport.mono()+fit[0])<8000+Math.max(0,next-OfficialTransport.mono()))break;
            waitMono(next);double[] s=api.clockSample();samples.add(s);
            if(samples.size()>=3){try{fit=PurchaseRules.fit(samples.toArray(new double[0][]));}catch(IllegalArgumentException e){throw new BiliClient.Problem("官方校时时间样本不一致或无效，未准备下单。请网络稳定后再试。");}if(samples.size()>=6&&fit[1]<=160)break;}
            double earliest=Math.max(OfficialTransport.mono()+10,s[1]+1137);
            if(fit==null)next=earliest;else {double bias=(samples.size()%2==0?-1:1)*Math.min(60,fit[1]/4);double half=(s[2]-s[1])/2;next=Math.ceil((earliest+fit[0]+half-bias)/1000)*1000-fit[0]-half+bias;}
        }
        if(fit==null||fit[1]>500)throw new BiliClient.Problem("官方时间误差范围过大，未提交订单。请网络稳定后重新启动。");
        return fit;
    }
    public void run()throws Exception {try{runOnce();}finally{flush();}}
    private void runOnce()throws Exception {
        alive();report("checking","正在核对登录与官方开售时间",0,0);api.account();alive();
        long target=api.nextSale(),now=System.currentTimeMillis();
        if(target>0&&(target<now-3000||target-now>24*3600000L))throw new BiliClient.Problem("没有未来 24 小时内可参加的场次。");
        if(target>0){
            report("waiting","已启动，可放到后台；开售前自动校时和读取套餐",target,0);
            // Sleep against a monotonic anchor; a large wall-clock change aborts later calibration.
            double anchor=OfficialTransport.mono();long wall=System.currentTimeMillis();waitMono(anchor+Math.max(0,target-wall-30000));
            if(Math.abs(System.currentTimeMillis()-wall-(OfficialTransport.mono()-anchor))>3000)throw new BiliClient.Problem("手机时间在等待期间发生变化，请重新启动。");
            alive();api.account();report("calibrating","正在用官方活动时间自动校准",target,0);
        }
        double[] fit;
        if(target==0){double before=OfficialTransport.mono();long epoch=System.currentTimeMillis();try{if(android.os.Build.VERSION.SDK_INT>=33)epoch=android.os.SystemClock.currentNetworkTimeClock().millis();}catch(Exception ignored){}fit=new double[]{epoch-(before+OfficialTransport.mono())/2,0};}
        else fit=calibrate(target);
        if(target>0)waitMono(target-5500-fit[0]);
        alive();BiliClient.Offer offer=api.offer(2000);
        if(target>0){clockSamples.add(offer.sample);try{fit=PurchaseRules.fit(clockSamples.toArray(new double[0][]));}catch(Exception e){throw new BiliClient.Problem("临近开售时官方时间样本不一致，未发送请求。");}}
        double offset=fit[0]+OfficialTransport.mono()-System.currentTimeMillis();
        if(target==0){if(!"ON_SALE".equals(offer.status))throw new BiliClient.Problem("官方活动状态已改变，请重新启动。");target=Math.round(OfficialTransport.mono()+fit[0]);}
        else if(!"ON_SALE".equals(offer.status)&&offer.next!=target)throw new BiliClient.Problem("官方开售时间已改变，本次未下单。");
        if(!("ON_SALE".equals(offer.status)||"ABOUT_TO_OPEN".equals(offer.status)||"NOT_ON_SALE_TODAY".equals(offer.status)||"SOLD_OUT_TODAY".equals(offer.status)))throw new BiliClient.Problem("当前账号或活动状态不允许购买。");
        if(OfficialTransport.mono()+fit[0]>target+1000)throw new BiliClient.Problem("准备完成时已超过预定开售时间，未下单。");
        String attemptKey=key(target);JSONObject previous=vault.read(attemptKey);
        if(previous!=null)throw new BiliClient.Problem("这个账号本场已有提交记录。请先在官方订单中核对；可用“订单”打开已保存的收银台。");
        double deadline=target-fit[0];List<BiliClient.PreparedOrder> prepared=new ArrayList<>();prepared.add(api.prepareOrder(offer));
        if(deadline-OfficialTransport.mono()>25)for(int i=1;i<3;i++)prepared.add(api.prepareOrder(offer));
        alive();vault.write(attemptKey,new JSONObject().put("state","possibly_sent").put("target",target));
        report("ready","套餐已核对，等待开售提交",target,offset);double next=deadline,lastFlush=OfficialTransport.mono();
        for(int attempt=1;;attempt++){
            alive();BiliClient.Order order;
            try{BiliClient.PreparedOrder request=prepared.isEmpty()?api.prepareOrder(offer):prepared.remove(0);order=request.executeAt(next);}
            catch(Exception e){flush();Diagnostics.event("order","RESULT_UNKNOWN",Diagnostics.failure(e)+"；不自动重发，请到官方核对订单。");try{vault.write(attemptKey,new JSONObject().put("state","unknown").put("target",target));}catch(Exception ignored){}throw new BiliClient.Problem("下单结果暂不明确，已停止重发。请到 B 站核对订单，避免重复购买。");}
            double sent=order.reply.sent;
            pendingLogs.add(new String[]{order.outcome.name(),String.format(Locale.ROOT,"第 %d 次：%s；HTTP %d，业务码 %d，往返 %.1f 毫秒，本机调度迟到 %.3f 毫秒（不代表服务器收到时间）。",attempt,Diagnostics.outcome(order.outcome),order.reply.status,order.reply.body.optInt("code",Integer.MIN_VALUE),order.reply.received-sent,Math.max(0,sent-next))});
            if(order.outcome==PurchaseRules.Outcome.CREATED){
                flush();try{vault.write("payment",new JSONObject().put("uid",api.uid()).put("params",order.payment).put("created_at",System.currentTimeMillis()));
                vault.write(attemptKey,new JSONObject().put("state","created").put("target",target));}catch(Exception e){throw new BiliClient.Problem("官方已经创建订单，但本地付款入口保存失败。不要重复启动，请直接到 B 站官方订单页付款。");}
                report("created","178 元订单已创建，点通知或“订单”选择支付方式",0,offset);listener.created();return;
            }
            long elapsed=(long)(OfficialTransport.mono()-deadline);
            if(PurchaseRules.canRetry(order.outcome,attempt,elapsed)){
                if(elapsed>=3000&&OfficialTransport.mono()-lastFlush>=5000){flush();report("retrying","官方明确未生成订单，继续按原规则串行请求；可点停止",target,offset);lastFlush=OfficialTransport.mono();}
                next=Math.max(sent+PurchaseRules.retryInterval(elapsed),OfficialTransport.mono());continue;
            }
            vault.write(attemptKey,new JSONObject().put("state",order.outcome.name()).put("target",target));
            String msg=order.outcome==PurchaseRules.Outcome.SOLD_OUT?"官方显示套餐已售罄。":order.outcome==PurchaseRules.Outcome.BLOCKED?"官方要求验证或限制请求，已停止。请在 B 站自行处理。":order.outcome==PurchaseRules.Outcome.UNKNOWN?"订单结果不明确，已停止重发；请到 B 站核对。":"官方未接受订单，已停止本场任务。";
            throw new BiliClient.Problem(msg);
        }
    }
}
