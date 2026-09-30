package io.github.ayxqn.salestart;

public final class SalePolicyTest {
    private static int cases;
    private static void check(boolean b){cases++;if(!b)throw new AssertionError("case "+cases);}
    private static void reject(String s){try{SalePolicy.validateLink(s);throw new AssertionError("Unsafe link accepted");}catch(IllegalArgumentException expected){cases++;}}
    public static void main(String[] args){
        check(SalePolicy.validateLink(" https://www.bilibili.com/blackboard/test.html ").equals("https://www.bilibili.com/blackboard/test.html"));
        check(SalePolicy.validateLink("https://m.bilibili.com/blackboard/era/demo.html").startsWith("https://m.bilibili.com/"));
        for(String s:new String[]{"http://www.bilibili.com/blackboard/x", "https://evil.example/blackboard/x", "https://www.bilibili.com.evil.example/blackboard/x", "https://user:secret@www.bilibili.com/blackboard/x", "https://www.bilibili.com:443/blackboard/x", "bilibili://browser?url=x", "file:///tmp/test", "https://www.bilibili.com/blackboard/x?SESSDATA=demo", "https://www.bilibili.com/blackboard/x#private", "https://www.bilibili.com/blackboard/../login", "https://www.bilibili.com/blackboard/%2e%2e/login", "https://www.bilibili.com/blackboard//x", "https://www.bilibili.com/video/x", "",null})reject(s);
        long now=1000000L;SalePolicy.validateTime(now+1000,now);cases++;SalePolicy.validateTime(now+SalePolicy.MAX_WAIT_MS,now);cases++;
        for(long t:new long[]{now-1,now,now+999,now+SalePolicy.MAX_WAIT_MS+1}){try{SalePolicy.validateTime(t,now);throw new AssertionError("bad time");}catch(IllegalArgumentException expected){cases++;}}
        check(SalePolicy.remaining(11000,1000,50,1050)==9000);check(SalePolicy.remaining(11000,1000,50,10050)==0);check(SalePolicy.remaining(11000,1000,50,11050)==0);
        check(!SalePolicy.clockChanged(5000,1000,4000));check(!SalePolicy.clockChanged(7000,1000,4000));check(SalePolicy.clockChanged(7001,1000,4000));check(SalePolicy.clockChanged(2999,1000,4000));
        System.out.println(cases+" policy checks passed; no network or purchases.");
    }
}
