# 本地代码关联 GitCode 操作记录

> 日期：2026-09-01
> 仓库：https://gitcode.com/m0_56310181/ai_manage.git
> 本地目录：`D:\java_project\ai_code\ai_manage-master`

## 一、背景

本地目录 `ai_manage-master` 原为从 GitCode 打包下载的**早期快照**（无 `.git` 目录，不含任何提交历史）。
本次操作的目的是将本地代码与 GitCode 远程仓库建立关联并同步。

## 二、操作过程

1. **初始化本地仓库**
   - `git init`
   - 配置用户信息：
     ```bash
     git config user.name m0_56310181
     git config user.email m0_56310181@noreply.gitcode.com
     ```
   - 关联远程：`git remote add origin https://gitcode.com/m0_56310181/ai_manage.git`

2. **首次提交与推送受挫**
   - 将全部源码提交为合成初始 commit（`af90419`，node_modules / target / .idea 均按 .gitignore 排除）
   - 推送时被 Git Credential Manager 的图形登录窗口阻塞（无法在命令行完成授权）
   - 网络探测确认仓库可达（返回 401，仅缺认证）

3. **完成认证**
   - 再次发起 `git push`，由用户在桌面弹出的 GitCode 授权窗口完成登录，凭据已保存
   - 推送被拒：远程包含本地没有的提交（`! [rejected] fetch first`）

4. **关键发现**
   - 远程仓库**不是空仓库**，而是本项目的原始仓库，已有 5 条历史提交
   - 对比结果：**远程版本比本地快照更新**，多约 1242 行：
     | 文件 | 差异 |
     |---|---|
     | `service/AiChatService.java` | +550 行（三模块查询、防幻觉三层防护） |
     | `task-miniprogram/dev-certs/` | 新增局域网 HTTPS 证书 |
     | `task-miniprogram/src/config/URLS.js`、`manifest.json`、`vue.config.js`、`pages/ai/ai-chat.vue` | HTTPS 部署相关更新 |
     | `doc/` | 新增 AI 功能清单、功能汇报、防护方案文档 |
   - 本地快照无远程缺少的独有内容

5. **最终处理**
   - 放弃合成的初始 commit，将本地分支直接挂到远程真实历史上：
     ```bash
     git fetch origin
     git reset --hard origin/master
     ```
   - 本地工作区已更新到远程最新提交：
     ```
     8159126 AI助手三模块查询、防幻觉三层防护、局域网HTTPS部署
     1faa45d AI助手功能迭代：语音迁移智谱ASR、会话管理、智能交互与多项修复
     431be6d 将AI密钥写入application.yml默认值：修复重启后端后环境变量丢失导致GLM/百度语音不可用的问题
     b6405d9 更新AI聊天功能：后端接口与前端页面优化，新增项目文档
     1ed8a2b 初始提交：任务管理系统（Spring Boot 后端 + uni-app 前端 +AI助手）
     ```

## 三、最终状态

- 本地目录 = GitCode 仓库完整克隆，含全部 5 条提交历史，工作区干净
- 凭据已保存，后续 `git pull / push` 无需再次登录
- 日常同步流程：
  ```bash
  git pull            # 拉取远程更新
  git add -A && git commit -m "说明"
  git push
  ```

## 四、遗留注意事项（代码审查发现，推送公开仓库前需处理）

1. **敏感信息明文入库**：`application.yml` 中含数据库密码、邮箱授权码、GLM API Key（提交 `431be6d` 甚至专门把密钥写成了默认值）；docker-compose / readme 中亦有服务器 IP 与密码
2. **鉴权放行过宽**：`WebConfig` 对 `/works/**`、`/sysdict/**`、`/files/**` 全部免 token，重点工作增删改与字典接口可未授权访问，租户隔离在这些接口上失效
3. **复制级 bug**：`PlanController` / `WorksController` 的 `getPlanLogByPid` 查询后未 return 数据，直接 `return Result.success()`
4. 密码为**无盐 MD5**，新增用户默认密码 = 工号；JWT 载荷用 `split("-")` 解析
5. 数据库时间字段均为 varchar；`plan.head` 历史数据混存用户 ID 与姓名
6. `WebSocketConfig` 缺 `@Configuration`（引入未用）；Spring Security 实际 `permitAll`，鉴权全靠 JwtInterceptor

> 注：以上问题基于早期快照审查得出，`8159126` 新版本中部分可能仍存在，修改前建议逐条复核。
