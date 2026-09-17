# AI 智能助手汇报

> 任务管理系统 · AI 助手（智能问答 + 语音识别 + 历史会话）
> 技术栈：Spring Boot + MyBatis + 智谱 GLM（glm-4-flash / glm-asr） + uni-app（H5）

---

## 〇、功能总览

用户通过 **文字或语音** 向 AI 助手提问，AI 自动理解问题、查询数据库中的**任务、会议、重点工作**三类数据，以自然语言回答；答案中的条目名称可点击跳转详情页；支持澄清选项点选、历史会话回溯、逐轮删除。

**一句话架构**：本地不部署 AI 模型，后端作为"调度器"调用智谱 GLM 的 Function Calling（工具调用）能力——**模型负责理解语言和组织回答，后端负责查库、权限控制与结果质检**。

```
用户提问（文字 / 语音）
   │ 语音先经 GLM-ASR 转文字（带热词表，降低专有名词错字率）
   ▼
前端 ai-chat.vue ──fetch SSE──▶ /ai/chat/stream
   │
   ▼
AiChatService.doChat()
   ├─ 意图门控：模糊问题直接返回标准追问（不进模型）
   ├─ Function Calling 循环（最多5轮）：
   │     GLM："我要调 getUsers" → 后端查库 → 结果回传
   │     GLM："我要调 getTasks" → 后端查库 → 结果回传
   │     GLM 返回纯文本 → 即最终答案
   ├─ 防幻觉质检 + 摘取【选项】+ 问答落库
   ▼
前端逐帧渲染：思考步骤 → 打字机答案 → 可点击选项 / 任务链接
```

---

## 一、可以解析的提问方式（关键字体系）

AI 通过**后端写死的意图门控 + GLM 语义解析**两层机制理解提问。

### 1.1 两层识别机制总览：后端"门卫" + GLM"解析"（谁在识别关键字）

**结论：两层都有，但职责完全不同。后端识别是"门卫"，只判断问题能不能放行；GLM 识别才是"真正的解析"，负责把话变成查询参数。**

以"张三最近三天的任务"为例，一句话的完整旅程：

```
"张三最近三天的任务"
   │
   ▼ 第一层：后端写死的关键字识别（门卫，不调模型、零成本）
   intentGate()：有"任务"→有模块 ✓；有"最近"→有时间 ✓ → 条件充分，放行
   │   （后端到这一步都不知道"张三"是谁、"三天"是几号，它只回答了两个是非题）
   ▼
   │ 第二层：GLM 的语义解析（真正的关键字理解）
   GLM 读到问题+提示词规则 → 决定调 getUsers("张三") 拿用户ID
   │                    → 再决定调 getTasks(headId=u123, startTime=…, endTime=…)
   │                      （"最近三天"→具体日期的换算在这步由GLM完成）
   ▼
   后端执行工具=查数据库 → 结果回传GLM → GLM 写答案
```

**第一层：后端写死的识别**（intentGate / detectModule / hasTimeExpression，AiChatService.java 474-534行）——只回答 3 个是非题（有没有模块词？有没有时间词？是不是数字/省略追问？），输出只有"放行 / 拦下"两种。拦下时直接返回标准问句+点选选项，GLM 根本不会被调用：

```java
// intentGate() 的三分支决策（简化）
if (module != null && !hasTime)            // "查会议"：有模块没时间
    return 标准问句 + 选项【查本周的会议|查本月的会议|…】;   // 不进GLM
if (module == null && !hasTime && message.length() <= 12)   // "查一下"
    return "您想查询哪类信息？" + 选项【查本周的任务|查会议|查重点工作】;
return null;                               // 条件充分 → 放行，交给GLM
```

**第二层：GLM 的识别**——通过门控的问题，"关键字→查询条件"全部由 GLM 完成。但 GLM 不是天生就会，映射规则是**写成文字（提示词）**喂给它的（buildSystemPrompt，1179-1231行）：

```java
// 模块映射规则（GLM据此决定调哪个工具）
"用户说\"会议/开会\"指meeting，说\"重点工作/重要事项/事件\"指works，说\"任务/计划\"指plan。"
// 时间换算规则（先告诉它今天几号，再要求换算成具体日期）
"今天是 " + today + "。用户提到的\"今天/本周/本月\"等相对时间，请先换算成 yyyy-MM-dd 再传给工具。"
// 条件解析规则
"6. 用户提到负责人姓名(如\"张三的任务\")时，先调用 getUsers 按姓名解析出用户id，再把id作为headId传给getTasks…"
```

