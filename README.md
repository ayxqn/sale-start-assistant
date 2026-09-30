# 开售助手

**功能核对：当前 1.0.0 APK 是倒计时与官方页面跳转版本，没有旧 V2.4 的账号会话读取、服务端校时、自动创建订单或条件重试。它不是旧抢购流程的等价交付；完整目标仍待恢复。详见 [旧版功能对照](docs/LEGACY_COMPARISON.md)。**

给 B 站官方活动页设置开售时间，在前台倒计时，到点打开官方活动页。登录、下单、核对金额和付款仍由你在官方 App 内完成。

**Android 8.0 及以上 · 1.0.0 内测版 · 原生 Java · MIT 许可**

它只辅助进入活动页，不自动下单、不自动付款，也不保证买到。不支持 iPhone、iPad 或 iOS，与 B 站没有合作关系。

## 先下载哪个文件

普通安卓用户只需下载 **[sale-start-assistant-1.0.0.apk](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-1.0.0.apk)** 并安装。安装后独立运行，不需要电脑、Python、ADB、SDK、服务器、配置文件、AutoJs6、Root 或无障碍服务。

仓库源码与 1.0.0 内测发布文件公开可访问。源码采用 MIT 许可；公开不代表功能已恢复或手机验收已完成。

| 文件 | 适合谁 | 下载 |
| --- | --- | --- |
| `sale-start-assistant-1.0.0.apk` | 普通安卓用户，使用时只需这个安装包 | [下载 APK](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-1.0.0.apk) |
| `sale-start-assistant-manual-zh-CN.pdf` | 想离线阅读使用与开发说明的人，可选 | [下载中文手册](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-manual-zh-CN.pdf) |
| `sale-start-assistant-source-1.0.0.zip` | 想看代码或自行编译的人，使用 App 不需要 | [下载源码快照](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/sale-start-assistant-source-1.0.0.zip) |
| `SHA256SUMS.txt` | 核对下载文件是否与发布文件一致 | [下载校验文件](https://github.com/ayxqn/sale-start-assistant/releases/download/v1.0.0/SHA256SUMS.txt) |

[查看 1.0.0 内测发布页](https://github.com/ayxqn/sale-start-assistant/releases/tag/v1.0.0) · [安装与下载说明](docs/DOWNLOADS.md)

## 第一次打开怎么用

1. 安装上面的 APK，阅读首次启动的中文说明。
2. 点“10 秒演练，不联网”，先熟悉开始和停止。演练不会打开网页或购买。
3. 在大陆版官方 B 站 App 登录自己的账号。
4. 填写官方 `/blackboard/` 活动页 HTTPS 链接，删除问号、井号及其后的分享参数。点“先打开官方页，检查账号”，核对实际页面、商品和资格。
5. 按北京时间选择开售日期和时间，在开售前两小时内点“开始倒计时”，确认后看到“正在等待”。
6. 保持助手在前台，手机解锁并亮屏。到点进入官方页后，由你自己下单和付款。

切到后台、锁屏、来电占据页面或进程被结束，会停止等待，不会自动恢复。回来后需要重新开始。开售时间由你填写，没有活动服务器校时。

详细步骤、换账号、跳转失败和清空设置见 [使用手册](docs/USER_GUIDE.md)。

## 隐私和安全先看这里

- 当前 APK 没有申请 Android 权限，包括联网、无障碍、截图、位置、通讯录、相机、麦克风和外部文件权限。
- 代码没有登录框，不读取或保存账号密码、Cookie、短信、订单或付款资料。登录和付款都在官方 App 内处理。
- 没有第三方运行库、广告、统计、崩溃上报或自动更新 SDK；只在本机保存活动名称、公开链接、时间和已读说明。
- 打开活动页时会把公开链接交给官方 App，失败后由你选择是否使用浏览器。这些外部应用的联网和隐私政策另行适用。
- 编译、30 项核心规则检查、签名验证、权限检查和源码隐私扫描已通过；这些检查不等于无病毒证明或第三方安全审计。

普通设置没有额外加密，不要填写密码或个人资料。不要关闭系统防护来安装，也不要使用来源不明的同名 APK。完整核实范围和限制见 [安全与隐私核实说明](docs/SAFETY.md)、[隐私政策](docs/PRIVACY.md) 和 [安全说明](SECURITY.md)。

## 当前验证到哪一步

| 项目 | 1.0.0 状态 |
| --- | --- |
| Java、资源和 APK 构建 | 已通过 |
| 链接、时间等核心规则 | 30 项检查通过 |
| APK 签名与权限 | v2、v3 签名验证通过；没有权限申请 |
| 真机安装、界面与官方 App 跳转 | 尚未验证 |
| 模拟器实际运行、多机型兼容 | 尚未验证 |
| 真实购买 | 没有测试；助手不自动购买 |

因此本版标为内测。打开页面请求成功不等于正确页面已经显示，更不代表购买成功。具体清单见 [测试与验收](docs/TESTING.md)。

## 按你的目的找说明

| 你想做什么 | 从这里开始 |
| --- | --- |
| 下载、安装、确认哪些文件不用下载 | [安装与下载说明](docs/DOWNLOADS.md) |
| 正常使用、换账号、处理跳转和停止问题 | [使用手册](docs/USER_GUIDE.md) |
| 了解权限、联网、数据保存和安全检查 | [安全与隐私核实说明](docs/SAFETY.md)、[隐私政策](docs/PRIVACY.md) |
| 找代码、测试、工具和文件用途 | [文档与文件导航](docs/README.md) |
| 学习开发步骤或自己构建 APK | [开发与发布](docs/DEVELOPMENT.md) |
| 核对旧版抢购流程是否包含在当前 APK 中 | [旧版功能对照](docs/LEGACY_COMPARISON.md) |
| 核对版本变化和许可 | [版本记录](CHANGELOG.md)、[MIT 许可证](LICENSE) |

## 给开发者

应用源码在 `app/`，核心规则测试在 `tests/`，构建和文档工具在 `tools/`。项目不是微信小程序，也不是跨平台网页 App。开发工具只供编译使用，普通用户不需要安装。

旧个人实验中的内部接口下单、持续重试、账号导入、支付交接和复杂测试菜单没有进入这个独立版本。旧账号、订单、截图和实验备份留在所有者本地，不在本仓库和发布包中。

源码采用 [MIT 许可](LICENSE)，可依许可使用、修改和再分发，需保留版权与许可声明。软件许可不等于平台活动授权；活动禁止辅助工具时不要使用。
