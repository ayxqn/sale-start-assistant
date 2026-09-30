"""Create the distributable PDF from the same checked-in user/developer docs."""
from pathlib import Path
from xml.sax.saxutils import escape
import re
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.pagesizes import A4

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "dist" / "开售助手使用与开发手册.pdf"
FONT = Path("C:/Windows/Fonts/simsun.ttc")
pdfmetrics.registerFont(TTFont("Chinese", str(FONT), subfontIndex=0))
styles = {
    "title": ParagraphStyle("title",fontName="Chinese",fontSize=25,leading=34,textColor=colors.black,spaceAfter=18,wordWrap="CJK"),
    "h1": ParagraphStyle("h1",fontName="Chinese",fontSize=19,leading=27,textColor=colors.black,spaceAfter=14,wordWrap="CJK",keepWithNext=True),
    "h2": ParagraphStyle("h2",fontName="Chinese",fontSize=14,leading=21,textColor=colors.black,spaceBefore=14,spaceAfter=7,wordWrap="CJK",keepWithNext=True),
    "body": ParagraphStyle("body",fontName="Chinese",fontSize=11,leading=18,textColor=colors.HexColor("#202833"),spaceAfter=9,wordWrap="CJK",alignment=TA_LEFT),
    "small": ParagraphStyle("small",fontName="Chinese",fontSize=9,leading=15,textColor=colors.HexColor("#526274"),spaceAfter=7,wordWrap="CJK"),
}
def paragraph_text(raw):
    raw = re.sub(r"\[([^\]]+)\]\(([^)]+)\)",r"\1（\2）",raw)
    raw = raw.replace("**","").replace("`","")
    return escape(raw).replace("\n","<br/>")

story=[]
def p(s,style="body"):
    story.append(Paragraph(paragraph_text(s),styles[style]))

p("开售助手使用与开发手册","title")
p("Android 1.0.0 内测版　2026 年 9 月 30 日","small")
p("这份手册给第一次使用和第一次开发安卓应用的人。安装后，你可以设定开售时间，让助手到点进入 B 站官方活动页。登录、购买和付款由你在官方 App 内完成。")
p("先把功能边界说清楚","h2")
p("这是进场辅助工具，不是自动抢单器。它不自动下单、不自动付款，也不保证买到。只支持 Android 8.0 及以上，不支持 iPhone、iPad 或 iOS。")
p("旧版账号导入和内部接口购买没有进入这个成品。新应用独立重写，不预装开发者账号；原始实验备份仍留在所有者电脑，不在 GitHub 发布包内。")
p("目前可以下载什么","h2")
p("私有仓库 https://github.com/ayxqn/sale-start-assistant 。Releases 提供安卓 APK、本手册、文件校验值和源码包。私有仓库只有获授权的用户才能访问。")
p("当前验证情况","h2")
p("安装包已经构建并通过签名校验；30 项无网络核心规则检查通过，确认没有权限申请。尚未完成新 APK 的真机安装、界面、官方 App 跳转和不同手机系统验收，也未进行真实购买。这个版本标为内测，不能理解为已经全面验证的正式版本。")
p("阅读顺序","h2")
p("普通用户先读使用手册与隐私政策，再看安全说明。想了解开发过程，继续读从旧脚本到安卓应用、成品声明和测试验收。仓库里的文字版与这份 PDF 使用相同正文。")

sections=[("docs/USER_GUIDE.md","使用手册"),("docs/PRIVACY.md","隐私政策"),("SECURITY.md","安全说明"),("docs/DEVELOPMENT.md","从旧脚本到安卓应用"),("docs/TESTING.md","测试与验收")]
for filename,title in sections:
    story.append(PageBreak());p(title,"h1")
    blocks=(ROOT/filename).read_text(encoding="utf-8").split("\n\n")
    for block in blocks:
        block=block.strip()
        if not block or block.startswith("# "):continue
        if block.startswith("## "):p(block[3:],"h2")
        elif block.startswith("```"):continue
        else:
            lines=block.splitlines()
            if len(lines)>1 and all(re.match(r"^(?:- |\d+\.)",line) for line in lines):
                for line in lines:p(line)
            else:p(block)

def footer(canvas,doc):
    canvas.saveState();canvas.setFont("Chinese",8);canvas.setFillColor(colors.HexColor("#617186"))
    canvas.drawString(46,28,"开售助手 1.0.0 内测版")
    canvas.drawRightString(A4[0]-46,28,str(doc.page));canvas.restoreState()

OUT.parent.mkdir(parents=True,exist_ok=True)
doc=SimpleDocTemplate(str(OUT),pagesize=A4,rightMargin=46,leftMargin=46,topMargin=45,bottomMargin=48,
    title="开售助手使用与开发手册",author="ayxqn",subject="Android 应用使用 隐私 开发 发布与验收")
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print(str(OUT))
