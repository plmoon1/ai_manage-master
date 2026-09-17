# Cloudflare Tunnel 部署方案（方案B：固定域名 + 免费可信 HTTPS，免备案）

> 目标：为 https://118.31.165.121（自签证书，微信/iOS 不可用）替换为可信证书的固定域名入口
> 原理：服务器上 cloudflared 进程**主动外连** Cloudflare（出站连接，不经服务器入站 80/443），
> 绕开"未备案域名解析国内服务器被拦截"的限制；CF 边缘自动挂 Universal SSL 可信证书
> 证书：**无需自己申请**——Cloudflare 自动签发
> 预计耗时：1 小时 + 域名实名审核（几分钟~1天，唯一不可控项）

---

## 阶段①：注册域名（阿里云万网）

1. wanwang.aliyun.com 搜 `.top` / `.xyz`（首年 ¥5~20），选短域名
2. 完成**域名实名认证**（身份证，审核几分钟~1小时）——实名通过前不能改 DNS
3. **不需要备案**：流量走 CF 隧道出站，域名不解析到国内服务器 IP

## 阶段②：接入 Cloudflare（免费）

1. dash.cloudflare.com 注册免费账号
2. **Add a site** → 输入域名 → 选 **Free($0)**
3. 记下 CF 分配的**两个 NS 地址**（如 `xxx.ns.cloudflare.com`）
4. 阿里云域名管理 → DNS 修改/DNS 服务器 → 换成 CF 的两个 NS
5. 等 CF 面板域名状态变 **Active**

## 阶段③：服务器安装 cloudflared

```bash
wget -q https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -O /usr/local/bin/cloudflared && chmod +x /usr/local/bin/cloudflared
cloudflared --version
```

（GitHub 下载慢：本地下载后 scp 上传到 /usr/local/bin/cloudflared）

## 阶段④：授权登录

```bash
cloudflared tunnel login
```

复制输出的 `https://dash.cloudflare.com/argotunnel?...` 链接到**本地浏览器**打开 →
登录 CF 账号 → 选中域名 → Authorize → 服务器显示 successfully logged in

## 阶段⑤：创建命名隧道 + 绑定子域名

```bash
cloudflared tunnel create task-manage          # 记下 Tunnel ID（永久固定）
cloudflared tunnel route dns task-manage app.你的域名
```

## 阶段⑥：配置 + systemd 守护

```bash
mkdir -p /etc/cloudflared
cat > /etc/cloudflared/config.yml <<'EOF'
tunnel: task-manage
credentials-file: /root/.cloudflared/<TunnelID>.json
ingress:
  - hostname: app.你的域名
    service: http://127.0.0.1:8889      # nginx 现有完整站点(前端+/api+/resource)
  - service: http_status:404
EOF

cloudflared service install
systemctl enable --now cloudflared
systemctl status cloudflared --no-pager | head -5
```

## 阶段⑦：验证

- 浏览器 `https://app.你的域名` → 绿锁 → 登录 → AI 问答
- **微信内打开** → 绿锁、麦克风可用（最终目标场景）
- URL 永久固定；服务器重启 systemd 自动拉起，地址不变

---

## 对比与退路

| 项 | 自签(现状) | trycloudflare | **方案B(本方案)** | 学校域名+阿里云证书(最终态) |
|----|-----------|---------------|------------------|--------------------------|
| 证书 | 自签(需点继续前往) | CF 可信 | CF 可信 | CA 可信 |
| URL 固定 | IP 固定 | ❌ 随机且重启变 | ✅ 固定 | ✅ 固定 |
| 微信/iOS | ❌ | ✅ | ✅ | ✅ |
| 备案 | 不需要 | 不需要 | 不需要 | 需要(学校域名已备案可免) |
| 国内直连速度 | ✅ 最快 | CF 中转 | CF 中转 | ✅ 最快 |

将来切换到学校域名：nginx 443 块改两行 ssl_certificate + server_name 即可，隧道随时停用（`systemctl disable --now cloudflared`）。

## 常见问题

- **SSE 流式经 CF**：默认支持 EventSource/fetch stream；若 AI 回答变"一坨出"，检查 CF 面板 Speed→Optimization 的 Rocket Loader/Auto Minify 是否干扰 JS（一般无需动）
- **cloudflared 日志**：`journalctl -u cloudflared -f`
- **8889 作源的安全性**：cloudflared→nginx 是服务器本机回环（127.0.0.1），不出机器，明文无风险