GLM 按规则产出的就是**工具调用参数**，由后端取出来执行查库（doChat 循环，222-223行）：

```java
String fnName = call.getJSONObject("function").getStr("name");      // GLM决定调哪个工具
String fnArgs = call.getJSONObject("function").getStr("arguments"); // GLM生成的查询参数JSON
result = executeTool(fnName, fnArgs, turnDetails);                  // 后端拿参数查数据库
```

**两层分工对照表**：

| | 后端写死的识别（门卫） | GLM 的识别（解析） |
|---|---|---|
| 代码位置 | intentGate 等（474-534行），Java `contains()` + 正则 | 规则写在 buildSystemPrompt（1179-1231行），识别发生在智谱服务器 |
| 识别什么 | 3 个是非题：有没有模块词/时间词/是不是追问 | 完整解析：哪个模块、时间是几号到几号、姓名→ID、科室→ID、状态→字典ID、"我的"→mine |
| 输出什么 | 放行 / 拦下（+标准选项） | 工具名 + 参数 JSON，如 `{"headId":"u123","startTime":"2026-08-29"}` |
| 处理哪些输入 | 所有消息先过这一关 | 只处理放行的、条件充分的 |
| 为什么需要它 | 模糊问题交给小模型最容易瞎编，写死规则 100% 可控且零成本 | 条件组合爆炸（时间×人名×科室×状态×模块），写规则不现实，必须靠模型泛化 |

> 一句话：**后端识别"要不要听懂"（守门），GLM 识别"到底说了什么"（解析）**。两层关键字表长得像（会议/开会、任务/计划），但一层是 Java 的 contains()，另一层是提示词文字规则由模型执行。

### 1.2 模块识别（查哪张表）

| 用户说法 | 识别为 | 实际查询 |
|---|---|---|
| "任务"、"计划" | 任务模块 | plan 表 |
| "会议"、"开会" | 会议模块 | meeting 表 |
| "重点工作"、"重要工作"、"事件"、"事项" | 重点工作模块 | works 表 |

### 1.3 时间表达识别

| 提问中的时间词 | 处理方式 |
|---|---|
| 今天 / 明天 / 昨天 / 后天 | 按当天日期换算成 yyyy-MM-dd |
| 本周 / 这周 / 下周 / 上周 / 当周 | 换算为本周一起止日期 |
| 本月 / 这个月 / 上个月 / 下个月 / 上月 / 下月 | 换算为月区间 |
| 今年 / 去年 / 季度 / 年度 / 最近 | 换算为对应日期区间 |
| 具体日期："2026-08-31"、"8月5号"、"2026年8月" | 正则识别后直接使用 |

系统提示词中注入当天日期，模型据此完成相对时间→绝对日期的换算：

```java
// buildSystemPrompt() 中
LocalDate today = LocalDate.now();
String[] weeks = {"一","二","三","四","五","六","日"};
"今天是 " + today + " 星期" + weeks[today.getDayOfWeek().getValue()-1] + "。"
+ "用户提到的\"今天/明天/本周/上周/本月/下月\"等相对时间，"
+ "请先按今天日期换算成 yyyy-MM-dd 再传给工具。"
```

### 1.4 条件类表达

| 提问方式 | 解析结果 |
|---|---|
| "张三的任务"、"周敏香负责什么" | 先调 getUsers 按姓名解析出用户ID → headId 条件 |
| "信息化建设科的任务" | 先调 getDepartments 解析出科室ID → departmentId 条件 |
| "我的任务"、"我最近完成了什么" | mine=true，只查当前登录人自己申报的 |
| "进行中的"、"重要的"、"已完成的" | 先调 getDictOptions 拿字典ID → statusId / impId / typeId |
| "张三主持的会议" | host 条件（主持人姓名模糊匹配） |
| 关键词："关于XX的任务"、"XX会议" | keyword 名称模糊匹配 |
| 追问式："那任务呢"、"会议呢" | 从上下文继承时间等条件，自动补全查询 |
| 语音错字："章三"、"健设科" | 字符重合度自动纠错，纠错后附确认选项 |

