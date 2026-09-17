# 浏览器语音输入（麦克风）HTTPS 部署步骤

> 目标：解决"浏览器禁止不安全页面录音"，让 AI 助手的按住说话/语音转文字可用
> 涉及环境：本机开发(8080)、局域网访问、生产服务器 118.31.165.121（普通 nginx 部署，非 docker）

---

## 一、原理与判定（先看这个，免得白忙）

浏览器规定：**麦克风（getUserMedia）只允许在"安全上下文"里使用**。是不是安全上下文，一张表判定：

| 访问方式 | 是否可用麦克风 |
|---------|--------------|
| `http://localhost:8080` / `http://127.0.0.1:8080` | ✅ 可以（localhost 被豁免） |
| `http://localhost:8090`（代理页） | ⚠️ localhost 本身豁免，但**内嵌浏览器是 iframe 环境**，iframe 没带 `allow="microphone"` 会被权限策略拦截——**语音测试别在代理页里做** |
| `http://192.168.x.x:8080`（局域网 IP + HTTP） | ❌ 禁止 |
| `https://192.168.x.x:8080`（局域网 IP + HTTPS） | ✅ 可以（自签证书需点"继续前往"） |
| `http://118.31.165.121:8889`（服务器 HTTP） | ❌ 禁止 |
| `https://118.31.165.121`（服务器 HTTPS，本次目标） | ✅ 可以 |

另一个硬性规则：**HTTPS 页面不能调用 HTTP 接口（混合内容拦截）**。所以上了 HTTPS 后，接口必须同源走 `https://…/api/…` 反代到后端——前端 `URLS.js` 已配置生产环境 baseURL=`/api`（同源相对路径），nginx 443 里配好反代即可，前端代码**不用改**。

---

## 二、总览：三个场景三条路

1. **本机自己测** → 用 `https://localhost:8080`（dev 服务器已配好 HTTPS，证书含 localhost）
2. **局域网其他电脑/手机测** → 用 `https://<本机局域网IP>:8080`（dev 证书已含 192.168.1.127 / 192.168.1.113 / 10.239.170.249 / 10.65.191.24；IP 变了要重签，见附录 A）
3. **生产服务器真实使用** → `https://118.31.165.121`（本文重点，第二~五章）

---

## 三、服务器部署 HTTPS（118.31.165.121）

### 步骤 1：上传证书到服务器

证书已备好在仓库：`backend/nginx/certs/server-121.crt` 和 `server-121.key`
（自签，CN=118.31.165.121，SAN 含 `IP:118.31.165.121`，有效期至 2036 年）

在本机 Git Bash 执行：

```bash
# 先在服务器上建目录（ssh 连上后）
ssh root@118.31.165.121 "mkdir -p /etc/nginx/certs"

# 上传证书和私钥
scp D:/java_project/ai_code/ai_manage-master/backend/nginx/certs/server-121.crt root@118.31.165.121:/etc/nginx/certs/
scp D:/java_project/ai_code/ai_manage-master/backend/nginx/certs/server-121.key root@118.31.165.121:/etc/nginx/certs/

# 私钥权限收紧（600 = 仅 root 可读写）
ssh root@118.31.165.121 "chmod 600 /etc/nginx/certs/server-121.key"
```

### 步骤 2：上传最新的前端包和后端 jar

> 最近 AI 助手改了很多（语音、链接、筛选菜单、是非确认），**必须同步部署新包**，否则 HTTPS 好了功能还是旧的。

**前端打包（本机执行）：**

```bash
cd D:/java_project/ai_code/ai_manage-master/task-miniprogram
npm run build
# 产物在 dist/ 目录，整个上传到服务器 H5 根目录（覆盖旧的）
scp -r dist/* root@118.31.165.121:<你的H5目录>/
```

**后端打包（本机执行，注意用 JDK8）：**

```bash
cd D:/java_project/ai_code/ai_manage-master/backend/springboot
JAVA_HOME="D:\develop\jdk1.8" mvn package -DskipTests
# 上传 jar
scp target/springboot-0.0.1-SNAPSHOT.jar root@118.31.165.121:<你的后端目录>/
```

**服务器上重启后端**（按你现有启动方式，例如）：

