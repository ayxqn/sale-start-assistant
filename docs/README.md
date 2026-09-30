# 文档与文件导航

[返回项目首页](../README.md)

第一次来，先按自己的目的选一条阅读路线。普通使用者不需要看构建工具或下载源码。

## 阅读路线

| 目的 | 阅读顺序 |
| --- | --- |
| 下载并使用 | [安装与下载](DOWNLOADS.md) → [使用手册](USER_GUIDE.md) |
| 确认隐私与安全 | [安全核实说明](SAFETY.md) → [隐私政策](PRIVACY.md) → [安全说明](../SECURITY.md) |
| 核对旧版抢购功能和当前 APK 的差异 | [旧版功能对照](LEGACY_COMPARISON.md) |
| 看测试是否足够 | [测试与验收](TESTING.md) |
| 学习开发或自行构建 | [开发与发布](DEVELOPMENT.md) → [版本记录](../CHANGELOG.md) → [MIT 许可证](../LICENSE) |

## 仓库里的文件做什么

| 位置 | 用途 | 普通用户需要处理吗 |
| --- | --- | --- |
| [README.md](../README.md) | 项目首页、下载入口、功能边界和版本状态 | 建议先读 |
| [docs/](.) | 安装、使用、隐私、安全、开发与测试说明 | 按需要阅读 |
| [app/src/main/](../app/src/main/) | 安卓应用的代码、安装清单、图标与界面样式 | 不需要 |
| [MainActivity.java](../app/src/main/java/io/github/ayxqn/salestart/MainActivity.java) | 界面、前台倒计时和官方页面打开流程 | 不需要 |
| [SalePolicy.java](../app/src/main/java/io/github/ayxqn/salestart/SalePolicy.java) | 官方链接、时间与时钟变化规则 | 不需要 |
| [tests/SalePolicyTest.java](../tests/SalePolicyTest.java) | 30 项不联网、不购买的核心检查 | 不需要 |
| [tools/build.ps1](../tools/build.ps1) | 开发者在 Windows 构建并签名 APK | 不需要运行 |
| [tools/privacy-check.mjs](../tools/privacy-check.mjs) | 开发者检查源码中可能混入的隐私内容 | 不需要运行 |
| [tools/make-manual.py](../tools/make-manual.py) | 开发者生成 PDF 手册 | 不需要运行 |
| [LICENSE](../LICENSE) | MIT 使用、修改和再分发许可 | 再分发时需保留 |
| [SECURITY.md](../SECURITY.md) | 安全边界和问题反馈 | 遇到问题时查看 |
| [CHANGELOG.md](../CHANGELOG.md) | 版本变化 | 更新前查看 |

## 下载文件和本地构建文件的区别

[Releases](https://github.com/ayxqn/sale-start-assistant/releases/tag/v1.0.0) 提供 APK、PDF 手册、源码快照和校验文件。**使用 App 只需 APK**，其余文件用于阅读、开发或核对来源。

GitHub 的“Code → Download ZIP”下载的是源码，不能直接安装到手机。请用首页的“下载 APK”入口。

开发者自行构建后，本地可能出现 `build/`、`dist/`、`output/`：分别是中间文件、交付文件和发布记录，均不进入 Git。签名私钥和密码文件也不进入 Git，用户无需提供或配置这些文件。

1.0.0 发布页里的 PDF 和源码包是该版本的交付快照。后续导航和说明更新以仓库当前文档为准；应用本身的测试范围以 [测试与验收](TESTING.md) 为准。