### 1.5 模糊提问的引导（意图门控）

提问缺模块或缺时间时，**不经过模型**，后端直接返回标准问句+点选选项，避免模型瞎猜：

```java
// intentGate()：核心是关键词+正则的确定性判断
private String detectModule(String message) {
    if (message.contains("重点工作") || message.contains("重要工作")
            || message.contains("事件") || message.contains("事项")) return "重点工作";
    if (message.contains("会议") || message.contains("开会")) return "会议";
    if (message.contains("任务") || message.contains("计划")) return "任务";
    return null;
}
```

- 只说"查一下"（无模块无时间的短消息）→ 问"您想查询哪类信息？"+ 选项：查本周的任务 / 查本周的会议 / 查本周的重点工作
- 只说"查会议"（有模块无时间）→ 问"您想查询什么时间范围的会议？"+ 选项：查本周 / 查本月 / 查最近一个月 / 查全部

**支持的完整问法示例**：

- "本周我有哪些任务？"
- "张三这个月完成了什么工作？"
- "明天有什么会议？"
- "下周的信息化重点工作会议在哪开？"
- "最近三个月我完成了什么？" →（接着问）"那会议呢？"
- （语音）"看一下李伟本周的重点工作"

### 1.6 时间识别的边界与优化思路（已知局限）

**先澄清一个容易误解的点**："最近三天的任务"**是可以识别的**。识别分两层：

- 门控层词表收录了"最近"（这层只判断"有没有时间"，不负责算日期）→ 放行；
- "三天 → 具体日期区间"的换算由 GLM 完成（提示词中有明确指令）。

**真正的边界**是词表未收录的说法，如"**近**三天"（词表是"最近"）、"**近期**"、"**这几天**"。这类输入会命中"有模块没时间"分支，AI 反问时间范围并给标准选项——**这是刻意设计的 fail-safe 安全降级：宁可不猜也不查错范围，代价只是用户多点一次选项**。

| 输入 | 实际行为 |
|---|---|
| "最近三天的任务" | ✅ 正常识别并查询（"最近"在词表 + GLM换算日期） |
| "近三天的任务"、"近期的任务" | ⚠️ 安全降级：反问时间范围，用户点选后继续 |

**硬编码的优化路线（由轻到重）**：

1. **补量化正则**（成本最低，收益最大）：中文时间高度规律，一条正则可兜住"近N天/这N周/前N个月"全部量化表达：
   ```java
   // 覆盖"近3天/最近三天/这两周/前两个月"等量化表达
   if (message.matches(".*[近前这][0-9一二三四五六七八九十两]+\\s*(天|日|周|礼拜|月|个月).*")) {
       return true;
   }
   ```
2. **引入中文时间归一化库**（如开源 Time-NLP）：把"下礼拜三""大后天""三天前"等任意中文时间表达**确定性地**解析为日期区间，替换手写词表，覆盖率接近100%且不依赖模型；
3. **规则配置化**：词表从 Java 常量迁到字典表（sys_dict），运营在管理界面加词即生效，无需发版；
4. **数据驱动迭代**：利用已落库的 ai_chat_message 表统计"被反问时间"的会话比例与用户原始说法，针对性补词——用真实数据驱动词表完善，而非拍脑袋。

> 设计观点：**硬编码不是原罪，"只靠硬编码且永不迭代"才是**。门控层的定位是轻量守门员（防小模型在模糊输入上瞎发挥），正确姿势是"正则兜量化表达 + 时间库兜长尾 + 日志数据驱动迭代"，三者叠加后硬编码盲区趋近于零。

---

## 二、涉及的数据库表

### 2.1 业务查询表（8张，全部只读）

| 表名 | 用途 | AI 工具 | 权限控制 |
|---|---|---|---|
| `plan` | 任务/计划主表 | getTasks | 复用网页端数据范围（AuthCheckUtils.applyPlanQueryScope），按角色限定本科室/本部门/个人可见 |
| `meeting` | 会议安排 | getMeetings | 本租户内可见 |
| `works` | 重点工作 | getWorks | 非学院领导只能查公开（isopen=1）的 |
| `sys_dict` | 字典表（状态/类型/重要程度/性质） | getDictOptions | 本租户字典 |
| `user` | 用户表 | getUsers | 只查本租户；只返回 id/姓名/科室/角色，**绝不返回密码等敏感字段** |
| `department` | 科室表 | getDepartments | 本租户科室 |
| `plan_log` / `works_log` 等 | 由底层 Service 内部关联 | — | 随主查询联动 |