```bash
# 找到旧进程并停掉
ps -ef | grep springboot | grep -v grep
kill <旧进程PID>

# 启动新 jar
nohup java -jar springboot-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

### 步骤 3：nginx 增加 443 配置

编辑服务器上的 nginx 配置（路径通常是 `/etc/nginx/nginx.conf`，若用了 conf.d 拆分则在 `/etc/nginx/conf.d/` 下新建一个 `.conf` 文件）。

> 注意：仓库 `backend/nginx.conf` 里的 443 配置是 **docker 版**（proxy_pass 写的是容器服务名），普通部署要用下面这份。**两处标了【按实际改】的路径换成你的真实路径。**

```nginx
# ============ H5 小程序（443 HTTPS）——语音输入必须走这里 ============
server {
    listen 443 ssl;
    server_name 118.31.165.121;

    ssl_certificate     /etc/nginx/certs/server-121.crt;
    ssl_certificate_key /etc/nginx/certs/server-121.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # H5 前端静态文件【按实际改：你 8889 那个 server 里 root 指向哪，这里就写哪】
    location / {
        root /usr/share/nginx/html/h5;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # 后端接口反代：同源 HTTPS，规避混合内容拦截
    # 注意 proxy_pass 末尾的 / ——会把 /api/xxx 转成 /xxx 再发给后端(9090无/api前缀)
    location /api/ {
        proxy_pass http://127.0.0.1:9090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 600s;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
        # AI 流式回答(SSE)必须关缓冲，否则打字机效果变成一坨一起出
        proxy_buffering off;
        proxy_cache off;
    }

    # 上传文件等静态资源【按实际改：和 8889 里的 /resource 保持一致】
    location /resource/ {
        root /app;
        autoindex off;
        expires 30d;
    }
}
```

> `/api/` 的 `proxy_pass` 末尾带 `/` 是关键：前端请求 `https://118.31.165.121/api/ai/chat`，转发到后端变成 `http://127.0.0.1:9090/ai/chat`（后端没有 /api 前缀）。如果后端接口本身带 /api 前缀，就去掉这个斜杠。

### 步骤 4：检查并重载 nginx

```bash
nginx -t                # 测试配置语法，必须显示 ok / successful
nginx -s reload         # 平滑重载（或 systemctl reload nginx）
```

若 `nginx -t` 报 `unknown directive ssl`，说明 nginx 编译时没带 ssl 模块（很少见），执行 `nginx -V 2>&1 | grep -o with-http_ssl_module` 确认。

### 步骤 5：阿里云安全组放行 443 端口

1. 登录阿里云控制台 → ECS → 实例 → 找到 118.31.165.121 这台
2. 点实例进入 → **安全组** 标签 → 点安全组 ID → **配置规则**
3. **入方向** → **手动添加**：
   - 授权策略：允许
   - 协议类型：自定义 TCP
   - 端口范围：**443/443**
   - 授权对象：0.0.0.0/0
4. 确定（不用重启实例，立即生效）

### 步骤 6：服务器本机防火墙（如果开了的话）

```bash
# Ubuntu (ufw)
ufw status              # 显示 inactive 就跳过这步
ufw allow 443/tcp

# CentOS (firewalld)
firewall-cmd --list-ports
firewall-cmd --permanent --add-service=https && firewall-cmd --reload
```

### 步骤 7：连通性验证

```bash
# 本机 Git Bash 验证（-k 跳过自签证书校验）
curl -vk https://118.31.165.121

# 期望：HTTP 200，且输出里能看到证书 CN=118.31.165.121
# 再验证接口反代：
curl -k -X POST https://118.31.165.121/api/login -H "Content-Type: application/json" -d '{}'
# 期望：返回 JSON（哪怕是账号密码错误，说明 /api 通了）
```

任何一步不通 → 查第六章排错表。

---

## 四、浏览器端允许麦克风（每个浏览器一次）

1. 浏览器打开 `https://118.31.165.121`（先登录系统进入 AI 助手页）
2. **第一次会弹证书警告**（自签证书正常现象）：
   - Chrome/Edge：点"高级" → "继续前往 118.31.165.121（不安全）"
   - 点了"继续前往"后页面就是 HTTPS 安全上下文，**麦克风可用**，这是自签证书的正常用法
3. 长按"按住说话"按钮 → 浏览器弹出麦克风权限询问 → **允许**
4. 开始说话 → 松开 → 文字进入输入框 = 全链路成功

### 权限被拒过怎么办

- 点地址栏左侧的"⚠ 不安全"/锁图标 → 网站设置 → 麦克风 → 改为"允许" → 刷新页面
- 或直接进设置：Chrome 地址栏输入 `chrome://settings/content/microphone`，把 118.31.165.121 从"不允许"删掉或加入"允许"

---

## 五、本地/局域网开发环境测语音

dev 服务器（8080）已经配好 HTTPS 和自签证书（`task-miniprogram/dev-certs/`，SAN 覆盖本机所有 IP）：

1. **本机测**：新开浏览器标签（不要用内嵌浏览器/8090 代理页）访问 `https://localhost:8080`
2. **局域网测**：手机/其他电脑访问 `https://<本机IP>:8080`（如 `https://192.168.1.127:8080`），同样点"高级→继续前往"
3. Windows 防火墙需放行 8080 入站（之前已加过规则；换了网络环境若访问不了，重新检查）

> ⚠️ 语音功能测试务必在**独立浏览器标签**里做，别在 localhost:8090 的内嵌浏览器页里测——内嵌环境是 iframe，浏览器对 iframe 里的麦克风有额外权限策略限制，测了也不准。

---

## 六、排错表（按现象对号入座）

| 现象 | 原因 | 解决 |
|------|------|------|
| 浏览器打开 `https://118.31.165.121` 一直转圈/拒绝连接 | 安全组 443 没放行 / nginx 没监听 | 步骤 5、6；服务器上 `curl -vk https://127.0.0.1` 本机自测，通则是安全组问题 |
| `nginx -t` 报错 | 配置粘贴不完整/路径错 | 看报错行号；确认证书路径、H5 root 路径存在 |
| 页面能开但登录就报"请求失败" | /api 反代没通 | `curl -k -X POST https://118.31.165.121/api/login ...` 验证；检查 proxy_pass 是否指向 127.0.0.1:9090（非 docker 部署别用容器服务名） |
| 登录正常但 AI 回答"一坨一起出"没有打字机效果 | /api 反代没关缓冲 | 确认 443 的 location /api/ 里有 `proxy_buffering off;` |
| 长按说话没反应、提示被禁止 | 权限被拒过 | 第四章"权限被拒过怎么办" |
| 提示麦克风设备不存在 | 系统级问题 | Windows 设置→隐私→麦克风→允许桌面应用访问；换浏览器试 |
| 安卓 Chrome 点"继续前往"后麦克风仍不可用 | 证书 SAN 不含该 IP（老证书） | 证书 SAN 已含 118.31.165.121；若以后换 IP 需按附录 A 重签 |
| **iOS Safari** 无法信任自签证书 | iOS 政策：IP 自签证书装描述文件也不稳定 | 见第七章，正式上域名 + 免费可信证书 |
| **微信里打开**麦克风不可用/白屏 | 微信内置浏览器不支持自签证书；iOS 微信无 MediaRecorder | 见第七章 |

---

## 七、平台兼容性说明（重要预期管理）

| 平台 | 自签 IP 证书（当前方案） | 说明 |
|------|------------------------|------|
| 桌面 Chrome / Edge | ✅ 完整可用 | 点"继续前往"后一切正常 |
| Android Chrome / 系统浏览器 | ✅ 可用 | 同上 |
| iOS Safari | ⚠️ 勉强/不可用 | iOS 要求安装并信任描述文件（设置→通用→VPN与设备管理→安装；再设置→通用→关于本机→证书信任设置→开启信任），对纯 IP 证书支持仍不稳定 |
| 微信内置浏览器 | ❌ 不建议 | 自签证书直接不认；且 iOS 微信没有 MediaRecorder API |

**正式上线建议**（后续有域名时）：
1. 买/绑一个域名（解析到 118.31.165.121）
2. 阿里云免费 DV 证书（或 Let's Encrypt）签发该域名
3. 替换 nginx 里两行证书路径即可，其余配置不变
→ 所有平台（含 iOS、微信）直接信任，无需任何手动操作。

---

## 八、验收清单（全过 = 完成）

- [ ] `curl -vk https://118.31.165.121` 返回 200，证书 CN=118.31.165.121
- [ ] `curl -k -X POST https://118.31.165.121/api/login -d '{}'` 返回 JSON
- [ ] 浏览器 `https://118.31.165.121` 能登录系统（证书警告点继续后）
- [ ] AI 助手问答正常，回答是流式打字机效果（验证 SSE 没被缓冲）
- [ ] 回答里的任务名称可点击跳详情（验证新包已部署）
- [ ] 切到麦克风输入，长按说话，松开后文字上屏
- [ ] 左滑取消、右滑转文字手势正常
- [ ] 旧链接 `http://118.31.165.121:8889` 自动跳转到 https（如果保留了 8889→301 的配置）

---

## 附录 A：以后本机 IP 变了，重签局域网开发证书

```bash
cd D:/java_project/ai_code/ai_manage-master/task-miniprogram/dev-certs

MSYS2_ARG_CONV_EXCL="*" openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout dev-cert.key -out dev-cert.pem -days 3650 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:192.168.1.127,IP:<新IP>"
```

（`MSYS2_ARG_CONV_EXCL="*"` 是 Git Bash 必需的，防止把 /CN=… 当路径转换；IP 列表写成当前所有会用到的地址）签完重启前端 dev server 即可。

## 附录 B：仓库文件对照

| 文件 | 用途 |
|------|------|
| `backend/nginx/certs/server-121.crt/.key` | 生产 118.31.165.121 的自签证书（SAN 含该 IP，2036 年到期） |
| `backend/nginx.conf` | docker 版参考配置（普通部署按本文第三章手写版） |
| `task-miniprogram/dev-certs/dev-cert.pem/.key` | 本地开发 HTTPS 证书（SAN 含本机各 IP） |
| `task-miniprogram/vue.config.js` | dev server 已启用 HTTPS（读这两个证书文件） |
| `task-miniprogram/src/config/URLS.js` | 生产/开发 baseURL 均为同源 `/api`，无需为 HTTPS 修改 |
