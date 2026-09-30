# 安装与下载说明

[项目首页](../README.md) · [文档导航](README.md) · [使用手册](USER_GUIDE.md) · [安全核实说明](SAFETY.md)

**当前 APK 不包含旧 V2.4 的接口抢购流程，不是用户要求的完整功能版本。下载前先看 [旧版功能对照](LEGACY_COMPARISON.md)。完整功能目标尚未完成，不把本版作为该目标的最终安装包交付。**

## 只想用当前进场 App，下载这一个

**[sale-start-assistant-1.0.0.apk](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-1.0.0.apk)**

这是 1.0.0 内测版的安卓安装包，支持 Android 8.0 及以上。下载、安装后独立运行，不需要电脑、USB、ADB、Python、SDK、服务器、源码或外部配置文件，也不用 AutoJs6、Root、无障碍服务或截图权限。

仓库与发布页公开可访问，正常下载不需要仓库授权或 GitHub 登录。若链接无法打开，先检查网络和准确文件名，不要转而安装来源不明的同名文件。

iPhone、iPad 和 iOS 不能安装。B 站国际版、HD 版、其他购物平台和不同手机系统的实际跳转没有完成验证，不承诺兼容。

## 安装步骤

1. 打开 [1.0.0 内测发布页](https://github.com/ayxqn/sale-start-assistant/releases/tag/v1.0.0)，选择文件名完全一致的 APK。不要下载 GitHub 自动生成的 `Source code (zip)` 来安装。
2. 下载完成后，在安卓手机上点开 APK。若系统要求允许安装，只给当前下载或文件管理应用临时授权，安装后可以关闭此来源权限。不要关闭系统防护或给所有来源放开权限。
3. 安装后打开“开售助手”，先读中文功能与隐私说明，再点“10 秒演练，不联网”。这一步不需要账号、活动链接或任何其他配置。
4. 正式使用时，在大陆版官方 B 站 App 登录自己的账号。回到助手，填写公开活动链接和北京时间，先试打开页面，再确认开始倒计时。
5. 保持助手前台、手机解锁并亮屏。到点进入官方页后，你自己核对商品、金额、资格，再下单和付款。

切到后台、锁屏或被系统结束后，倒计时会停止，不会自动恢复。链接与开售时间是每场活动必须提供的信息，助手不会自动寻找商品或自动购买。详细操作见 [使用手册](USER_GUIDE.md)。

如果系统报告安全风险、证书异常或文件损坏，先停止安装并反馈，不要通过关闭保护绕过提示。当前版本尚未完成真机或模拟器运行验收。

## 其余文件是否需要下载

| 发布文件 | 用途 | 使用 App 是否必需 |
| --- | --- | --- |
| `sale-start-assistant-1.0.0.apk` | 手机安装包 | 是 |
| `sale-start-assistant-manual-zh-CN.pdf` | 九页中文使用与开发手册 | 否，可选阅读 |
| `sale-start-assistant-source-1.0.0.zip` | 1.0.0 源码快照 | 否，只供开发 |
| `SHA256SUMS.txt` | 发布文件的 SHA-256 校验值 | 安装运行不依赖，建议核对 |

[中文手册](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-manual-zh-CN.pdf) · [源码快照](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-source-1.0.0.zip) · [校验文件](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/SHA256SUMS.txt)

PDF 和源码包保留 1.0.0 发布时的内容。最新安装提示、导航和安全说明请看仓库当前文档。

## 来源和完整性核对

1.0.0 APK 的 SHA-256：

```text
b458405afdf156033092df6b0190f19d146a49c68f6c63ca736ed15342c64758
```

Android 包名：`io.github.ayxqn.salestart`。发布 APK 已通过 v2、v3 签名验证，签名证书 SHA-256：

```text
0f921eb1dabf8f948bb8ff459f02f8bb7c8e7f66c0d9e4144e56030daff09978
```

校验值相同可帮助确认文件与此发布版本一致；签名可帮助识别发布来源和后续升级是否使用同一签名。**它们都不是“无病毒”或“100% 安全”的证明。** 若文件不一致，先不要安装。更多核实事实见 [安全与隐私核实说明](SAFETY.md)。
