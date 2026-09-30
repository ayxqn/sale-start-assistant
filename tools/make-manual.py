"""Render the checked-in v2 user, privacy and engineering documentation as A4 PDF."""
from pathlib import Path
from xml.sax.saxutils import escape
import re
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Image, CondPageBreak
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.pagesizes import A4
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"dist"/"sale-start-assistant-manual-2.0-zh-CN.pdf"
pdfmetrics.registerFont(TTFont("Chinese","C:/Windows/Fonts/simsun.ttc",subfontIndex=0))
BLUE=colors.HexColor("#326FC6")
styles={
"title":ParagraphStyle("title",fontName="Chinese",fontSize=27,leading=38,textColor=BLUE,spaceAfter=16,wordWrap="CJK"),
"h1":ParagraphStyle("h1",fontName="Chinese",fontSize=21,leading=30,textColor=BLUE,spaceAfter=16,wordWrap="CJK",keepWithNext=True),
"h2":ParagraphStyle("h2",fontName="Chinese",fontSize=14,leading=22,textColor=BLUE,spaceBefore=10,spaceAfter=7,wordWrap="CJK",keepWithNext=True),
"body":ParagraphStyle("body",fontName="Chinese",fontSize=10.5,leading=17.5,textColor=colors.HexColor("#253D59"),spaceAfter=8,wordWrap="CJK"),
"small":ParagraphStyle("small",fontName="Chinese",fontSize=9,leading=14,textColor=colors.HexColor("#647B98"),spaceAfter=7,wordWrap="CJK")
}
def clean(s):
    s=re.sub(r"\[([^\]]+)\]\(([^)]+)\)",r"\1（\2）",s)
    return escape(s.replace("**","").replace(chr(96),"")).replace("\n","<br/>")
story=[]
def p(s,kind="body"):story.append(Paragraph(clean(s),styles[kind]))
p("开售小助手\n使用与开发手册","title")
p("Android 2.0.0 预发布版 · 2026 年 9 月 30 日","small")
story.append(Image(str(ROOT/"app/src/main/res/drawable-nodpi/twins_banner.png"),width=499,height=166.33))
story.append(Spacer(1,18))
p("登录自己的账号，等待官方开售；订单交给程序准备，付款留给本人确认。")
p("先看这几句话","h2")
p("这是独立安卓 APK，不需要 AutoJs6 或电脑。2.0 已加入官方登录、本机加密、活动校时、后台创建订单和报错日志；不是只打开页面的 1.0 旧版。")
p("新 APK 已验证登录、手机安装、界面和短时后台演练，但真实开售下单与收银台全链路仍待验证。旧脚本成功记录不能代替新 APK 的实测。")
p("只适配当前 178 元套餐。与 B 站无官方合作，不保证库存或资格；付款及权益以官方记录为准。")
p("怎么读","h2")
p("先读使用手册与隐私政策；想学习开发，再读开发全过程和测试记录。遇到异常，优先查官方订单，别急着清空数据重新启动。")
p("公开项目与下载：https://github.com/ayxqn/sale-start-assistant","small")
sections=[("docs/USER_GUIDE.md","使用手册"),("docs/PRIVACY.md","隐私与本机数据"),("SECURITY.md","安全与发布签名"),("docs/DEVELOPMENT.md","开发全过程"),("docs/TESTING.md","验证记录与下一步")]
for index,(filename,title) in enumerate(sections):
    story.append(PageBreak() if index==0 else CondPageBreak(230))
    if index:story.append(Spacer(1,22))
    p(title,"h1")
    for block in (ROOT/filename).read_text(encoding="utf-8").split("\n\n"):
        block=block.strip()
        if not block or block.startswith("# "):continue
        if block.startswith("## "):p(block[3:],"h2")
        elif block.startswith(chr(96)*3):continue
        else:
            lines=block.splitlines()
            if len(lines)>1 and all(re.match(r"^(?:- |\d+\.)",line) for line in lines):
                for line in lines:p(line)
            else:p(block)
def footer(c,d):
    c.saveState();c.setStrokeColor(colors.HexColor("#D6E5F8"));c.line(48,40,A4[0]-48,40)
    c.setFont("Chinese",8);c.setFillColor(colors.HexColor("#647B98"));c.drawString(48,27,"开售小助手 2.0 · 使用 / 隐私 / 开发")
    c.drawRightString(A4[0]-48,27,str(d.page));c.restoreState()
OUT.parent.mkdir(parents=True,exist_ok=True)
SimpleDocTemplate(str(OUT),pagesize=A4,rightMargin=48,leftMargin=48,topMargin=44,bottomMargin=56,title="开售小助手 2.0 使用与开发手册",author="ayxqn").build(story,onFirstPage=footer,onLaterPages=footer)
print(OUT)
