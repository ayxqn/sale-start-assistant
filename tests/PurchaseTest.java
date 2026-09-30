package io.github.ayxqn.salestart;
import org.json.*;
import java.util.*;

/** Synthetic replies only: no real login, network, order or payment. */
public final class PurchaseTest {
    private static int passed;
    interface Case {void run()throws Exception;}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed++;}
    static void fails(Case c,String name)throws Exception{boolean failed=false;try{c.run();}catch(Exception expected){failed=true;}check(failed,name);}
    static final String COOKIE="SESSDATA=fixture-session; bili_jct=fixture-csrf; DedeUserID=12345";
    static JSONObject payment()throws Exception{return new JSONObject().put("orderId","fixture-order").put("sign","fixture-sign").put("payAmount",17800);}
    static JSONObject sku()throws Exception{return new JSONObject().put("sku_name","超大年度套餐").put("product_biz","tv").put("discount_price",17800).put("original_price",38800)
        .put("sku_image","https://i0.hdslb.com/bfs/vip/6630298d422ea44351fd30db6080e730a4bb46bf.jpg")
        .put("order_params",new JSONObject().put("vip_sku_id","1431533923").put("vip_price_id","941208775").put("buy_gift_id","977480454").put("buy_gift_type","prop").put("card_type","12").put("promo_token","fixture-promo").put("promo_uuid","fixture-uuid"));}
    static JSONObject offerBody(JSONObject sku)throws Exception{return new JSONObject().put("code",0).put("data",new JSONObject().put("current_time",1790740800).put("drainage_status","ON_SALE").put("next_open_at",1790740800)
        .put("activity_panels",new JSONArray().put(new JSONObject().put("component_id",BiliClient.COMPONENT).put("skus",new JSONArray().put(sku)))));}
    static final class Fake implements BiliClient.Transport {
        JSONObject response;int status=200,calls;String method,url,cookie;JSONObject form;boolean fail;
        Fake(JSONObject r){response=r;}
        public BiliClient.Reply request(String m,String u,JSONObject f,String c,int timeout)throws Exception{calls++;method=m;url=u;form=f;cookie=c;if(fail)throw new java.io.IOException("fixture");return new BiliClient.Reply(status,response,10,20,0);}
        public void cancel(){}
    }
    static BiliClient.Order order(JSONObject response,int http)throws Exception{Fake f=new Fake(response);f.status=http;return new BiliClient(f,COOKIE).createOrder(new BiliClient.Offer(new JSONObject(),"ON_SALE",0,new double[4]));}
    static JSONObject reply(int code,JSONObject d)throws Exception{return new JSONObject().put("code",code).put("data",d);}
    public static void main(String[] args)throws Exception {
        check(PurchaseRules.cookies(COOKIE).size()==3,"own session parsed");
        check(!PurchaseRules.cookieHeader(PurchaseRules.cookies(COOKIE+"; tracking=ignored")).contains("tracking"),"unneeded cookies omitted");
        for(String c:new String[]{"",null,"SESSDATA=x; bili_jct=y","SESSDATA=x; bili_jct=y; DedeUserID=not-number",COOKIE+"\r\nX:bad",COOKIE+"; SESSDATA=another"})fails(()->PurchaseRules.cookies(c),"bad or ambiguous session rejected");
        for(String u:new String[]{"https://api.bilibili.com/x/web-interface/nav","https://api.bilibili.com/x/vip/order/create/activity"})check(PurchaseRules.apiDestination(u),"official API allowed");
        for(String u:new String[]{"http://api.bilibili.com/x","https://evil.example/x","https://api.bilibili.com.evil.example/x","https://user:pass@api.bilibili.com/x","https://api.bilibili.com:443/x","file:///x","https://api.bilibili.com\\@evil.example"})check(!PurchaseRules.apiDestination(u),"API destination blocked");
        check(PurchaseRules.officialWeb("https://passport.bilibili.com/login",false),"official login");
        check(PurchaseRules.officialWeb("https://static.geetest.com/a.js",true),"captcha resource");
        check(!PurchaseRules.officialWeb("https://static.geetest.com/a",false),"captcha not top-level login");
        for(String u:new String[]{"https://bilibili.com.evil.example/login","http://passport.bilibili.com/login","javascript:alert(1)","file:///data/private","https://evil.example"})check(!PurchaseRules.officialWeb(u,true),"web resource blocked");
        check(PurchaseRules.canRetry(PurchaseRules.Outcome.BUSY,1,10),"explicit busy permits bounded serial retry");
        check(PurchaseRules.canRetry(PurchaseRules.Outcome.NOT_OPEN,1,0),"not open retry");
        for(PurchaseRules.Outcome o:PurchaseRules.Outcome.values())if(o!=PurchaseRules.Outcome.BUSY&&o!=PurchaseRules.Outcome.NOT_OPEN)check(!PurchaseRules.canRetry(o,1,1),"terminal outcome never retries");
        check(PurchaseRules.canRetry(PurchaseRules.Outcome.BUSY,120,1),"legacy explicit busy continues serially");check(!PurchaseRules.canRetry(PurchaseRules.Outcome.BUSY,0,1),"invalid attempt blocked");
        check(PurchaseRules.retryInterval(2999)==100&&PurchaseRules.retryInterval(3000)==300&&PurchaseRules.retryInterval(10000)==1000,"legacy adaptive intervals");
        double[][] samples={{10000,100,200,0},{11000,1100,1200,0},{11000,1600,1700,0}};double[] fit=PurchaseRules.fit(samples);
        check(fit[0]==10100&&fit[1]==600,"truncated seconds interval intersection");
        fails(()->PurchaseRules.fit(new double[][]{{1,1,2,0}}),"too few clock samples");
        fails(()->PurchaseRules.fit(new double[][]{{10000,100,200,1},{11000,1100,1200,0},{11000,1600,1700,0}}),"cached sample rejected");
        fails(()->PurchaseRules.fit(new double[][]{{10000,100,200,0},{90000,1100,1200,0},{11000,1600,1700,0}}),"contradictory clock rejected");
        Fake account=new Fake(reply(0,new JSONObject().put("isLogin",true).put("mid",12345).put("uname","Fixture")));
        check(new BiliClient(account,COOKIE).account().getString("uid").equals("12345"),"official account matches current cookie");
        account.response=reply(0,new JSONObject().put("isLogin",true).put("mid",54321));fails(()->new BiliClient(account,COOKIE).account(),"different account rejected");
        account.response=reply(-101,new JSONObject());fails(()->new BiliClient(account,COOKIE).account(),"expired login rejected");
        Fake f=new Fake(offerBody(sku()));BiliClient api=new BiliClient(f,COOKIE);BiliClient.Offer offer=api.offer(2000);
        check(offer.payload.getString("csrf").equals("fixture-csrf"),"request bound to this user");check(offer.payload.getString("promo_token").equals("fixture-promo"),"fresh official promo used");
        check(offer.status.equals("ON_SALE")&&f.method.equals("GET")&&f.calls==1,"offer read never purchases");
        for(String field:new String[]{"sku_name","product_biz","sku_image"}){JSONObject s=sku().put(field,"changed");Fake bad=new Fake(offerBody(s));fails(()->new BiliClient(bad,COOKIE).offer(2000),"changed identity blocked");}
        for(String field:new String[]{"vip_sku_id","vip_price_id","buy_gift_id","buy_gift_type","card_type"}){JSONObject s=sku();s.getJSONObject("order_params").put(field,"changed");Fake bad=new Fake(offerBody(s));fails(()->new BiliClient(bad,COOKIE).offer(2000),"changed product parameter blocked");}
        for(double price:new double[]{17801,17800.5,16800,0}){Fake bad=new Fake(offerBody(sku().put("discount_price",price)));fails(()->new BiliClient(bad,COOKIE).offer(2000),"price mismatch blocked");}
        JSONObject nested=new JSONObject().put("payParams",payment());check(order(reply(0,nested),200).outcome==PurchaseRules.Outcome.CREATED,"valid order accepted");
        for(String alias:new String[]{"payParam","pay_param","pay_params"})check(order(reply(0,new JSONObject().put(alias,payment())),200).outcome==PurchaseRules.Outcome.CREATED,"payment envelope alias");
        check(order(reply(0,payment()),200).outcome==PurchaseRules.Outcome.CREATED,"direct envelope");
        for(double price:new double[]{16800,17800.5,17801})check(order(reply(0,payment().put("payAmount",price)),200).outcome==PurchaseRules.Outcome.UNKNOWN,"wrong amount with order never retries");
        check(order(reply(43055,new JSONObject().put("orderId","fixture-order")).put("message","活动太火爆，稍后再试"),200).outcome==PurchaseRules.Outcome.UNKNOWN,"order reference overrides busy");
        check(order(reply(43055,new JSONObject()).put("message","活动太火爆，稍后再试"),200).outcome==PurchaseRules.Outcome.BUSY,"exact known busy reply");
        check(order(reply(0,new JSONObject().put("fail_dialog",new JSONObject().put("content","尚未开售"))),200).outcome==PurchaseRules.Outcome.NOT_OPEN,"explicit not open");
        check(order(reply(0,new JSONObject().put("fail_dialog",new JSONObject().put("content","商品已售罄"))),200).outcome==PurchaseRules.Outcome.SOLD_OUT,"sold out");
        check(order(reply(-352,new JSONObject()),200).outcome==PurchaseRules.Outcome.BLOCKED,"verification block stops");
        check(order(reply(0,new JSONObject()),429).outcome==PurchaseRules.Outcome.BLOCKED,"rate limit stops");
        check(order(reply(0,new JSONObject()),302).outcome==PurchaseRules.Outcome.UNKNOWN,"redirect not replayed");
        check(order(new JSONObject(),200).outcome==PurchaseRules.Outcome.UNKNOWN,"missing code remains uncertain");
        Fake timeout=new Fake(new JSONObject());timeout.fail=true;fails(()->new BiliClient(timeout,COOKIE).createOrder(offer),"timeout propagates as uncertain");check(timeout.calls==1,"transport exception not retried by client");
        check(BiliClient.cashier(payment()).startsWith("https://pay.bilibili.com/pay-v2/cashier/cashier-desk?params="),"official cashier fixed destination");
        fails(()->BiliClient.cashier(payment().put("payAmount",1)),"invalid order never opens cashier");
        System.out.println("Purchase checks passed: "+passed+". Synthetic replies only; no network or purchases.");
    }
}
