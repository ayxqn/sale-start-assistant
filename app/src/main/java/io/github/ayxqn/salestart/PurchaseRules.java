package io.github.ayxqn.salestart;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure rules. No network, Android, credentials, or real orders in this class. */
public final class PurchaseRules {
    public static final long AMOUNT = 17800;
    public enum Outcome { CREATED, NOT_OPEN, BUSY, SOLD_OUT, BLOCKED, REJECTED, UNKNOWN }
    private PurchaseRules() {}

    public static Outcome classify(int http, int code, boolean hasOrder, boolean validPayment, boolean dialog, String message) {
        if (http == 200 && code == 0 && !dialog && validPayment) return Outcome.CREATED;
        if (hasOrder) return Outcome.UNKNOWN;
        String msg = message == null ? "" : message;
        if (http == 429 || code == -412 || code == -352 || msg.matches("(?s).*(验证码|验证身份|操作频繁|请求频繁|风控|账号异常).*")) return Outcome.BLOCKED;
        if (http != 200 || code == Integer.MIN_VALUE) return Outcome.UNKNOWN;
        if (msg.matches("(?s).*(售罄|售完|抢光|库存不足).*")) return Outcome.SOLD_OUT;
        if (code == 0 && dialog && msg.matches("(?s).*(尚未开售|暂未开售|还未开售|未到开售时间|活动尚未开始|活动未开始).*")) return Outcome.NOT_OPEN;
        if (code == 43055 && msg.contains("活动太火爆") && msg.contains("稍后再试")) return Outcome.BUSY;
        if (dialog || code != 0) return Outcome.REJECTED;
        return Outcome.UNKNOWN;
    }

    public static boolean canRetry(Outcome outcome, int attempt, long elapsed) {
        return (outcome == Outcome.NOT_OPEN || outcome == Outcome.BUSY) && attempt > 0 && elapsed >= 0;
    }
    public static long retryInterval(long elapsed) { return elapsed<3000?100:elapsed<10000?300:1000; }

    public static Map<String,String> cookies(String raw) {
        if (raw == null || raw.length() > 16384 || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0) throw new IllegalArgumentException("登录信息无效，请重新登录。");
        Map<String,String> out = new LinkedHashMap<>();
        for (String part : raw.split(";")) {
            int i = part.indexOf('='); if (i < 1) continue;
            String k = part.substring(0,i).trim(), v = part.substring(i+1).trim();
            if (k.equals("SESSDATA") || k.equals("bili_jct") || k.equals("DedeUserID") || k.equals("DedeUserID__ckMd5") || k.equals("bili_ticket") || k.equals("bili_ticket_expires")) {
                if (!v.matches("[\\x21-\\x7E]{1,4096}") || v.indexOf(';') >= 0 || out.containsKey(k)) throw new IllegalArgumentException("登录信息冲突，请重新登录。");
                out.put(k,v);
            }
        }
        if (!out.containsKey("SESSDATA") || !out.containsKey("bili_jct") || !out.containsKey("DedeUserID") || !out.get("DedeUserID").matches("[0-9]{1,20}")) throw new IllegalArgumentException("尚未取得完整登录信息。请在官方页面完成登录，再点核对。");
        return out;
    }
    public static String cookieHeader(Map<String,String> values) {
        StringBuilder b = new StringBuilder(); for (Map.Entry<String,String> e: values.entrySet()) { if (b.length() > 0) b.append("; "); b.append(e.getKey()).append('=').append(e.getValue()); } return b.toString();
    }

    public static boolean officialWeb(String raw, boolean resource) {
        try {
            URI u = new URI(raw); String h=u.getHost();
            if (!"https".equals(u.getScheme()) || h==null || u.getRawUserInfo()!=null || u.getPort()!=-1 || raw.contains("\\")) return false;
            if (h.equals("bilibili.com") || h.endsWith(".bilibili.com")) return true;
            return resource && (h.equals("hdslb.com") || h.endsWith(".hdslb.com") || h.equals("static.geetest.com") || h.equals("api.geetest.com") || h.equals("gcaptcha4.geetest.com") || h.equals("gctus.com") || h.endsWith(".gctus.com"));
        } catch (Exception e) { return false; }
    }
    public static boolean apiDestination(String raw) {
        try {
            URI u=new URI(raw);
            return "https".equals(u.getScheme()) && "api.bilibili.com".equals(u.getHost()) && u.getPort()==-1 && u.getRawUserInfo()==null && !raw.contains("\\");
        } catch(Exception e) { return false; }
    }

    /** Intersect truncated-server-second intervals, without assuming RTT symmetry. */
    public static double[] fit(double[][] samples) {
        if (samples.length < 3) throw new IllegalArgumentException("校时样本不足，未准备下单。");
        double lower=Double.NEGATIVE_INFINITY,upper=Double.POSITIVE_INFINITY;
        for(double[] s:samples) {
            if(s.length!=4 || !Double.isFinite(s[0]) || s[0]%1000!=0 || !Double.isFinite(s[1]) || !Double.isFinite(s[2]) || s[2]<s[1] || s[2]-s[1]>2000 || s[3]!=0) throw new IllegalArgumentException("校时样本无效，未准备下单。");
            lower=Math.max(lower,s[0]-s[2]);upper=Math.min(upper,s[0]+1000-s[1]);
        }
        if (!(upper>lower)) throw new IllegalArgumentException("活动时间样本不一致，未准备下单。");
        return new double[]{(lower+upper)/2,upper-lower};
    }
}