所有查询强制带租户隔离条件（多租户 SaaS 架构）：

```java
User user = TokenUtils.getCurrentUser();
Plan plan = new Plan();
plan.setTid(user.getTid());   // 只查本租户数据，A机构查不到B机构
// ...填充模型给出的查询条件
AuthCheckUtils.applyPlanQueryScope(plan);  // 与网页端完全一致的数据权限
PageInfo<Plan> pageInfo = planService.getPlanPage(plan, page, pageSize);  // 复用现有Service
```

### 2.2 AI 功能自建表（2张）

`backend/springboot/sql/ai_chat_init.sql`（幂等脚本，可重复执行）：

```sql
CREATE TABLE IF NOT EXISTS `ai_chat_session` (      -- 会话表（软删）
  `id` VARCHAR(64) NOT NULL COMMENT '主键UUID',
  `uid` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(100) DEFAULT '' COMMENT '会话标题(首问前20字)',
  `isdelete` CHAR(1) DEFAULT '0' COMMENT '软删标记',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_uid_update` (`uid`,`update_time`)
) COMMENT='AI助手会话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (      -- 消息表（物理删）
  `id` VARCHAR(64) NOT NULL,
  `sid` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant',
  `content` TEXT COMMENT '消息内容',
  `seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插入序号(保证同轮顺序)',
  PRIMARY KEY (`id`), KEY `idx_sid_ctime` (`sid`,`create_time`), KEY `idx_seq` (`seq`)
) COMMENT='AI助手会话消息';
```

---

## 三、代码实现思路

### 3.1 整体分层

```
前端 ai-chat.vue（952行）     聊天界面：录音/SSE解析/打字机/选项/跳详情
后端 AiChatController（175行） 8个REST接口：对话(同步+SSE流式)/ASR/会话管理
后端 AiChatService（1355行）   核心：意图门控、FC循环、防幻觉、热词、落库
后端 AsrService（81行）        GLM-ASR 语音转文字封装
自建 utils/AuthCheckUtils      从网页端逻辑中抽出的数据权限工具
```

```js
前端向后端发起请求的接口

const resp = await fetch(config.baseURL + '/ai/chat/stream', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'token': uni.getStorageSync('token')   // JWT，JwtInterceptor 用它识别你是谁
  },
  body: JSON.stringify({
    message,           // 本次问题
    history,           // 最近10条历史对话（前端截取）
    sessionId,         // 当前会话ID（续聊用）
    clarifyReply       // 是否点了选项胶囊
  })
})
```



**1. GLM-4-flash（对话模型）的调用**

实际发 HTTP 请求的地方：`AiChatService.java:1140-1162` 的 `callChatCompletions()` 方法——这是整个系统唯一调对话模型的地方：

```java
private JSONObject callChatCompletions(JSONArray messages) {
    JSONObject reqBody = JSONUtil.createObj()
            .set("model", model)                    // 模型名从配置读入
            .set("messages", messages)              // 系统提示词+历史+用户消息+工具结果
            .set("tools", buildToolsSchema())       // 6个工具的定义
            .set("temperature", 0.3);
    String respBody = HttpRequest.post(baseUrl + "/chat/completions")  // 智谱接口
            .header("Authorization", "Bearer " + apiKey)
            .body(reqBody.toString())
            .timeout(60000)
            .execute().body();
    // ...解析 choices[0].message 返回
}

调用链：doChat() 循环里每轮 callChatCompletions(messages)（208行）→ 打到 https://open.bigmodel.cn/api/paas/v4/chat/completions

**2. GLM 语音转文本（glm-asr-2512）的调用**

整个 AsrService.java（81行）就是干这个的，核心是 recognizeWav()，47-79行：

```java
HttpRequest request = HttpRequest.post(baseUrl + "/audio/transcriptions")  // 智谱ASR接口
        .header("Authorization", "Bearer " + apiKey)   // 与对话模型共用同一个Key
        .form("model", asrModel)                       // glm-asr-2512
        .form("file", new BytesResource(audio, "voice.wav"));
