package io.github.ayxqn.salestart;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Local diagnostic facts only. Callers must pass authored text, never server bodies or exceptions. */
public final class Diagnostics {
    private static File dir;
    public static synchronized void init(Context c){dir=c.getFilesDir();}
    private static String time(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date());}
    public static synchronized void event(String stage,String code,String message){
        if(dir==null)return;
        if(!stage.matches("[A-Za-z0-9_-]{1,40}")||!code.matches("[A-Za-z0-9_-]{1,80}"))return;
        try{
            File log=new File(dir,"diagnostics.jsonl"),old=new File(dir,"diagnostics.previous.jsonl");
            if(log.length()>131072){if(old.exists()&&!old.delete())return;if(!log.renameTo(old))return;}
            JSONObject row=new JSONObject().put("time_beijing",time()).put("version","2.0.0").put("stage",stage).put("code",code).put("message",message);
            try(FileOutputStream out=new FileOutputStream(log,true)){out.write((row.toString()+"\n").getBytes(StandardCharsets.UTF_8));}
        }catch(Exception ignored){/* Logging must never turn a submitted order into an automatic retry. */}
    }
    public static void network(String stage,int status,int apiCode){event(stage,"HTTP_"+status,"官方响应：HTTP "+status+"，业务码 "+apiCode+"。这里只记录数字，不记录响应正文。");}
    public static String outcome(PurchaseRules.Outcome o){switch(o){case CREATED:return "订单已创建";case BUSY:return "活动繁忙，官方明确未生成订单";case NOT_OPEN:return "官方明确尚未开售";case SOLD_OUT:return "官方显示已售罄";case BLOCKED:return "官方要求验证或限制请求";case REJECTED:return "官方未接受订单";default:return "订单结果不明确";}}
    public static String failure(Throwable e){if(e instanceof java.net.UnknownHostException)return "域名解析失败，检查网络或 DNS";if(e instanceof javax.net.ssl.SSLException)return "加密连接校验失败，检查手机时间和网络";if(e instanceof java.net.SocketTimeoutException||e instanceof java.io.InterruptedIOException)return "网络等待超时或连接被取消";if(e instanceof java.net.ConnectException)return "无法连接官方服务器";if(e instanceof InterruptedException)return "任务已被停止或中断";if(e instanceof java.io.IOException)return "网络连接中断或响应无法读取";return "本机处理或数据校验异常";}
    public static synchronized String read(){
        StringBuilder out=new StringBuilder("开售助手 2.0.0 · 本机诊断记录\n时间为北京时间；未包含登录凭据、完整订单号或支付签名。\n\n");
        if(dir==null)return out.toString();
        for(String name:new String[]{"diagnostics.previous.jsonl","diagnostics.jsonl"}){File file=new File(dir,name);if(!file.exists())continue;
            try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null){try{JSONObject r=new JSONObject(line);out.append(r.optString("time_beijing")).append(" [").append(r.optString("stage")).append('/').append(r.optString("code")).append("] ").append(r.optString("message")).append('\n');}catch(Exception ignored){}}}catch(Exception ignored){out.append("部分记录无法读取。\n");}}
        return out.length()>10?out.toString():"暂无诊断记录。";
    }
    public static synchronized File export()throws Exception{File f=new File(dir,"diagnostics-export.txt");try(FileOutputStream out=new FileOutputStream(f)){out.write(read().getBytes(StandardCharsets.UTF_8));}return f;}
    public static synchronized void clear(){if(dir!=null)for(String name:new String[]{"diagnostics.jsonl","diagnostics.previous.jsonl","diagnostics-export.txt"}){File f=new File(dir,name);if(f.exists())f.delete();}}
}
