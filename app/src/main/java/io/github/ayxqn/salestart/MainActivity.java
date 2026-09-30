package io.github.ayxqn.salestart;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native home, official login page, local account and order management. */
public final class MainActivity extends Activity {
    private final int BG=Color.rgb(243,248,255),PANEL=Color.WHITE,BLUE=Color.rgb(50,111,198),TEXT=Color.rgb(38,61,91),MUTED=Color.rgb(88,112,142),PALE=Color.rgb(230,240,255);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SecureStore vault; private TextView status,countdown,account,battery;
    private Button start,stop,login,logout; private WebView web;
    private boolean resumed,checking,opening,noticeSeen; private int loginGeneration;
    private long openedPaymentTime; private JSONObject session; private String loadProblem;
    private Typeface round;
    private void roundText(View v){if(v instanceof TextView){TextView t=(TextView)v;boolean bold=t.getTypeface()!=null&&t.getTypeface().isBold();t.setTypeface(rounded(),bold?Typeface.BOLD:Typeface.NORMAL);}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)roundText(g.getChildAt(i));}}
    private final class RoundedDialogBuilder extends AlertDialog.Builder{RoundedDialogBuilder(){super(MainActivity.this);}@Override public AlertDialog show(){AlertDialog dialog=super.show();roundText(dialog.getWindow().getDecorView());return dialog;}}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d;}
    private Typeface rounded(){if(round==null)try{round=Typeface.createFromAsset(getAssets(),"fonts/ChillRoundF.ttf");}catch(Exception e){round=Typeface.create("sans-serif",Typeface.NORMAL);}return round;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);t.setTypeface(rounded(),bold?Typeface.BOLD:Typeface.NORMAL);return t;}
    private void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);p.addView(v,lp);}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTypeface(rounded());b.setTextSize(15);int[][] states={new int[]{-android.R.attr.state_enabled},new int[]{}};b.setTextColor(new android.content.res.ColorStateList(states,new int[]{Color.rgb(121,139,160),primary?Color.WHITE:BLUE}));android.graphics.drawable.StateListDrawable fills=new android.graphics.drawable.StateListDrawable();fills.addState(states[0],bg(Color.rgb(235,240,247)));fills.addState(states[1],bg(primary?BLUE:PALE));b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x226796D9),fills,null));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setMinHeight(dp(48));b.setStateListAnimator(null);return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(LinearLayout p){LinearLayout l=column();l.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable d=bg(PANEL);d.setStroke(dp(1),Color.rgb(219,232,249));l.setBackground(d);add(p,l,12);return l;}
    private TextView tag(String label,int color){TextView t=text(label,11,color,true);t.setPadding(dp(9),dp(4),dp(9),dp(4));t.setBackground(bg(PALE));return t;}
    private void pair(LinearLayout root,View a,View b,int top){LinearLayout row=new LinearLayout(this);row.addView(a,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(10);row.addView(b,p);add(root,row,top);}
    private void insets(View root){root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});root.requestApplyInsets();}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);vault=new SecureStore(this);Diagnostics.init(this);PurchaseService.channels(this);
        openedPaymentTime=getPreferences(MODE_PRIVATE).getLong("cashier_seen",0);loadSession();render();
        noticeSeen=getPreferences(MODE_PRIVATE).getBoolean("notice_v2",false);if(!noticeSeen)handler.post(this::notice);
    }
    private void loadSession(){try{session=vault.read("session");loadProblem=null;}catch(Exception e){session=null;loadProblem="原登录数据无法解密，请清空本应用数据后重新登录。";Diagnostics.event("storage","DECRYPT_FAILED","本地加密数据无法读取，请清空后重新登录。");}}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=column();root.setPadding(dp(20),dp(12),dp(20),dp(28));scroll.addView(root);
        add(root,text("开售小助手",29,TEXT,true),0);
        add(root,text("把等待交给我，把选择留给你。",13,MUTED,false),4);
        int artwork=getResources().getIdentifier("twins_banner","drawable",getPackageName());
        if(artwork!=0){ImageView art=new ImageView(this);art.setImageResource(artwork);art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setContentDescription("22 娘和 33 娘主题同人插画，浅蓝贴纸与星星涂鸦");art.setBackground(bg(PALE));art.setClipToOutline(true);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(116));ap.topMargin=dp(12);root.addView(art,ap);}
        LinearLayout hero=card(root);LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heading.addView(text("超大年度套餐",19,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));heading.addView(text("¥178",26,BLUE,true));add(hero,heading,0);
        LinearLayout tags=new LinearLayout(this);tags.addView(tag("萌节十周年",BLUE));TextView local=tag("本机处理",BLUE);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2);tp.leftMargin=dp(6);tags.addView(local,tp);add(hero,tags,5);
        countdown=text("等待启动",29,TEXT,true);add(hero,countdown,12);status=text("登录自己的账号，设置后台运行后点启动。",13,MUTED,false);add(hero,status,6);
        LinearLayout ac=card(root);account=text("",15,TEXT,true);add(ac,account,0);
        login=button("登录 B 站",false);add(ac,login,9);login.setOnClickListener(v->showLogin());
        battery=text("",12,MUTED,false);add(root,battery,12);Button setup=button("设置后台运行与通知",false);add(root,setup,7);setup.setOnClickListener(v->backgroundSettings());
        start=button("启动 · 到点创建 178 元订单",true);add(root,start,12);start.setOnClickListener(v->startTask(false));
        stop=button("停止任务",false);stop.setOnClickListener(v->stopTask());Button order=button("订单 / 去付款",false);order.setOnClickListener(v->openCashier());pair(root,stop,order,9);
        Button logs=button("结果与报错日志",false);logs.setOnClickListener(v->showLogs());Button help=button("使用说明",false);help.setOnClickListener(v->help());pair(root,logs,help,9);
        logout=button("账号与数据",false);add(root,logout,9);logout.setOnClickListener(v->dataMenu());
        add(root,text("✧ 下单交给助手，付款由你确认。\n记得核对官方订单哦。",12,MUTED,false),14);
        setContentView(scroll);insets(scroll);refresh();
    }
    private void notice(){new RoundedDialogBuilder().setTitle("使用前，请读这几句话")
        .setMessage("本应用是个人开发的 B 站活动助手。点“启动”会使用你登录的账号，在官方开售时间创建 178 元订单；付款仍由你本人确认。\n\n登录会话、必要订单参数和防重复记录在本机加密保存，不发给开发者。官方登录页面会处理你输入的登录信息；助手不收集密码、短信、银行卡或支付密码。登录和下单需要联网。\n\n只适配当前列出的活动，不保证库存或资格。平台限制、系统清理和网络异常可能导致失败。请先确认官方活动允许此类工具。")
        .setPositiveButton("明白并使用",(d,w)->{getPreferences(MODE_PRIVATE).edit().putBoolean("notice_v2",true).apply();noticeSeen=true;})
        .setNegativeButton("退出",(d,w)->finish()).setCancelable(false).show();}
    private void info(String title,String message){if(!isFinishing())new RoundedDialogBuilder().setTitle(title).setMessage(message).setPositiveButton("知道了",null).show();}
    private String mask(String uid){return uid.length()>3?"尾号 "+uid.substring(uid.length()-3):"已核对";}
    private void refresh(){
        if(web!=null||status==null)return;boolean running=PurchaseService.RUNNING.get();SharedPreferences p=getSharedPreferences("task_v2",MODE_PRIVATE);
        account.setText(session==null?"尚未登录":session.optString("name","B 站用户")+" · "+mask(session.optString("uid")));
        login.setText(session==null?"登录 B 站":"重新登录 / 换账号");login.setEnabled(!running&&!checking);logout.setEnabled(!running&&!checking);start.setEnabled(!running&&!checking&&session!=null);stop.setEnabled(running);
        boolean unrestricted=((PowerManager)getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());boolean notifications=getSystemService(NotificationManager.class).areNotificationsEnabled();
        battery.setText("后台电量："+(unrestricted?"已允许不受限制":"建议设为不受限制")+"　通知："+(notifications?"已开启":"未开启"));
        String state=p.getString("state","idle"),message=p.getString("message","登录自己的账号，设置后台运行后点启动。");
        if(!running&&Arrays.asList("waiting","calibrating","checking","ready","retrying","demo").contains(state)){
            message="上一次后台任务已中断；不会自动补单。请先核对官方订单。";
            PurchaseService.state(this,"interrupted",message,0,0);Diagnostics.event("service","PROCESS_INTERRUPTED",message);
        }
        status.setText(loadProblem==null?message:loadProblem);long target=p.getLong("target",0),left=Math.max(0,target-System.currentTimeMillis()-p.getLong("offset",0));
        if(running&&target>0)countdown.setText(String.format(Locale.ROOT,"%02d:%02d:%02d",left/3600000,left/60000%60,left/1000%60));else countdown.setText(running?"正在准备":"created".equals(state)?"订单已创建":"demo_done".equals(state)?"演练完成":Arrays.asList("failed","stopped","interrupted").contains(state)?"本次已停止":"等待启动");
        if(resumed&&!opening&&"created".equals(state)&&p.getLong("updated",0)>openedPaymentTime){
            openedPaymentTime=p.getLong("updated",0);getPreferences(MODE_PRIVATE).edit().putLong("cashier_seen",openedPaymentTime).apply();handler.post(this::openCashier);
        }
    }
    private final Runnable tick=new Runnable(){public void run(){if(resumed){refresh();handler.postDelayed(this,500);}}};
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(tick);handler.post(tick);if("open_cashier".equals(getIntent().getAction())){getIntent().setAction(null);handler.post(this::openCashier);}}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if("open_cashier".equals(i.getAction())){i.setAction(null);openCashier();}}
    private void stopTask(){startService(new Intent(this,PurchaseService.class).setAction(PurchaseService.STOP));handler.postDelayed(this::refresh,100);}
    private void startTask(boolean demo){
        if(!noticeSeen||checking||PurchaseService.RUNNING.get())return;if(!demo&&session==null){showLogin();return;}
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);return;}
        if(!getSystemService(NotificationManager.class).areNotificationsEnabled()){info("请开启通知","后台创建订单后需要用通知提醒你付款。请在系统设置中开启开售助手的通知。");return;}
        try{startForegroundService(new Intent(this,PurchaseService.class).setAction(demo?PurchaseService.DEMO:PurchaseService.START));handler.postDelayed(this::refresh,150);}catch(Exception e){info("未启动","系统没有允许启动后台任务，请检查后台运行设置。");}
    }
    private void backgroundSettings(){new RoundedDialogBuilder().setTitle("一次设置，之后直接启动")
        .setMessage("允许通知，订单创建后才能提醒你。把电量限制设为“不受限制”；小米等手机还可在应用详情中允许后台自启动，并在最近任务里锁定本应用。\n\n等待期间可能增加耗电，建议开售前几分钟启动。不要强行停止应用或使用清理工具。")
        .setPositiveButton("设置电量",(d,w)->{try{startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}})
        .setNeutralButton("设置通知",(d,w)->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));})
        .setNegativeButton("稍后",null).show();}
    private void showLogin(){if(PurchaseService.RUNNING.get()||checking)return;
        new RoundedDialogBuilder().setTitle("登录你自己的 B 站账号")
            .setMessage("接下来打开 B 站官方登录页。请本人输入登录信息并完成验证码。登录成功后，助手会核对账号，并在本机加密保存用于下单的会话。\n\n切换账号会退出本应用原账号并清除其待付款入口；官方订单不会被取消。")
            .setPositiveButton("打开官方登录",(d,w)->beginLogin()).setNegativeButton("取消",null).show();
    }
    private void beginLogin(){
        try{vault.logout();session=null;}catch(Exception e){info("无法登录","旧账号退出失败，请重试。");return;}
        checking=true;final int generation=++loginGeneration;
        CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();if(generation==loginGeneration&&!isFinishing())createLoginWeb();});
    }
    private void createLoginWeb(){
        checking=false;getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);LinearLayout root=column();root.setBackgroundColor(BG);
        TextView title=text("B 站官方登录 · passport.bilibili.com",15,TEXT,true);title.setPadding(dp(15),dp(12),dp(15),dp(8));add(root,title,0);
        TextView tip=text("请本人完成短信或密码登录。完成后点下方核对。",12,MUTED,false);tip.setPadding(dp(15),0,dp(15),dp(8));add(root,tip,0);
        web=new WebView(this);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSaveFormData(false);s.setCacheMode(WebSettings.LOAD_NO_CACHE);s.setMediaPlaybackRequiresUserGesture(true);WebView.setWebContentsDebuggingEnabled(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
            @Override public boolean onShowFileChooser(WebView v,android.webkit.ValueCallback<Uri[]> c,FileChooserParams p){c.onReceiveValue(null);return true;}
        });
        web.setDownloadListener((a,b,c,d,e)->info("未下载","登录页面不需要下载其他安装包。"));
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !PurchaseRules.officialWeb(r.getUrl().toString(),false);}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(PurchaseRules.officialWeb(u,!r.isForMainFrame())||u.startsWith("data:"))return null;return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();runOnUiThread(()->info("连接未通过验证","请检查手机时间和网络，不要忽略证书错误。"));}
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));Button accept=button("已完成登录，核对并保存",true);add(root,accept,6);accept.setOnClickListener(v->saveLogin());
        Button back=button("返回，不保存",false);add(root,back,6);back.setOnClickListener(v->closeLogin());setContentView(root);insets(root);
        web.loadUrl("https://passport.bilibili.com/h5-app/passport/login");Diagnostics.event("login","PAGE_OPENED","用户打开官方登录页面；不记录输入或 Cookie。");
    }
    private void saveLogin(){
        if(web==null||checking)return;String raw=CookieManager.getInstance().getCookie("https://www.bilibili.com/");final String cookie;
        try{cookie=PurchaseRules.cookieHeader(PurchaseRules.cookies(raw));}catch(Exception e){info("还没有完成登录",e.getMessage());return;}
        checking=true;final int generation=loginGeneration;
        worker.execute(()->{OfficialTransport transport=new OfficialTransport();try{
            BiliClient api=new BiliClient(transport,cookie);JSONObject verified=api.account();verified.put("cookie",cookie);verified.put("saved_at",System.currentTimeMillis());
            runOnUiThread(()->{if(generation!=loginGeneration||web==null)return;try{vault.write("session",verified);session=verified;Diagnostics.event("login","SAVED","官方核对通过，会话已在本机加密保存。");closeLogin();info("账号已保存","已核对你的账号。设置后台运行后，即可点击启动。");}catch(Exception e){checking=false;info("未保存","本机加密保存失败，请重试。");}});
        }catch(Exception e){runOnUiThread(()->{if(generation==loginGeneration){checking=false;info("账号核对未通过",e instanceof BiliClient.Problem?e.getMessage():"网络或登录状态暂不可用，请稍后再试。");Diagnostics.event("login","VERIFY_FAILED","官方账号核对未通过；未保存会话。");}});}finally{transport.cancel();}});
    }
    private void closeLogin(){
        loginGeneration++;checking=false;WebView old=web;web=null;if(old!=null){old.stopLoading();old.loadUrl("about:blank");old.clearHistory();old.clearCache(true);old.clearFormData();if(old.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)old.getParent()).removeView(old);old.destroy();}
        CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());WebStorage.getInstance().deleteAllData();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);render();
    }
    private void openCashier(){
        if(opening||web!=null)return;opening=true;
        try{
            JSONObject saved=vault.read("payment");if(saved==null||session==null||!session.optString("uid").equals(saved.optString("uid"))){info("没有可打开的订单","还没有为当前账号保存的 178 元订单。请在 B 站官方订单页核对已有订单。");return;}
            String url=BiliClient.cashier(saved.getJSONObject("params"));Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("bilibili://browser?url="+Uri.encode(url))).setPackage("tv.danmaku.bili");
            try{startActivity(i);Diagnostics.event("cashier","OPEN_REQUESTED","已请求官方 App 打开收银台，需本人核对并付款。");}
            catch(ActivityNotFoundException e){new RoundedDialogBuilder().setTitle("需要官方收银台").setMessage("未能打开大陆版 B 站 App。可在浏览器打开官方收银台，请核对账号、商品与金额后自行付款。")
                .setPositiveButton("打开官方网页",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){info("无法打开","请安装官方 B 站 App，或直接在官方订单页查找订单。");}}).setNegativeButton("稍后",null).show();}
        }catch(Exception e){info("订单暂不可用","本地订单无法读取或核对，请到 B 站官方订单页处理。");Diagnostics.event("cashier","UNAVAILABLE","本地订单无法读取或校验。");}finally{opening=false;}
    }
    private void showLogs(){
        ScrollView s=new ScrollView(this);TextView t=text(Diagnostics.read(),12,TEXT,false);t.setTextIsSelectable(true);t.setPadding(dp(16),dp(12),dp(16),dp(12));s.addView(t);
        new RoundedDialogBuilder().setTitle("结果与报错日志").setView(s).setPositiveButton("关闭",null)
            .setNeutralButton("导出日志",(d,w)->{try{
                Diagnostics.export();Uri u=Uri.parse("content://io.github.ayxqn.salestart.diagnostics/diagnostics.txt");
                Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("诊断日志",u));startActivity(Intent.createChooser(share,"选择保存或发送位置"));
            }catch(Exception e){info("未能导出","没有可用的分享应用，或本地记录暂不可读。");}})
            .setNegativeButton("清空日志",(d,w)->{if(PurchaseService.RUNNING.get()){info("任务正在运行","请先停止任务，再清空日志。");return;}new RoundedDialogBuilder().setMessage("只删除本应用的诊断日志，不删除登录和订单数据。").setPositiveButton("清空",(a,b)->Diagnostics.clear()).setNegativeButton("取消",null).show();}).show();
    }
    private void dataMenu(){
        if(PurchaseService.RUNNING.get())return;new RoundedDialogBuilder().setTitle("账号与本地数据")
            .setItems(new String[]{"退出当前账号","清空本应用全部数据","隐私和权限说明","关于与开源许可"},(d,which)->{
                if(which==2){privacy();return;}if(which==3){licenses();return;}new RoundedDialogBuilder().setTitle(which==0?"退出当前账号？":"清空全部数据？")
                    .setMessage(which==0?"删除登录会话和本地付款入口，保留加密的防重复提交记录。不会退出手机上 B 站 App 的账号，也不会取消订单。":"删除登录、订单入口、诊断日志和防重复记录。官方订单不会取消；清空后不要在同一场重复启动，请先到 B 站核对订单。")
                    .setPositiveButton("确认",(a,b)->{try{if(which==0)vault.logout();else{vault.clear();Diagnostics.clear();getSharedPreferences("task_v2",MODE_PRIVATE).edit().clear().commit();}session=null;loadProblem=null;CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());WebStorage.getInstance().deleteAllData();refresh();}catch(Exception e){info("操作未完成","本地数据处理失败，请重试。");}})
                    .setNegativeButton("取消",null).show();
            }).show();
    }
    private void licenses(){
        StringBuilder content=new StringBuilder("开售助手 2.0.0 内测版\n仅 Android 8.0 及以上，不支持 iOS。\n个人开发，与 B 站无官方合作。\n新 APK 尚未完成真实开售下单及收银台全链路验证，不保证抢到。\n\n");
        for(String name:new String[]{"NOTICE.txt","ChillRound-OFL.txt","ChillRound-upstream-LICENSE.txt","Apache-2.0.txt"})try(java.io.InputStream in=getAssets().open("licenses/"+name)){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);content.append(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8)).append("\n\n");}catch(Exception ignored){}
        ScrollView scroll=new ScrollView(this);TextView t=text(content.toString(),13,TEXT,false);t.setTextIsSelectable(true);t.setPadding(dp(18),dp(12),dp(18),dp(12));scroll.addView(t);new RoundedDialogBuilder().setTitle("关于与开源许可").setView(scroll).setPositiveButton("知道了",null).show();
    }
    private void privacy(){info("数据只为当前账号下单使用",
        "本机处理：登录会话、账号标识、昵称、订单号、金额、收银台签名和防重复记录。它们使用 Android Keystore 和 AES-GCM 加密保存。登录页临时数据由本机 WebView 处理，退出登录页后清理。\n\n联网对象：B 站官方登录、活动和订单服务；登录页面可能加载官方静态资源及验证码服务。没有开发者服务器、广告或统计上报。\n\n付款在官方 App 或浏览器中完成；助手不收集支付密码、银行卡、短信、指纹或面部数据，不会代你确认付款。\n\n本机诊断日志记录时间、阶段、错误码和脱敏结果，不记录登录凭据、完整订单号或支付签名。只有你点导出并选择接收应用才会分享。\n\n权限：联网、前台服务、保持处理器唤醒、通知和申请电量不受限制。没有无障碍、截图、通讯录、定位或外部存储权限。\n\n含 OkHttp、Okio、Kotlin 等开源组件。经过检查不等于零风险；请从项目发布页下载签名安装包。完整说明及许可见仓库手册。");
    }
    private void help(){new RoundedDialogBuilder().setTitle("怎么使用")
        .setMessage("1. 在本应用的 B 站官方页面登录并保存。\n2. 开启通知，把电量设为不受限制。\n3. 点启动，程序自动读取下一场时间、校时、核对套餐并创建订单，可以放到后台。\n4. 收到订单通知后，点通知进入官方收银台，自行选支付方式并付款。\n\n不用 AutoJs6，不用连电脑，不用填写商品编号。只适配这一款 178 元活动套餐；活动结束或数据改变时会停止。系统强行结束任务后不会自动补单。已发送请求不能撤回，结果不明时请核对官方订单。")
        .setPositiveButton("知道了",null).setNeutralButton("20 秒后台演练",(d,w)->startTask(true)).setNegativeButton("隐私与权限",(d,w)->privacy()).show();}
    @Override public void onBackPressed(){if(web!=null)closeLogin();else super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);loginGeneration++;worker.shutdownNow();if(web!=null){web.stopLoading();web.destroy();web=null;CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());WebStorage.getInstance().deleteAllData();}super.onDestroy();}
}