if (hotwords != null && !hotwords.isEmpty()) {
    request.form("hotwords", JSONUtil.toJsonStr(hotwords));  // 热词表
}
String resp = request.timeout(30000).execute().body();
return json.getStr("text", "").trim();                // 返回识别文本
```



### 3.2 核心：Function Calling（工具调用）循环

模型本身不知道数据库里有什么。我们在请求中声明 6 个工具，模型需要真实数据时返回"工具调用请求"，由后端执行查库并把结果回传，循环直到模型给出最终回答：

```java
// AiChatService.doChat() 核心循环（简化）
for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {   // 最多5轮，防死循环
    JSONObject respMsg = callChatCompletions(messages);     // 调GLM
    JSONArray toolCalls = respMsg.getJSONArray("tool_calls");
    if (toolCalls == null || toolCalls.isEmpty()) {         // 不要工具了
        answer = respMsg.getStr("content");                 // → 纯文本即最终答案
        break;
    }
    messages.add(respMsg);                                  // 回传模型的调用请求
    for (int i = 0; i < toolCalls.size(); i++) {
        JSONObject call = toolCalls.getJSONObject(i);
        String fnName = call.getJSONObject("function").getStr("name");
        String fnArgs = call.getJSONObject("function").getStr("arguments", "{}");
        // 推送"思考步骤"给前端实时展示
        if (sink != null) sink.accept(step(describeTool(fnName, fnArgs)));
        // 执行工具=查数据库（复用现有Service与权限体系）
        String result = executeTool(fnName, fnArgs, turnDetails);
        messages.add(new JSONObject()
                .set("role", "tool")
                .set("tool_call_id", call.getStr("id"))
                .set("content", result));                   // 结果以role=tool回传模型
    }
}
```

**6 个工具（AI 唯一的数据入口）**：

| 工具 | 功能 | 底层 |
|---|---|---|
| getTasks | 条件查任务（关键词/状态/负责人/科室/时间/仅我的/分页） | planService.getPlanPage |
| getMeetings | 按天或日期范围查会议（含跨天会议） | meetingService |
| getWorks | 条件查重点工作 | worksService.getWorksPage |
| getDictOptions | 状态/类型/重要程度字典对照（自然语言→字典ID） | sysdictService |
| getUsers | 姓名模糊查用户（姓名→用户ID，含错字纠错） | userService |
| getDepartments | 科室对照（科室名→科室ID） | departmentService |

### 3.3 安全与权限设计（重点）

1. **AI 不直接接触数据库**：所有数据必须经 6 个工具进出，每个工具内部复用网页端已有 Service——网页上能看什么，AI 就只能查到什么；
2. **身份继承**：请求带 JWT → JwtInterceptor 校验并填充 ThreadLocal → 工具执行时 `TokenUtils.getCurrentUser()` 取当前用户，AI 查数据用的就是提问者本人身份；
3. **租户隔离**：每个查询强制 `setTid(user.getTid())`；
4. **字段脱敏**：getUsers 只返回 id/姓名/科室/角色，绝不返回密码；
5. **异常不外泄**：工具失败只告诉模型"查询失败"，不传堆栈。
6. **一个关键实现细节**：SSE 流式接口在请求线程**同步执行**——用户上下文在 ThreadLocal 中，若开异步线程调 GLM 将取不到用户身份，权限直接失效。

### 3.4 防幻觉五层防御（本项目特色）

"幻觉"= 模型一本正经编造不存在的数据。因使用免费的 glm-4-flash 小模型，采取多层防御：

| 层 | 机制 | 位置 |
|---|---|---|
| 1 | **意图门控**：模糊问题不进模型，后端直接返回标准追问 | intentGate() |
| 2 | **提示词铁律**："任何数据必须来自工具返回，严禁编造，没查到就说没查到" | buildSystemPrompt() |
| 3 | **消息级强注入**：点选选项/省略式追问("那任务呢")等高危场景，把禁令直接拼在用户消息后（小模型对长系统提示不敏感） | doChat() 预处理 |
| 4 | **事后检测**：本轮零工具调用但答案像数据清单（"任务名称："或≥2行编号列表）→ 判定编造，整段替换为诚实引导 | looksLikeFabricatedData() |
| 5 | **数字兜底**：模型违规把选项写成正文编号清单时，用户回数字"2"也能正确命中对应选项 | pickNumberedOption() |

```java
// 第4层：事后检测（核心代码）
int toolsCalled = 0;   // 循环中每次executeTool成功后+1
// ...循环结束后
boolean fabricated = toolsCalled == 0 && looksLikeFabricatedData(answer);
if (fabricated) {
    answer = "抱歉，这个问题我没有查到真实数据，不能凭空回答。"
           + "请点选下方选项，或换个说法（如\"查本周的任务\"）：";
    options.add("查任务"); options.add("查会议"); options.add("查重点工作");
}
```

### 3.5 【选项】协议与点选闭环

模型被要求把追问选项写成固定格式（`【选项】查本周|查本月|查全部`）。后端把它从正文摘出，结构化下发；前端渲染成胶囊按钮，点击即发送，并带 `clarifyReply=true`：

```java
// 后端摘取【选项】（模型不一定遵守"单独一行"，按行内任意位置切分）
for (String line : answer.split("\n")) {
    int idx = line.indexOf("【选项】");
    if (idx < 0) { kept.append(line).append('\n'); continue; }
    for (String o : line.substring(idx + 4).split("\\|")) {
        if (!o.trim().isEmpty() && options.size() < 4) options.add(o.trim());
    }
}
```

```js
// 前端点击选项：作为新消息发送，clarifyReply=true让后端禁止AI再追问
tapOption(opt) {
  if (this.loading) return
  this.send(opt, { clarifyReply: true })   // 最多追一轮，不会无限套娃
}
```

### 3.6 语音链路（两段式：前端只录、后端转）

```
按住说话 → recorder-core 录 wav 16kHz/16bit/单声道（GLM-ASR要求）
        → 30秒到点自动提交
        → POST /ai/asr（multipart）
        → 后端携带热词表调 GLM-ASR → 返回文本 → 自动发送进入对话流程
