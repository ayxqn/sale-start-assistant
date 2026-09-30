package io.github.ayxqn.salestart;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Local foreground countdown. No network client, login form, WebView or service. */
public final class MainActivity extends Activity {
    private final int BG = Color.rgb(10,20,36), PANEL = Color.rgb(17,35,59), BLUE = Color.rgb(88,166,255), TEXT = Color.rgb(230,239,251), MUTED = Color.rgb(159,179,204);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TimeZone zone = TimeZone.getTimeZone("Asia/Shanghai");
    private Calendar target;
    private EditText titleInput, linkInput;
    private TextView countdown, status, timeLabel;
    private Button timeButton, startButton, stopButton, openButton, demoButton;
    private SharedPreferences prefs;
    private boolean running = false, demo = false;
    private long targetMillis, anchorWall, anchorMono;
    private String activeLink;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("local_task", MODE_PRIVATE);
        target = Calendar.getInstance(zone);
        target.setTimeInMillis(prefs.getLong("target", System.currentTimeMillis()));
        if (target.getTimeInMillis() < System.currentTimeMillis()) {
            target = Calendar.getInstance(zone); target.add(Calendar.MINUTE, 5); target.set(Calendar.SECOND,0); target.set(Calendar.MILLISECOND,0);
        }
        render();
        if (!prefs.getBoolean("notice_v1", false)) handler.post(this::firstNotice);
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); if (border != 0) d.setStroke(dp(1), border); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(dp(3),1);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private void add(LinearLayout parent, View v, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(margin); parent.addView(v,p);
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(18),dp(18),dp(18),dp(18)); v.setBackground(background(PANEL, Color.rgb(35,60,89))); add(parent,v,16); return v;
    }
    private Button button(String value, boolean primary) {
        Button b=new Button(this); b.setText(value); b.setTextSize(16); b.setAllCaps(false); b.setTextColor(primary?BG:TEXT);
        b.setBackground(background(primary?BLUE:Color.rgb(26,48,77),0)); b.setPadding(dp(12),dp(12),dp(12),dp(12)); b.setMinHeight(dp(52)); return b;
    }
    private EditText input(String hint, String value, boolean url) {
        EditText e=new EditText(this); e.setText(value); e.setHint(hint); e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setTextSize(16);
        e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setBackground(background(BG,Color.rgb(40,68,101)));
        e.setInputType(url?android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI:android.text.InputType.TYPE_CLASS_TEXT);
        e.setMaxLines(url?4:2); e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(url?1024:60)}); return e;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        scroll.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(22),dp(22),dp(28)); scroll.addView(root);
        add(root,text("ANDROID · 本地运行",12,BLUE,true),0);
        add(root,text("开售助手",30,TEXT,true),6);
        add(root,text("把准备工作做在前面，到点进入官方活动页。",15,MUTED,false),6);
        LinearLayout hero=card(root); add(hero,text("距离开售",14,MUTED,false),0);
        countdown=text("尚未开始",38,TEXT,true); countdown.setContentDescription("开售倒计时"); add(hero,countdown,8);
        timeLabel=text("",14,BLUE,false); add(hero,timeLabel,8);
        status=text("填写活动链接，先试一次页面跳转。",14,MUTED,false); add(hero,status,12);
        LinearLayout form=card(root); add(form,text("这次要参加的活动",18,TEXT,true),0);
        titleInput=input("活动名称，可不填",prefs.getString("title",""),false); add(form,titleInput,12);
        add(form,text("官方活动页链接",14,MUTED,false),12);
        linkInput=input("https://www.bilibili.com/blackboard/…",prefs.getString("link",""),true); add(form,linkInput,8);
        timeButton=button("选择开售日期和时间",false); add(form,timeButton,14); timeButton.setOnClickListener(v->pickDate());
        add(form,text("所有时间按北京时间填写，秒数为 00。请先打开手机自动校时。",13,MUTED,false),8);
        openButton=button("先打开官方页，检查账号",false); add(root,openButton,16); openButton.setOnClickListener(v->manualOpen());
        startButton=button("开始倒计时",true); add(root,startButton,12); startButton.setOnClickListener(v->confirmStart());
        stopButton=button("停止倒计时",false); add(root,stopButton,12); stopButton.setEnabled(false); stopButton.setOnClickListener(v->stop("已停止。需要时可以重新开始。"));
        demoButton=button("10 秒演练，不联网",false); add(root,demoButton,12); demoButton.setOnClickListener(v->beginDemo());
        add(root,text("仅辅助进场，不自动下单或付款。请在官方 App 里核对账号、商品、金额和活动规则。",13,MUTED,false),16);
        LinearLayout more=new LinearLayout(this); more.setOrientation(LinearLayout.HORIZONTAL);
        Button help=button("使用与隐私",false),clear=button("清空本地设置",false);
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1); more.addView(help,half); LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,-2,1); second.leftMargin=dp(10);more.addView(clear,second); add(root,more,16);
        help.setOnClickListener(v->showHelp()); clear.setOnClickListener(v->confirmClear());
        add(root,text("1.0.0 · 仅 Android 8.0 及以上 · 不支持 iOS",12,MUTED,false),16);
        setContentView(scroll); updateTimeLabel(); scroll.requestApplyInsets();
    }

    private String format(long millis) { SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA);f.setTimeZone(zone);return f.format(millis); }
    private void updateTimeLabel() { timeLabel.setText("开售 " + format(target.getTimeInMillis()) + " 北京时间"); }
    private void firstNotice() {
        new AlertDialog.Builder(this).setTitle("先说清楚这几件事")
            .setMessage("这是个人开发的开售助手，与 B 站没有合作关系。\n\n它只在前台倒计时并打开官方活动页，不保证抢到，不提交订单，不代付。账号由你在官方 App 里登录，助手不读取密码或 Cookie。\n\n切到后台、锁屏或进程被结束时，倒计时会停止。活动是否允许辅助工具，请先看官方规则。")
            .setPositiveButton("明白了",(d,w)->prefs.edit().putBoolean("notice_v1",true).apply()).setNegativeButton("退出",(d,w)->finish()).setCancelable(false).show();
    }
    private void pickDate() {
        if(running)return;
        new DatePickerDialog(this,(v,y,m,d)->{
            target.set(Calendar.YEAR,y);target.set(Calendar.MONTH,m);target.set(Calendar.DAY_OF_MONTH,d);
            new TimePickerDialog(this,(tv,h,min)->{target.set(Calendar.HOUR_OF_DAY,h);target.set(Calendar.MINUTE,min);target.set(Calendar.SECOND,0);target.set(Calendar.MILLISECOND,0);updateTimeLabel();},target.get(Calendar.HOUR_OF_DAY),target.get(Calendar.MINUTE),true).show();
        },target.get(Calendar.YEAR),target.get(Calendar.MONTH),target.get(Calendar.DAY_OF_MONTH)).show();
    }
    private String checkedLink() { return SalePolicy.validateLink(linkInput.getText().toString()); }
    private void save(String link) { prefs.edit().putString("link",link).putString("title",titleInput.getText().toString().trim()).putLong("target",target.getTimeInMillis()).apply(); }
    private void problem(String message) { new AlertDialog.Builder(this).setTitle("需要调整一下").setMessage(message).setPositiveButton("知道了",null).show(); }
    private void manualOpen() {
        try { String link=checkedLink();save(link);stop("已离开助手，请在官方页面核对账号和商品。");openOfficial(link); }
        catch(IllegalArgumentException e) {problem(e.getMessage());}
    }
    private void confirmStart() {
        try {
            final String link=checkedLink(); SalePolicy.validateTime(target.getTimeInMillis(),System.currentTimeMillis());
            new AlertDialog.Builder(this).setTitle("确认后开始等待")
                .setMessage("开售时间："+format(target.getTimeInMillis())+" 北京时间\n\n到点只打开官方活动页一次，购买由你操作。请确认官方 App 里是自己的账号，活动允许使用此类辅助工具。\n\n保持本页在前台。切走、锁屏或修改手机时间，会停止等待。")
                .setPositiveButton("确认并开始",(d,w)->{try{SalePolicy.validateTime(target.getTimeInMillis(),System.currentTimeMillis());save(link);begin(target.getTimeInMillis(),link,false);}catch(IllegalArgumentException e){problem(e.getMessage());}})
                .setNegativeButton("先不开始",null).show();
        }catch(IllegalArgumentException e){problem(e.getMessage());}
    }
    private void beginDemo() { begin(System.currentTimeMillis()+10000L,null,true); }
    private void begin(long time,String link,boolean simulation) {
        stop("");demo=simulation;activeLink=link;targetMillis=time;anchorWall=System.currentTimeMillis();anchorMono=SystemClock.elapsedRealtime();running=true;
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(linkInput.getWindowToken(),0);linkInput.clearFocus();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setControls();status.setText(simulation?"正在演练，到点不会打开网页。":"正在等待，到点进入官方活动页。请勿切走。");handler.post(tick);
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!running)return;
        long mono=SystemClock.elapsedRealtime();
        if(SalePolicy.clockChanged(System.currentTimeMillis(),anchorWall,mono-anchorMono)){stop("手机时间发生变化，已停止。核对开售时间后重新开始。");return;}
        long left=SalePolicy.remaining(targetMillis,anchorWall,anchorMono,mono);
        long sec=(left+999)/1000;countdown.setText(String.format(Locale.ROOT,"%02d:%02d:%02d",sec/3600,sec/60%60,sec%60));
        if(left==0){boolean wasDemo=demo;String link=activeLink;stop(wasDemo?"演练完成，没有联网或打开网页。":"已到开售时间。请在官方页面核对并购买。");countdown.setText(wasDemo?"演练完成":"时间到了");
            if(!wasDemo)openOfficial(link);return;} handler.postDelayed(this,100);
    }};
    private void setControls(){startButton.setEnabled(!running);openButton.setEnabled(!running);timeButton.setEnabled(!running);demoButton.setEnabled(!running);titleInput.setEnabled(!running);linkInput.setEnabled(!running);stopButton.setEnabled(running);}
    private void stop(String message){running=false;handler.removeCallbacks(tick);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(status!=null&&!message.isEmpty())status.setText(message);if(startButton!=null)setControls();}
    private void openOfficial(String link) {
        try {
            String safe=SalePolicy.validateLink(link);
            Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("bilibili://browser?url="+Uri.encode(safe)));i.setPackage("tv.danmaku.bili");startActivity(i);
            status.setText("已请求 B 站打开活动页。请检查实际页面是否正确。");
        }catch(ActivityNotFoundException | SecurityException e){
            new AlertDialog.Builder(this).setTitle("没有成功打开 B 站")
                .setMessage("请确认安装了官方 B 站 App。国际版、HD 版或新版跳转方式可能不同。你可以在浏览器查看活动，再自行进入官方 App。")
                .setPositiveButton("在浏览器查看",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(SalePolicy.validateLink(link))));}catch(Exception ignored){problem("浏览器也未能打开，请手动访问官方活动页。");}})
                .setNegativeButton("取消",null).show();
        }catch(IllegalArgumentException e){problem(e.getMessage());}
    }
    private void showHelp(){
        if(running)stop("打开说明，倒计时已停止。");
        new AlertDialog.Builder(this).setTitle("使用与隐私")
            .setMessage("1. 安装官方 B 站 App，登录自己的账号。\n2. 粘贴 /blackboard/ 官方活动链接，去掉分享参数。\n3. 先打开活动页，核对账号、资格、商品和金额。\n4. 返回助手，在开售前两小时内设定北京时间，开始等待。\n5. 保持助手前台、亮屏、手机解锁，到点进入官方页，手动下单和付款。\n\n助手不申请联网、无障碍、截图、存储、通讯录或定位权限；没有广告或统计 SDK。只在本机保存名称、活动链接、开售时间和已读说明。\n\n切到后台或锁屏会停止，不会自动恢复任务。通知、来电和系统清理可能中断等待。开售时间由你填写，没有服务器校时，也不保证毫秒准确。\n\n卸载或清空本地设置会删除助手的数据，不会退出 B 站账号或取消订单。官方 App、浏览器及支付平台按各自隐私政策处理数据。\n\n仅 Android 8.0 及以上，不支持 iPhone、iPad 或 iOS。")
            .setPositiveButton("知道了",null).show();
    }
    private void confirmClear(){new AlertDialog.Builder(this).setTitle("清空本地设置？").setMessage("会删除活动名称、链接、时间和已读说明，停止当前倒计时。不会删除官方 App 的账号、订单或付款记录。")
        .setPositiveButton("清空",(d,w)->{stop("已清空本地设置。");prefs.edit().clear().commit();titleInput.setText("");linkInput.setText("");target=Calendar.getInstance(zone);target.add(Calendar.MINUTE,5);target.set(Calendar.SECOND,0);target.set(Calendar.MILLISECOND,0);updateTimeLabel();countdown.setText("尚未开始");firstNotice();})
        .setNegativeButton("保留",null).show();}
    @Override protected void onPause(){if(running)stop("已离开前台或锁屏，倒计时停止。返回后请重新开始。");super.onPause();}
    @Override protected void onDestroy(){stop("");super.onDestroy();}
}
