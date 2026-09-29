# XHookLab

LSPosed Hook 模块：支持在线调试、热改参数、九种 Hook 类型、Native 热改 diff。

## 功能
- 实时规则：`/data/adb/xhooklab/rules.json`
- 控制 HTTP 端口：主进程 19321
- 路由：/ping /reload /reapply /natives /logs /clear /set /get
- Hook 类型：BEFORE / AFTER / REPLACE / REPLACE_ARGS / THROW / CALL_STACK / CONSTRUCTOR / FIELD / NATIVE

## 编译
推送到 GitHub main 分支后，Actions 自动出包（无需本地环境）。
产物在 Actions -> Artifacts：XHookLab-apk。
