package io.github.ayxqn.salestart;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Fixed activity adapter migrated from the successful V2.4 script. */
public final class BiliClient {
    public static final String ACTIVITY="3ERAcwloghvy1e00", COMPONENT="i5lVNtVBuy";
    public static final String PAGE="https://www.bilibili.com/blackboard/era/rZPKSDqrJEOrtkVi.html";
    public interface Transport {
        Reply request(String method,String url,JSONObject form,String cookie,int timeout) throws Exception;
        default Pending prepare(String method,String url,JSONObject form,String cookie,int timeout) throws Exception {return deadline->request(method,url,form,cookie,timeout);}
        void cancel();
    }
    public interface Pending {Reply executeAt(double deadline) throws Exception;}
    public interface PreparedOrder {Order executeAt(double deadline) throws Exception;}
    public static final class Reply {
        public final int status; public final JSONObject body; public final double sent,received; public final long age;
        public Reply(int status,JSONObject body,double sent,double received,long age) {this.status=status;this.body=body;this.sent=sent;this.received=received;this.age=age;}
    }
    public static final class Problem extends Exception { public Problem(String message){super(message);} }
    public static final class Offer {
        public final JSONObject payload; public final String status; public final long next; public final double[] sample;
        Offer(JSONObject p,String s,long n,double[] sample){payload=p;status=s;next=n;this.sample=sample;}
    }
    public static final class Order {
        public final PurchaseRules.Outcome outcome; public final JSONObject payment;public final Reply reply;
        Order(PurchaseRules.Outcome o,JSONObject p,Reply r){outcome=o;payment=p;reply=r;}
    }
    private final Transport transport;
    private final Map<String,String> cookies;
    private final String cookie;
    public BiliClient(Transport transport,String rawCookie){this.transport=transport;cookies=PurchaseRules.cookies(rawCookie);cookie=PurchaseRules.cookieHeader(cookies);}
    public void cancel(){transport.cancel();}
    public String uid(){return cookies.get("DedeUserID");}
    public JSONObject account() throws Exception {
        Reply r=transport.request("GET","https://api.bilibili.com/x/web-interface/nav",null,cookie,5000);
        JSONObject d=r.body.optJSONObject("data");
        if(r.status!=200||r.body.optInt("code",-1)!=0||d==null||!d.optBoolean("isLogin"))throw new Problem("登录已失效，请重新登录 B 站。");
        if(!uid().equals(String.valueOf(d.optLong("mid",-1))))throw new Problem("登录账号信息不一致，请退出后重新登录。");
        return new JSONObject().put("uid",uid()).put("name",d.optString("uname","B 站用户"));
    }
    private Reply activity(int timeout) throws Exception {
        JSONObject ext=new JSONObject().put("activity_id",ACTIVITY).put("component_id",COMPONENT).put("is_preview",false).put("creative_id",0).put("new_act_panel",1);
        String url="https://api.bilibili.com/x/vip/activity/sale/summer2026/attract_card?platform=pc&scene=activity&new_act_panel=1&csrf="+encode(cookies.get("bili_jct"))+"&ext_params="+encode(ext.toString());
        Reply r=transport.request("GET",url,null,cookie,timeout);
        if(r.status!=200||r.body.optInt("code",-1)!=0||r.body.optJSONObject("data")==null)throw new Problem("官方活动暂时无法读取，或当前账号没有资格。没有提交订单。");
        return r;
    }
    public double[] clockSample() throws Exception {Reply r=activity(2000);return sample(r);}
    private double[] sample(Reply r) throws Exception {
        double server=r.body.getJSONObject("data").optDouble("current_time",Double.NaN)*1000;
        return new double[]{server,r.sent,r.received,r.age};
    }
    public long nextSale() throws Exception {
        JSONObject d=activity(5000).body.getJSONObject("data");
        String status=d.optString("drainage_status");
        if("ON_SALE".equals(status))return 0;
        long next=d.optLong("next_open_at",0)*1000;
        if(!("ABOUT_TO_OPEN".equals(status)||"NOT_ON_SALE_TODAY".equals(status)||"SOLD_OUT_TODAY".equals(status))||next<=0)throw new Problem("官方没有返回下一场可参加的活动；可能已结束、已购或没有资格。");
        return next;
    }
    public Offer offer(int timeout) throws Exception {
        Reply r=activity(timeout);JSONObject d=r.body.getJSONObject("data");
        String status=d.optString("drainage_status");
        if("USER_HAS_BUY".equals(status))throw new Problem("官方显示这个账号已购买，未重复下单。");
        JSONArray panels=d.optJSONArray("activity_panels");JSONObject panel=null;
        if(panels!=null)for(int i=0;i<panels.length();i++){JSONObject p=panels.optJSONObject(i);if(p!=null&&COMPONENT.equals(p.optString("component_id"))){if(panel!=null)throw new Problem("活动商品列表发生变化，未下单。");panel=p;}}
        JSONArray skus=panel==null?null:panel.optJSONArray("skus");
        if(skus==null||skus.length()!=1)throw new Problem("官方尚未展示目标套餐，或活动已结束。没有改买其他套餐。");
        JSONObject sku=skus.getJSONObject(0),p=sku.optJSONObject("order_params");
        if(!identity(sku,p))throw new Problem("官方套餐信息与已适配的 178 元套餐不一致，已停止。");
        JSONObject payload=new JSONObject().put("platform","pc").put("os_ver","").put("country_code","").put("sdk_version","")
            .put("dtype",2).put("pay_sdk_version","1.5.4").put("from_activity",1).put("scene","activity").put("csrf",cookies.get("bili_jct"))
            .put("returnUrl","https://m.bilibili.com/doria/pay-success-page.html?source=0&scene=activity&wb_ui=a3_w100p_h70p_c1_m30_r15&wbType=common")
            .put("noToast",1).put("activity_id",ACTIVITY).put("component_id",COMPONENT).put("appId",0).put("allow_degrade_payway",true);
        java.util.Iterator<String> names=p.keys();while(names.hasNext()){String k=names.next();payload.put(k,p.get(k));}
        // Account fields are always supplied by this user's session, never by product data.
        payload.put("csrf",cookies.get("bili_jct"));payload.put("activity_id",ACTIVITY);payload.put("component_id",COMPONENT);
        JSONObject report=sku.optJSONObject("report_params");if(report==null)report=new JSONObject();else report=new JSONObject(report.toString());
        report.put("activity_id",ACTIVITY).put("activity_name","萌节十周年").put("from_spmid","").put("session_id","").put("spmid","888.153775.0.0").put("msource","zhc_cebianlan");
        payload.put("order_report_params",report.toString());
        return new Offer(payload,status,d.optLong("next_open_at",0)*1000,sample(r));
    }
    static boolean identity(JSONObject sku,JSONObject p) {
        return p!=null&&"超大年度套餐".equals(sku.optString("sku_name"))&&"tv".equals(sku.optString("product_biz"))
            &&sku.optDouble("discount_price",-1)==17800&&sku.optDouble("original_price",-1)==38800
            &&"https://i0.hdslb.com/bfs/vip/6630298d422ea44351fd30db6080e730a4bb46bf.jpg".equals(sku.optString("sku_image"))
            &&"1431533923".equals(p.optString("vip_sku_id"))&&"941208775".equals(p.optString("vip_price_id"))
            &&"977480454".equals(p.optString("buy_gift_id"))&&"prop".equals(p.optString("buy_gift_type"))&&"12".equals(p.optString("card_type"))
            &&!p.optString("promo_token").isEmpty()&&!p.optString("promo_uuid").isEmpty()&&!p.has("panel_type");
    }
    public Order createOrder(Offer offer) throws Exception {
        return prepareOrder(offer).executeAt(Double.NaN);
    }
    public PreparedOrder prepareOrder(Offer offer) throws Exception {
        Pending pending=transport.prepare("POST","https://api.bilibili.com/x/vip/order/create/activity",offer.payload,cookie,8000);
        return deadline->parseOrder(pending.executeAt(deadline));
    }
    private Order parseOrder(Reply r)throws Exception {
        JSONObject b=r.body,d=b.optJSONObject("data"),p=d;
        if(d!=null)for(String name:new String[]{"payParams","payParam","pay_param","pay_params"})if(d.optJSONObject(name)!=null){p=d.getJSONObject(name);break;}
        boolean hasOrder=reference(p)||reference(d),valid=validPayment(p);
        JSONObject dialog=d==null?null:d.optJSONObject("fail_dialog");
        String msg=dialog==null?b.optString("showMsg",b.optString("message",b.optString("msg"))):dialog.optString("content",dialog.optString("message"));
        PurchaseRules.Outcome outcome=PurchaseRules.classify(r.status,b.optInt("code",Integer.MIN_VALUE),hasOrder,valid,dialog!=null,msg);
        return new Order(outcome,outcome==PurchaseRules.Outcome.CREATED?p:null,r);
    }
    static boolean reference(JSONObject p){return p!=null&&(!p.optString("orderId").isEmpty()||!p.optString("order_no").isEmpty()||!p.optString("vipOrderNo").isEmpty());}
    public static boolean validPayment(JSONObject p){return p!=null&&p.optDouble("payAmount",-1)==PurchaseRules.AMOUNT&&!p.optString("orderId").isEmpty()&&!p.optString("sign").isEmpty();}
    public static String cashier(JSONObject payment) throws Exception {
        if(!validPayment(payment))throw new Problem("没有经过核对的 178 元订单，不能打开收银台。");
        return "https://pay.bilibili.com/pay-v2/cashier/cashier-desk?params="+encode(payment.toString());
    }
    public static String encode(String value){try{return URLEncoder.encode(value,"UTF-8").replace("+","%20");}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}}
}