```

**热词表**是降低专有名词错字率的关键：识别前把本租户的**字典值+科室名+人名**（上限100个）传给 ASR，按租户缓存10分钟：

```java
public List<String> buildAsrHotwords() {
    Set<String> words = new LinkedHashSet<>();
    collectDictWords(sysdictService.getAllSysdicByTid(tid), words);   // 状态/类型等字典值
    for (Department d : departmentService.getDepartmentByTid(tid))    // 科室名
        words.add(d.getName());
    for (User u : userService.getAllUser(tid))                        // 人名
        words.add(u.getName());
    return new ArrayList<>(words);   // ≤100个
}
```

**错字纠错**：语音常把"张三"识别成"章三"。getUsers 查不到时按**字符重合度**找最相似姓名，规则足够严格才自动纠正（重合≥2字且严格领先第二名），并机制性附上"对的，就查这个人 | 不是这个人"确认选项：

```java
if (topScore >= 2 && topScore > secondScore) {   // 确定性纠错，不依赖模型自觉
    return JSONUtil.createObj()
        .set("users", new JSONArray().set(toUserBrief(best)))
        .set("note", "未找到与「"+keyword+"」完全匹配的用户，已自动匹配最相似的「"
             + best.getName() + "」。先向用户说明（可能是语音识别错字），然后直接查询…");
}
```

### 3.7 流式输出（SSE）与前端体验

后端每执行一个工具就推一个事件，前端实时渲染"思考过程"，避免长时间白屏等待：

```
data: {"type":"step","content":"正在把「张三」解析为系统用户…"}
data: {"type":"step","content":"按条件检索任务列表…"}
data: {"type":"done","answer":"张三本周有3个任务：…","options":[...],"tasks":[...]}
```

前端用原生 **fetch**（uni.request 不支持流式读取）逐帧解析 SSE，done 后打字机输出答案。工具调用被翻译成用户能看懂的步骤文案：

```java
private String describeTool(String name, String argsJson) {
    switch (name) {
        case "getUsers":  return "正在把「" + keyword + "」解析为系统用户…";
        case "getTasks":  return "按条件检索任务列表…";
        case "getMeetings": return "检索当天的会议安排…";
        ...
    }
}
```

**任务名→详情页链接**：后端把本轮查到的条目完整对象随答案下发（不进模型上下文），前端用正则把答案文本中出现的名称替换成可点击链接，按 type 跳任务/会议/工作详情页。

---

