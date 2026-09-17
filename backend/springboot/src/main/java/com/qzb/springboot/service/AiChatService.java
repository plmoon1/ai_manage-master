package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.AiChatMessage;
import com.qzb.springboot.entity.AiChatSession;
import com.qzb.springboot.entity.Department;
import com.qzb.springboot.entity.Meeting;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.SysDict;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.entity.Works;
import com.qzb.springboot.mapper.AiChatMessageMapper;
import com.qzb.springboot.mapper.AiChatSessionMapper;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.utils.AuthCheckUtils;
import com.qzb.springboot.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AI 对话服务：GLM(智谱) Function Calling
 * 工具执行复用现有 Service 和权限体系（JwtInterceptor 已为当前请求线程填充用户上下文），
 * AI 不直接接触数据库，用户只能查到自己有权限的任务。
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    /** 最多工具调用轮数，防止死循环 */
    private static final int MAX_TOOL_ROUNDS = 5;
    /** 单页最大返回条数 */
    private static final int MAX_PAGE_SIZE = 20;
    /** 携带的历史对话条数上限 */
    private static final int MAX_HISTORY = 10;
    /** ASR热词上限(GLM-ASR接口限制100个) */
    private static final int MAX_HOTWORDS = 100;
    /** 热词缓存有效期：10分钟 */
    private static final long HOTWORD_TTL_MS = 10 * 60 * 1000L;
    /** 常驻参考层(字典/科室/人员)内嵌的人数上限：超过则不内嵌人员表，人名解析走 getUsers 工具 */
    private static final int REF_MAX_USERS = 150;
    /** 常驻参考层缓存有效期：10分钟（与热词缓存一致） */
    private static final long REF_TTL_MS = 10 * 60 * 1000L;

    @Value("${glm.base-url}")
    private String baseUrl;
    @Value("${glm.api-key}")
    private String apiKey;
    @Value("${glm.model}")
    private String model;

    @Resource
    private PlanService planService;
    @Resource
    private SysdictService sysdictService;
    @Resource
    private UserService userService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private MeetingService meetingService;
    @Resource
    private WorksService worksService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AiChatSessionMapper aiChatSessionMapper;
    @Resource
    private AiChatMessageMapper aiChatMessageMapper;

    /**
     * 处理一轮对话（问答内容自动落库，支持多端历史同步）
     *
     * @param message      用户本次消息
     * @param history      历史对话，元素形如 {"role":"user|assistant","content":"..."}
     * @param sessionId    会话ID，为空或不属于当前用户时自动新建会话
     * @param clarifyReply 本次消息是否为用户对澄清选项的回答（是则禁止再次追问，直接查询）
     * @return {sessionId: 会话ID, answer: 回答, options: 澄清选项}
     */
    public JSONObject chat(String message, List<Map<String, Object>> history, String sessionId, boolean clarifyReply) {
        return doChat(message, history, sessionId, null, clarifyReply);
    }

    /**
     * 流式对话：Function Calling 循环中每个思考步骤通过 sink 实时推送(step 事件)，
     * 最终结果通过 done 事件推送，异常通过 error 事件推送。
     * sink 接收的是 JSON 字符串：{"type":"step|done|error", ...}
     */
    public void chatStream(String message, List<Map<String, Object>> history, String sessionId,
                           Consumer<String> sink, boolean clarifyReply) {
        try {
            JSONObject result = doChat(message, history, sessionId, sink, clarifyReply);
            sink.accept(JSONUtil.createObj()
                    .set("type", "done")
                    .set("sessionId", result.getStr("sessionId"))
                    .set("answer", result.getStr("answer"))
                    .set("options", result.getJSONArray("options"))
                    .set("tasks", result.getJSONArray("tasks"))
                    .set("userMsgId", result.getStr("userMsgId"))
                    .set("aiMsgId", result.getStr("aiMsgId"))
                    .toString());
        } catch (Exception e) {
            log.error("AI流式对话异常", e);
            sink.accept(JSONUtil.createObj()
                    .set("type", "error")
                    .set("content", "AI 服务异常，请稍后重试")
                    .toString());
        }
    }

    private JSONObject doChat(String message, List<Map<String, Object>> history, String sessionId,
                              Consumer<String> sink, boolean clarifyReply) {
        if (ObjectUtil.isEmpty(apiKey)) {
            throw new IllegalStateException("GLM API Key 未配置(glm.api-key)");
        }
        User user = TokenUtils.getCurrentUser();

        // ===== 预处理1：数字快捷选择——用户回复纯数字时，解析为上一条AI回答中对应编号的选项文本 =====
        // (兜住模型违规把选项写成正文编号清单的场景，用户回数字也能正确命中)
        String effectiveMessage = message;
        boolean clarify = clarifyReply;
        if (effectiveMessage.matches("[1-4]")) {
            String picked = pickNumberedOption(history, Integer.parseInt(effectiveMessage));
            if (ObjectUtil.isNotEmpty(picked)) {
                effectiveMessage = picked;
                clarify = true;
            }
        }

        // ===== 预处理1.5：是非确认跟进——上一轮AI给出建议/确认式提问后，用户回极短肯定/否定 =====
        // 这是对上一条建议的确认而非新查询(如空结果后AI建议"查全部任务"，用户答"是")：
        // 极短的"是/不用"不可能是新查询的开头，只要上文有AI回答就按确认处理；
        // 必须跳过意图门控(否则"是"会被问"查哪类信息"丢掉全部上下文)并强注入执行指令
        boolean yesFollowUp = isYesNoReply(effectiveMessage) && history != null && !history.isEmpty();
        boolean yesReply = yesFollowUp && !NO_WORDS.contains(effectiveMessage.trim());

        // ===== 预处理2：省略式追问检测("那任务呢"类，需强注入防编造) =====
        // ("算了/不用了"这类以"了"结尾的否定词已被是非确认优先识别)
        boolean ellipsisFollowUp = !yesFollowUp
                && history != null && !history.isEmpty()
                && effectiveMessage.length() <= 10
                && effectiveMessage.matches(".*[呢了]\\s*[？?]?$");

        // ===== 预处理2.5：筛选清单机制化——点选"按科室/按负责人/按状态筛选"时直接返回清单选项 =====
        // 模型在该场景行为不稳定(有时列清单有时直接重查)，改为后端确定性给出名单。
        // 手输也拦截的短语：含"缩小"，或"按XX筛选/查询"结构——短语短且无歧义，
        // 不会误伤"计算机系的任务有哪些"这类正常查询；不拦的话手输会被门控问"哪类信息"或落给模型罗列人名进正文
        boolean shrinkPhrase = effectiveMessage.length() <= 12
                && (effectiveMessage.contains("缩小")
                    || effectiveMessage.matches("^按.{1,6}(筛选|查询|过滤)$"));
        if (clarify || shrinkPhrase) {
            JSONObject filterMenu = buildFilterOptions(effectiveMessage, user);
            if (filterMenu != null) {
                String menuSid = resolveSession(sessionId, user, effectiveMessage);
                String question = filterMenu.getStr("question");
                List<String> menuOptions = filterMenu.getJSONArray("options").toList(String.class);
                JSONObject menuTurn = saveTurn(menuSid, effectiveMessage, question, null, menuOptions);
                return JSONUtil.createObj()
                        .set("sessionId", menuSid)
                        .set("answer", question)
                        .set("options", menuOptions)
                        .set("tasks", new JSONArray())
                        .set("userMsgId", menuTurn.getStr("userMsgId"))
                        .set("aiMsgId", menuTurn.getStr("aiMsgId"));
            }
        }

        // ===== 预处理3：意图门控——模糊输入不经过模型，直接给标准问句+标准选项 =====
        // (模块明确但缺时间→问时间；无模块无时间的短消息→问模块；模型没机会发明查询方向)
        // 两类放行：写操作意图(交给模型礼貌拒绝)；已带人员/科室/状态等条件的查询(意图已明确，直接查)
        boolean conditionQuery = !clarify && !ellipsisFollowUp
                && detectModule(effectiveMessage) != null
                && !hasTimeExpression(effectiveMessage)
                && !(effectiveMessage.contains("全部") || effectiveMessage.contains("所有"))
                && !looksLikeWriteIntent(effectiveMessage)
                && hasSpecificCondition(effectiveMessage);
        if (!clarify && !ellipsisFollowUp && !yesFollowUp && !looksLikeWriteIntent(effectiveMessage)
                && !conditionQuery && !menuFollowUp(effectiveMessage, history)) {
            JSONObject gate = intentGate(effectiveMessage);
            if (gate != null) {
                String sid = resolveSession(sessionId, user, effectiveMessage);
                String question = gate.getStr("question");
                List<String> gateOptions = gate.getJSONArray("options").toList(String.class);
                JSONObject turn = saveTurn(sid, effectiveMessage, question, null, gateOptions);
                return JSONUtil.createObj()
                        .set("sessionId", sid)
                        .set("answer", question)
                        .set("options", gateOptions)
                        .set("tasks", new JSONArray())
                        .set("userMsgId", turn.getStr("userMsgId"))
                        .set("aiMsgId", turn.getStr("aiMsgId"));
            }
        }

        // ===== 预处理4：独立新查询检测 =====
        // 用户自己说出了完整条件（模块+时间/范围）时，这是一轮全新查询：
        // 禁止继承上文的人员/科室/状态等筛选条件（否则"查下最近任务"会被上一轮的"李慧秋"污染）
        boolean standaloneQuery = !clarify && !ellipsisFollowUp
                && history != null && !history.isEmpty()
                && detectModule(effectiveMessage) != null
                && (hasTimeExpression(effectiveMessage)
                    || effectiveMessage.contains("全部") || effectiveMessage.contains("所有"));

        String sid = resolveSession(sessionId, user, effectiveMessage);

        String systemPrompt = buildSystemPrompt(user);
        if (clarify) {
            // 用户点选了上一轮回答下方的选项：机制性引导模型按点选内容直接行动，不再反问
            systemPrompt += "\n\n[重要] 用户刚刚点选了你上一条回答下方的选项。"
                    + "若点选内容是范围/条件的确认或切换，立即按新条件调用工具查询并给出结果；"
                    + "若点选内容是否定（如\"不是\"），用【选项】列出具体的备选让用户继续点选；"
                    + "除此之外不要再提问。";
        }

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        appendHistory(messages, history);
        // 澄清回答：小模型对长系统提示不敏感，把禁令直接附在用户消息里约束力最强；
        // 仅影响发给GLM的内容，落库仍保存用户原话
        String glidUserContent = effectiveMessage;
        if (clarify) {
            glidUserContent = effectiveMessage + "（这是用户对上一条回答所附选项的点选：确认或切换范围就直接查询，"
                    + "否定就列出具体备选选项）";
        } else if (yesFollowUp) {
            // 对上一条建议/确认式提问的极短回答：注入执行指令，防止模型再反问或丢上下文
            if (yesReply) {
                glidUserContent = effectiveMessage + "（用户用极短的\"是\"回应了你上一条回答中的建议或提问："
                        + "请立即按上一条最相关的建议条件调用工具查询并给出结果，禁止再提问或要求确认）";
            } else {
                glidUserContent = effectiveMessage + "（用户用极短的否定回应了你上一条回答中的建议或提问："
                        + "不要执行上一条建议，改用【选项】列出2~4个具体的备选条件让用户点选）";
            }
        } else if (ellipsisFollowUp) {
            // 省略式追问(如"那任务呢""会议呢")：小模型此时最容易跳过工具直接编造，机制性强注入
            glidUserContent = effectiveMessage + "（这是省略式追问：请从之前的对话上下文继承时间等查询条件，"
                    + "把本句话补全成完整查询后调用工具获取真实数据，严禁编造）";
        } else if (standaloneQuery) {
            // 独立新查询：条件以本条消息为准，切断对上文筛选条件的继承；
            // 上文里的任务列表已过时，必须重新调工具，防止模型直接复述上文列表
            glidUserContent = effectiveMessage + "（这是独立的新查询：只使用本条消息明确给出的条件，"
                    + "不要继承上文的人员/科室/状态等筛选条件；上文出现过的列表数据已过时，"
                    + "必须重新调用工具查询，禁止直接复述上文内容）";
        } else if (conditionQuery) {
            // 已带人员/科室/状态等条件的查询：时间缺省为全部，直接查，不要再问时间
            glidUserContent = effectiveMessage + "（该查询已带具体条件：时间缺省为全部时间，"
                    + "直接调用工具查询，禁止再向用户确认时间范围）";
        }
        // "最近/近期"不带量词时默认=最近7天，直接给出换算好的日期，杜绝模型再反问时间
        if (hasBareRecentWord(effectiveMessage)) {
            LocalDate today = LocalDate.now();
            glidUserContent += "（本消息中\"最近\"指最近7天：" + today.minusDays(6) + "至" + today
                    + "，直接按此时间范围查询，禁止再向用户确认时间）";
        }
        messages.add(new JSONObject().set("role", "user").set("content", glidUserContent));

        String answer = null;
        // 本轮查询涉及的详情条目：{type: plan|meeting|works, id, name, data完整对象}
        // 随done事件下发，前端把回答中的名称渲染成可点击链接并按type跳对应详情页
        List<JSONObject> turnDetails = new ArrayList<>();
        // 人名是否经过了语音错字自动纠错：用于机制性补上"识别结果是否符合"确认选项
        boolean nameCorrected = false;
        // 本轮实际调用工具的次数：为0却给出列表式"数据"即为编造
        int toolsCalled = 0;
        // 是否已注入过"请重新调用工具"的纠错重试（每轮对话最多一次）
        boolean retrievalNudgeUsed = false;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JSONObject respMsg = callChatCompletions(messages);
            if (respMsg.getStr("content") == null) {
                respMsg.set("content", "");
            }
            JSONArray toolCalls = respMsg.getJSONArray("tool_calls");
            // 没有工具调用，说明模型已给出最终回答
            if (toolCalls == null || toolCalls.isEmpty()) {
                answer = respMsg.getStr("content");
                // 查询类问题却一次工具都没调：模型跳过工具直接复述/编造了列表。
                // 机制性纠错——保留原回答，注入纠错指令重问一次，把模型拉回工具调用；
                // 仅重试一次，仍不调用才交给防幻觉兜底判死
                if (toolsCalled == 0 && !retrievalNudgeUsed
                        && looksLikeDataRequest(effectiveMessage, clarify, history)) {
                    retrievalNudgeUsed = true;
                    messages.add(respMsg);
                    messages.add(new JSONObject().set("role", "user").set("content",
                            "你上一条回答没有调用任何查询工具，其中的列表内容不是本次查询的真实结果"
                                    + "（可能是复述了上文或凭印象生成）。请重新调用工具查询真实数据后作答；"
                                    + "如果无法查询，请如实说明，不要给出任何任务/会议/工作列表。"));
                    answer = null;
                    continue;
                }
                break;
            }
            // 回传 assistant 的工具调用消息，再逐个执行并把结果以 role=tool 回传
            messages.add(respMsg);
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject call = toolCalls.getJSONObject(i);
                String fnName = call.getJSONObject("function").getStr("name");
                String fnArgs = call.getJSONObject("function").getStr("arguments", "{}");
                // 推送思考步骤，让前端实时展示"思考过程"
                if (sink != null) {
                    sink.accept(JSONUtil.createObj()
                            .set("type", "step")
                            .set("content", describeTool(fnName, fnArgs))
                            .toString());
                }
                String result;
                try {
                    result = executeTool(fnName, fnArgs, turnDetails);
                    toolsCalled++;
                    if ("getUsers".equals(fnName) && result != null && result.contains("已自动匹配最相似的")) {
                        nameCorrected = true;
                    }
                } catch (Exception e) {
                    // 不把原始异常信息回传给模型，避免泄露内部实现
                    log.error("AI工具执行失败", e);
                    result = JSONUtil.createObj().set("error", "查询失败，请提示用户稍后重试或缩小查询范围").toString();
                }
                messages.add(new JSONObject()
                        .set("role", "tool")
                        .set("tool_call_id", call.getStr("id"))
                        .set("content", result));
            }
            if (sink != null) {
                sink.accept(JSONUtil.createObj()
                        .set("type", "step")
                        .set("content", "查询完成，正在汇总结果…")
                        .toString());
            }
        }
        if (ObjectUtil.isEmpty(answer)) {
            answer = "这个问题需要查询的数据较多，处理超时。请缩小范围后重试（例如指定时间或状态）。";
        }
        // 防幻觉兜底：本轮一次工具都没调用，但回答里出现列表式"数据"（编号清单/名称：字段）→ 判定编造，
        // 替换为诚实引导——宁可说没查到，也不放编造的内容给用户
        boolean fabricated = toolsCalled == 0 && looksLikeFabricatedData(answer);
        if (fabricated) {
            answer = "抱歉，这个问题我没有查到真实数据，不能凭空回答。请点选下方选项，或换个说法"
                    + "（如\"查本周的任务\"\"这周有什么会议\"）：";
        }
        // 摘出【选项】标记：展示文本中去掉，作为结构化选项推给前端渲染成可点击按钮。
        // 模型不一定遵守"单独一行"的指令，所以按行内任意位置出现都切分
        List<String> options = new ArrayList<>();
        StringBuilder kept = new StringBuilder();
        for (String line : answer.split("\n")) {
            int idx = line.indexOf("【选项】");
            if (idx < 0) {
                kept.append(line).append('\n');
                continue;
            }
            String before = line.substring(0, idx).trim();
            if (!before.isEmpty()) {
                kept.append(before).append('\n');
            }
            for (String o : line.substring(idx + "【选项】".length()).split("\\|")) {
                String opt = o.trim();
                if (!opt.isEmpty() && options.size() < 10) {
                    options.add(opt);
                }
            }
        }
        String displayAnswer = kept.toString().trim();
        // 正文被剥离干净(模型只输出了【选项】行)时补引导语，避免空气泡
        if (ObjectUtil.isEmpty(displayAnswer) && !options.isEmpty()) {
            displayAnswer = "请点选下方选项：";
        }
        // 机制兜底：模型违规把科室/人名清单罗列进正文(不用【选项】标记)时，
        // 自动抽取清单词条为结构化选项(前端渲染成分行的可点胶囊)，并把罗列文字从正文剥离
        if (options.isEmpty()) {
            JSONObject salvage = salvageListedOptions(displayAnswer, user);
            if (salvage != null) {
                options.addAll(salvage.getJSONArray("options").toList(String.class));
                displayAnswer = salvage.getStr("answer");
            }
        }
        // 人名被自动纠错但模型没附确认选项时，机制性补上"识别结果是否符合"追问
        // （点"不是这个人"会走clarifyReply，模型按规则列出相近人选供点选）
        if (nameCorrected && options.isEmpty()) {
            options.add("对的，就查这个人");
            options.add("不是这个人");
        }
        // 编造回答被替换后，附上标准模块选项引导用户重新表达
        if (fabricated) {
            options.clear();
            options.add("查任务");
            options.add("查会议");
            options.add("查重点工作");
        }
        // 模型以"是否/要不要/…吗?"确认式问句结尾却没附选项：机制性补上确认按钮，
        // 用户点选走clarify通路；手输"是/不用了"由yesFollowUp预处理兜底
        if (!fabricated && options.isEmpty() && endsWithConfirmationQuestion(displayAnswer)) {
            options.add("是的，就这样查");
            options.add("不用了");
        }
        // 本轮条目按类型+id去重后随结果下发（含完整对象，前端按type跳对应详情页）
        Map<String, JSONObject> itemMap = new HashMap<>();
        for (JSONObject item : turnDetails) {
            if (item == null || ObjectUtil.isEmpty(item.getStr("id"))) {
                continue;
            }
            String key = item.getStr("type") + ":" + item.getStr("id");
            if (!itemMap.containsKey(key)) {
                itemMap.put(key, item);
            }
        }
        JSONArray turnTaskArr = new JSONArray();
        turnTaskArr.addAll(itemMap.values());
        // 问答落库（条目JSON+选项JSON一并落库，历史恢复时链接和按钮都可用）
        JSONObject turn = saveTurn(sid, effectiveMessage, displayAnswer, turnTaskArr, options);
        return JSONUtil.createObj()
                .set("sessionId", sid)
                .set("answer", displayAnswer)
                .set("options", options)
                .set("tasks", turnTaskArr)
                .set("userMsgId", turn.getStr("userMsgId"))
                .set("aiMsgId", turn.getStr("aiMsgId"));
    }

    /**
     * 把工具调用翻译成用户能看懂的思考步骤文案
     */
    private String describeTool(String name, String argsJson) {
        JSONObject args;
        try {
            args = JSONUtil.parseObj(ObjectUtil.isEmpty(argsJson) ? "{}" : argsJson);
        } catch (Exception e) {
            args = new JSONObject();
        }
        switch (name) {
            case "getUsers":
                String keyword = args.getStr("keyword");
                return ObjectUtil.isEmpty(keyword)
                        ? "解析负责人信息…"
                        : "正在把「" + keyword + "」解析为系统用户…";
            case "getTasks":
                return "按条件检索任务列表…";
            case "getMeetings":
                return ObjectUtil.isNotEmpty(args.getStr("date"))
                        ? "检索当天的会议安排…"
                        : "按条件检索会议安排…";
            case "getWorks":
                return "按条件检索重点工作…";
            default:
                return "调用 " + name + " 工具…";
        }
    }

    // ==================== 历史会话 ====================

    /**
     * 校验并返回会话ID：sessionId 有效且属于当前用户则复用，否则新建会话
     */
    private String resolveSession(String sessionId, User user, String firstMessage) {
        if (ObjectUtil.isNotEmpty(sessionId)) {
            AiChatSession exist = aiChatSessionMapper.getByIdAndUid(sessionId, user.getId());
            if (exist != null) {
                return sessionId;
            }
        }
        AiChatSession session = new AiChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setUid(user.getId());
        session.setTitle(ObjectUtil.isEmpty(firstMessage) ? "新对话"
                : firstMessage.substring(0, Math.min(20, firstMessage.length())));
        aiChatSessionMapper.insert(session);
        return session.getId();
    }

    private JSONObject saveTurn(String sid, String question, String answer,
                                JSONArray turnTasks, List<String> turnOptions) {
        List<AiChatMessage> msgs = new ArrayList<>();
        AiChatMessage q = new AiChatMessage();
        q.setId(UUID.randomUUID().toString());
        q.setSid(sid);
        q.setRole("user");
        q.setContent(question);
        msgs.add(q);
        AiChatMessage a = new AiChatMessage();
        a.setId(UUID.randomUUID().toString());
        a.setSid(sid);
        a.setRole("assistant");
        a.setContent(answer);
        // 条目JSON随回答落库：历史会话恢复后前端仍能把任务名渲染成跳转链接
        if (turnTasks != null && !turnTasks.isEmpty()) {
            a.setTasks(turnTasks.toString());
        }
        // 选项JSON一并落库：历史会话恢复后胶囊按钮仍然可点
        if (turnOptions != null && !turnOptions.isEmpty()) {
            a.setOptions(JSONUtil.toJsonStr(turnOptions));
        }
        msgs.add(a);
        aiChatMessageMapper.insertBatch(msgs);
        return JSONUtil.createObj().set("userMsgId", q.getId()).set("aiMsgId", a.getId());
    }

    /**
     * 我的会话列表（按最近活跃倒序）
     */
    public PageInfo<AiChatSession> listSessions(Integer pageNum, Integer pageSize) {
        User user = TokenUtils.getCurrentUser();
        PageHelper.startPage(pageNum, pageSize);
        List<AiChatSession> list = aiChatSessionMapper.listByUid(user.getId());
        return PageInfo.of(list);
    }

    /**
     * 加载某个会话的全部消息（校验归属，只能看自己的）
     */
    public List<AiChatMessage> loadMessages(String sessionId) {
        User user = TokenUtils.getCurrentUser();
        AiChatSession session = aiChatSessionMapper.getByIdAndUid(sessionId, user.getId());
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        return aiChatMessageMapper.listBySid(sessionId);
    }

    /**
     * 删除会话：软删会话，并物理删除消息内容
     */
    public void deleteSession(String sessionId) {
        User user = TokenUtils.getCurrentUser();
        if (aiChatSessionMapper.softDelete(sessionId, user.getId()) > 0) {
            aiChatMessageMapper.deleteBySid(sessionId);
        }
    }

    /**
     * 删除一轮问答：以该轮用户消息(mid)为起点，删除它及其后到下一轮用户消息之前的全部消息（物理删除）。
     * 删完后若会话已无任何消息，则连会话一起软删，不留空会话。
     */
    public void deleteMessageRound(String sessionId, String messageId) {
        User user = TokenUtils.getCurrentUser();
        AiChatSession session = aiChatSessionMapper.getByIdAndUid(sessionId, user.getId());
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        AiChatMessage target = aiChatMessageMapper.getById(messageId);
        if (target == null || !sessionId.equals(target.getSid())) {
            throw new IllegalArgumentException("消息不存在");
        }
        // listBySid按seq升序，从目标消息起删到下一轮user消息前
        List<AiChatMessage> all = aiChatMessageMapper.listBySid(sessionId);
        List<String> toDelete = new ArrayList<>();
        boolean started = false;
        for (AiChatMessage m : all) {
            if (!started) {
                if (m.getId().equals(messageId)) {
                    started = true;
                    toDelete.add(m.getId());
                }
                continue;
            }
            if ("user".equals(m.getRole())) {
                break;
            }
            toDelete.add(m.getId());
        }
        if (toDelete.isEmpty()) {
            throw new IllegalArgumentException("消息不存在");
        }
        aiChatMessageMapper.deleteByIds(toDelete);
        if (aiChatMessageMapper.countBySid(sessionId) == 0) {
            aiChatSessionMapper.softDelete(sessionId, user.getId());
        }
    }

    // ==================== 意图门控与防幻觉辅助 ====================

    /**
     * 意图门控：识别消息中的模块与时间。
     * 返回 null 表示条件充分（或消息复杂），放行走模型；否则返回标准问句+标准选项。
     */
    /**
     * 筛选清单机制化：用户点选"按科室筛选/按负责人筛选"类选项时，
     * 后端直接给出科室名单或"真实有任务的负责人"名单作为可点选项，不经过模型。
     * 返回null表示消息不是筛选清单请求。
     */
    private JSONObject buildFilterOptions(String message, User user) {
        if (ObjectUtil.isEmpty(message)) {
            return null;
        }
        // "缩小(查询)范围"：返回缩小方式菜单。之前靠模型自觉列菜单，行为不稳定——
        // 有时把原条件原样重查一遍(等于没缩小)，机制化后确定返回方式清单
        boolean wantShrink = message.length() <= 12 && message.contains("缩小");
        boolean wantDept = message.contains("科室") || message.contains("部门");
        boolean wantHead = !wantDept && (message.contains("负责人") || message.contains("经手"));
        boolean wantStatus = !wantDept && !wantHead && message.contains("状态");
        // 时间筛选：模型偶尔生成"按时间范围筛选"选项，机制化给出时间范围(查询时继承上文人员等条件)
        boolean wantTime = !wantDept && !wantHead && !wantStatus && message.contains("时间");
        if (!wantShrink && !wantDept && !wantHead && !wantStatus && !wantTime) {
            return null;
        }
        JSONArray opts = new JSONArray();
        String question;
        if (wantShrink) {
            opts.add("按科室筛选");
            opts.add("按负责人筛选");
            opts.add("按状态筛选");
            opts.add("按时间范围筛选");
            question = "请选择缩小范围的方式（也可以直接说出条件，如\"计算机系的任务\"）：";
        } else if (wantTime) {
            opts.add("查本周");
            opts.add("查本月");
            opts.add("查最近一个月");
            opts.add("查全部");
            question = "请选择时间范围：";
        } else if (wantDept) {
            try {
                for (Department d : departmentService.getDepartmentByTid(user.getTid())) {
                    if (opts.size() >= 10) break;
                    if (d != null && ObjectUtil.isNotEmpty(d.getName())) {
                        opts.add(d.getName());
                    }
                }
            } catch (Exception e) {
                log.warn("筛选清单-科室加载失败: {}", e.getMessage());
            }
            question = "请选择要筛选的科室：";
        } else if (wantStatus) {
            // 状态字典是平铺型：每个取值自己就是根节点(field=status)
            try {
                for (SysDict root : sysdictService.getAllSysdicByTid(user.getTid())) {
                    if (opts.size() >= 10) break;
                    if (root == null || !"status".equals(root.getField())
                            || ObjectUtil.isEmpty(root.getValue())) {
                        continue;
                    }
                    opts.add(root.getValue());
                }
            } catch (Exception e) {
                log.warn("筛选清单-状态加载失败: {}", e.getMessage());
            }
            question = "请选择要筛选的任务状态：";
        } else {
            try {
                for (String name : planService.getTopHeadNames(user.getTid(), 10)) {
                    if (ObjectUtil.isNotEmpty(name)) {
                        opts.add(name);
                    }
                }
            } catch (Exception e) {
                log.warn("筛选清单-负责人加载失败: {}", e.getMessage());
            }
            question = "请选择负责人（也可以直接说出姓名）：";
        }
        if (opts.isEmpty()) {
            return null;
        }
        return JSONUtil.createObj().set("question", question).set("options", opts);
    }

    private JSONObject intentGate(String message) {
        String module = detectModule(message);
        boolean hasTime = hasTimeExpression(message);
        // "全部/所有"是明确的全时间范围意图，不应再追问时间
        boolean explicitAll = message.contains("全部") || message.contains("所有");
        if (module != null && !hasTime && !explicitAll) {
            // 有模块无时间：问时间范围（带具体条件的查询已在调用侧放行，不会走到这里）
            JSONArray opts = new JSONArray();
            opts.add("查本周的" + module);
            opts.add("查本月的" + module);
            opts.add("查最近一个月的" + module);
            opts.add("查全部" + module);
            return JSONUtil.createObj()
                    .set("question", "您想查询什么时间范围的" + module + "？")
                    .set("options", opts);
        }
        if (module == null && !hasTime && message.length() <= 12) {
            // 完全模糊的短消息：问模块（选项带默认时间，一步到位）
            JSONArray opts = new JSONArray();
            opts.add("查本周的任务");
            opts.add("查本周的会议");
            opts.add("查本周的重点工作");
            return JSONUtil.createObj()
                    .set("question", "您想查询哪类信息？")
                    .set("options", opts);
        }
        return null;
    }

    /**
     * 消息是否已带具体查询条件：提到本租户任何人员姓名/科室名/字典取值，或"我的"。
     * 用于意图门控——这种消息意图已明确（如"李慧秋的任务"），不该再被通用的"问时间"选项拦下，
     * 那样会把用户给出的条件丢掉。
     */
    private boolean hasSpecificCondition(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        if (message.contains("我的") || message.contains("本人")) {
            return true;
        }
        User user = TokenUtils.getCurrentUser();
        String tid = user.getTid();
        try {
            for (User u : userService.getAllUser(tid)) {
                if (u != null && ObjectUtil.isNotEmpty(u.getName()) && message.contains(u.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("门控条件检测-人员加载失败: {}", e.getMessage());
        }
        try {
            for (Department d : departmentService.getDepartmentByTid(tid)) {
                if (d != null && ObjectUtil.isNotEmpty(d.getName()) && message.contains(d.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("门控条件检测-科室加载失败: {}", e.getMessage());
        }
        try {
            for (SysDict root : sysdictService.getAllSysdicByTid(tid)) {
                if (root == null) {
                    continue;
                }
                if (ObjectUtil.isNotEmpty(root.getValue()) && message.contains(root.getValue())) {
                    return true;
                }
                if (root.getChildren() != null) {
                    for (SysDict c : root.getChildren()) {
                        if (c != null && ObjectUtil.isNotEmpty(c.getValue()) && message.contains(c.getValue())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("门控条件检测-字典加载失败: {}", e.getMessage());
        }
        return false;
    }

    /** 写操作意图识别：命中则跳过意图门控，交给模型按"仅支持查询"规则回应 */
    private boolean looksLikeWriteIntent(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        for (String verb : new String[]{"创建", "新增", "添加", "修改", "编辑", "更新", "删除", "移除",
                "取消", "导入", "导出", "分配", "布置", "安排一下", "设置"}) {
            if (message.contains(verb)) {
                return true;
            }
        }
        return false;
    }

    /** 模块关键词识别：事件/事项都归入重点工作（用户的习惯叫法） */
    private String detectModule(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        if (message.contains("重点工作") || message.contains("重要工作")
                || message.contains("事件") || message.contains("事项")) {
            return "重点工作";
        }
        if (message.contains("会议") || message.contains("开会")) {
            return "会议";
        }
        if (message.contains("任务") || message.contains("计划")) {
            return "任务";
        }
        return null;
    }

    /**
     * 是否包含不带量词的"最近/近期"（如"我最近的会议"）。
     * "最近一个月/最近三天"这类自带时长的说法不算——模型能自己换算。
     */
    private boolean hasBareRecentWord(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        return message.matches(".*最近(?![0-9一二两三四五六七八九十半]).*")
                || message.contains("近期");
    }

    /** 是否包含时间表达（相对时间词或具体日期） */
    private boolean hasTimeExpression(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        if (message.matches(".*\\d{4}\\s*[-年].*") || message.matches(".*\\d{1,2}\\s*[月号日].*")) {
            return true;
        }
        for (String w : new String[]{"今天", "明天", "昨天", "后天", "本周", "这周", "下周", "上周", "当周",
                "本月", "这个月", "上个月", "上月", "下个月", "下月", "今年", "去年", "最近", "近期", "季度", "年度"}) {
            if (message.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 数字快捷选择：从最后一条AI回答中提取编号清单，返回第n项的文本。
     * 兜住模型违规把选项写成正文编号清单的场景（此时没有可点击胶囊，用户只能回数字）。
     */
    private String pickNumberedOption(List<Map<String, Object>> history, int n) {
        if (history == null) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> m = history.get(i);
            if (!"assistant".equals(m.get("role"))) {
                continue;
            }
            String content = m.get("content") == null ? "" : String.valueOf(m.get("content"));
            List<String> numbered = new ArrayList<>();
            for (String line : content.split("\n")) {
                java.util.regex.Matcher mm = java.util.regex.Pattern
                        .compile("^([1-9])[.、．)]\\s*(\\S.*)$").matcher(line.trim());
                if (mm.matches()) {
                    numbered.add(mm.group(2).trim());
                }
            }
            if (n >= 1 && n <= numbered.size()) {
                return numbered.get(n - 1);
            }
            return null;
        }
        return null;
    }

    /**
     * 机制兜底：检测回答正文是否为科室/人名清单的罗列(模型违规未用【选项】标记)。
     * 命中条件：短回答(≤200字，正常任务列表都远长于此)、含≥3个租户科室名/人名、
     * 且这些词占据正文主体；满足则抽取为结构化选项并剥离正文中的罗列文字。
     * 返回 {options, answer}，不满足返回null。长回答(正常任务列表)不会被误伤。
     */
    private JSONObject salvageListedOptions(String answer, User user) {
        if (ObjectUtil.isEmpty(answer) || answer.length() > 200) {
            return null;
        }
        // 候选词条：科室名+人名，长词优先匹配(避免长名称被子串词抢先命中)
        List<String> words = new ArrayList<>();
        try {
            for (Department d : departmentService.getDepartmentByTid(user.getTid())) {
                if (d != null && ObjectUtil.isNotEmpty(d.getName())) {
                    words.add(d.getName());
                }
            }
        } catch (Exception e) {
            log.warn("选项兜底-科室加载失败: {}", e.getMessage());
        }
        try {
            for (User u : userService.getAllUser(user.getTid())) {
                if (u != null && ObjectUtil.isNotEmpty(u.getName())) {
                    words.add(u.getName());
                }
            }
        } catch (Exception e) {
            log.warn("选项兜底-人员加载失败: {}", e.getMessage());
        }
        if (words.isEmpty()) {
            return null;
        }
        words.sort((a, b) -> b.length() - a.length());
        List<String> hits = new ArrayList<>();
        String masked = answer;
        for (String w : words) {
            if (masked.contains(w)) {
                hits.add(w);
                masked = masked.replace(w, "\u0001"); // 已命中部分占位，防止子串重复命中
            }
        }
        if (hits.size() < 3) {
            return null;
        }
        // 命中词须占正文主体(剔除标点空白数字后过半)，防止误伤正常叙述里的零星人名/科室名
        String stripped = answer.replaceAll("[\\s\\p{Punct}，。、；：？！（）()【】\\-\\*0-9]", "");
        int hitLen = 0;
        for (String h : hits) {
            hitLen += h.length();
        }
        if (hitLen * 2 <= stripped.length()) {
            return null;
        }
        // 按首次出现顺序排列，上限10个
        hits.sort((a, b) -> answer.indexOf(a) - answer.indexOf(b));
        if (hits.size() > 10) {
            hits = new ArrayList<>(hits.subList(0, 10));
        }
        // 清理正文：逐行剥离清单词，删除只剩标点/数字/空白的行，保留引导句
        StringBuilder kept = new StringBuilder();
        for (String line : answer.split("\n")) {
            String cleaned = line;
            for (String h : hits) {
                cleaned = cleaned.replace(h, "");
            }
            if (cleaned.replaceAll("[\\s\\p{Punct}，。、；：？！（）()【】\\-\\*0-9]", "").isEmpty()) {
                continue;
            }
            kept.append(cleaned.replaceAll("[\\s、，,|]+$", "").trim()).append('\n');
        }
        String cleanedAnswer = kept.toString().trim();
        if (ObjectUtil.isEmpty(cleanedAnswer)) {
            cleanedAnswer = "请点选下方选项：";
        }
        return JSONUtil.createObj().set("options", hits).set("answer", cleanedAnswer);
    }

    /** 判断回答是否像"列表式数据"（配合本轮零工具调用即判定为编造） */
    private boolean looksLikeFabricatedData(String answer) {
        if (ObjectUtil.isEmpty(answer)) {
            return false;
        }
        if (answer.contains("任务名称：") || answer.contains("会议名称：")
                || answer.contains("工作名称：") || answer.contains("事件名称：")) {
            return true;
        }
        int numbered = 0;
        for (String line : answer.split("\n")) {
            if (line.trim().matches("^[1-9][.、．)].*")) {
                numbered++;
            }
        }
        if (numbered >= 2) {
            return true;
        }
        // 单行压缩列表（"1. xxx  2. xxx"挤成一行）：按行内编号标记计数，
        // 门槛4个以上以避开"2026.5"之类日期或正常行文中的偶然编号
        int inline = 0;
        Matcher m = INLINE_NUMBERED.matcher(answer);
        while (m.find()) {
            inline++;
        }
        return inline >= 4;
    }

    /** 编号列表的行内模式："空格+1~2位数字+点/顿号"，用于识别单行压缩列表 */
    private static final Pattern INLINE_NUMBERED = Pattern.compile("\\s[1-9][0-9]?[.、．]");

    /**
     * 上一轮AI回答是筛选菜单("请选择…"开头)且本条是短消息：这是对菜单的点选/手输
     * (如纯人名"高贵芳"、科室名)，条件意图已明确，不该被通用意图门控拦下问"查哪类信息"。
     */
    private boolean menuFollowUp(String message, List<Map<String, Object>> history) {
        if (message == null || message.length() > 12 || history == null || history.isEmpty()) {
            return false;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> m = history.get(i);
            if (!"assistant".equals(m.get("role"))) {
                continue;
            }
            String c = m.get("content") == null ? "" : String.valueOf(m.get("content")).trim();
            return c.startsWith("请选择");
        }
        return false;
    }

    /**
     * 判断用户这条消息是否是数据查询类（零工具调用却给出数据时才值得纠错重试；闲聊不重试）。
     * 覆盖三类：胶囊点选(必为查询/筛选意图)；筛选菜单后的短消息点选/手输
     * (如纯人名"高贵芳"，不含任何查询关键词)；以及带查询词或业务模块的消息。
     */
    private boolean looksLikeDataRequest(String message, boolean clarify, List<Map<String, Object>> history) {
        if (ObjectUtil.isEmpty(message)) {
            return false;
        }
        boolean yesNoConfirm = isYesNoReply(message) && history != null && !history.isEmpty();
        if (clarify || menuFollowUp(message, history) || yesNoConfirm) {
            return true;
        }
        return detectModule(message) != null
                || message.contains("查") || message.contains("列出") || message.contains("有哪些")
                || message.contains("多少") || message.contains("看看") || message.contains("列表");
    }

    // ==================== 是非确认跟进 ====================

    /** 极短肯定回答词表（对AI上一条确认式提问的回复） */
    private static final Set<String> YES_WORDS = new HashSet<>(Arrays.asList(
            "是", "是的", "对", "对的", "好", "好的", "好吧", "可以", "行", "嗯", "嗯嗯",
            "要", "确认", "没错", "ok", "OK", "Ok", "就这样", "查吧", "可以查"));
    /** 极短否定回答词表（注意"算了/不用了"以"了"结尾，须先于省略式追问识别） */
    private static final Set<String> NO_WORDS = new HashSet<>(Arrays.asList(
            "不", "不是", "不用", "不要", "不用了", "不要了", "算了", "否", "没有", "没有了", "错了", "不查"));

    private boolean isYesNoReply(String message) {
        String m = message == null ? "" : message.trim();
        return YES_WORDS.contains(m) || NO_WORDS.contains(m);
    }

    /** 文本尾部(30字内)是否含确认式问句标志 */
    private boolean endsWithConfirmationQuestion(String text) {
        if (ObjectUtil.isEmpty(text)) {
            return false;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return false;
        }
        String tail = t.substring(Math.max(0, t.length() - 30));
        return tail.contains("是否") || tail.contains("要不要") || tail.contains("是不是")
                || tail.contains("吗？") || tail.contains("吗?") || tail.contains("行吗");
    }

    // ==================== 语音识别热词 ====================

    /** 热词缓存：tid -> (词表, 过期时间)，避免每次语音识别都查库 */
    private final Map<String, AbstractMap.SimpleEntry<List<String>, Long>> hotwordCache = new HashMap<>();

    /**
     * 构建当前租户的ASR热词表：字典值(状态/类型/重要程度/性质) + 科室名 + 人名。
     * 优先级：字典值和科室名在前(查询高频且数量少)，人名补足剩余名额，共100个上限。
     */
    public List<String> buildAsrHotwords() {
        User user = TokenUtils.getCurrentUser();
        String tid = user.getTid();
        long now = System.currentTimeMillis();
        synchronized (hotwordCache) {
            AbstractMap.SimpleEntry<List<String>, Long> cached = hotwordCache.get(tid);
            if (cached != null && now < cached.getValue()) {
                return cached.getKey();
            }
        }
        Set<String> words = new LinkedHashSet<>();
        try {
            collectDictWords(sysdictService.getAllSysdicByTid(tid), words);
        } catch (Exception e) {
            log.warn("热词-加载字典失败: {}", e.getMessage());
        }
        try {
            for (Department d : departmentService.getDepartmentByTid(tid)) {
                if (words.size() >= MAX_HOTWORDS) break;
                if (d != null && ObjectUtil.isNotEmpty(d.getName())) {
                    words.add(d.getName());
                }
            }
        } catch (Exception e) {
            log.warn("热词-加载科室失败: {}", e.getMessage());
        }
        try {
            for (User u : userService.getAllUser(tid)) {
                if (words.size() >= MAX_HOTWORDS) break;
                if (u != null && ObjectUtil.isNotEmpty(u.getName())) {
                    words.add(u.getName());
                }
            }
        } catch (Exception e) {
            log.warn("热词-加载用户失败: {}", e.getMessage());
        }
        List<String> list = new ArrayList<>(words);
        synchronized (hotwordCache) {
            hotwordCache.put(tid, new AbstractMap.SimpleEntry<>(list, now + HOTWORD_TTL_MS));
        }
        log.debug("ASR热词已构建({}个): {}", list.size(), list);
        return list;
    }

    /** 收集字典树的词(name与value都收，子节点优先) */
    private void collectDictWords(List<SysDict> roots, Set<String> words) {
        if (roots == null) {
            return;
        }
        for (SysDict root : roots) {
            if (words.size() >= MAX_HOTWORDS) {
                return;
            }
            if (ObjectUtil.isNotEmpty(root.getValue())) {
                words.add(root.getValue());
            }
            List<SysDict> children = root.getChildren();
            if (children != null) {
                for (SysDict c : children) {
                    if (words.size() >= MAX_HOTWORDS) {
                        return;
                    }
                    if (ObjectUtil.isNotEmpty(c.getValue())) {
                        words.add(c.getValue());
                    }
                    if (ObjectUtil.isNotEmpty(c.getName()) && !c.getName().equals(c.getValue())) {
                        words.add(c.getName());
                    }
                }
            }
        }
    }

    // ==================== 工具实现 ====================

    private String executeTool(String name, String argsJson, List<JSONObject> detailCollector) {
        JSONObject args;
        try {
            args = JSONUtil.parseObj(ObjectUtil.isEmpty(argsJson) ? "{}" : argsJson);
        } catch (Exception e) {
            args = new JSONObject();
        }
        switch (name) {
            case "getTasks":
                return toolGetTasks(args, detailCollector);
            case "getMeetings":
                return toolGetMeetings(args, detailCollector);
            case "getWorks":
                return toolGetWorks(args, detailCollector);
            case "getUsers":
                return toolGetUsers(args);
            default:
                return JSONUtil.createObj().set("error", "未知工具: " + name).toString();
        }
    }

    /**
     * 查询任务列表（复用 searchPlanByCondition 与现有数据范围控制）
     * detailCollector：同时收集原始对象，供前端"点击名称跳详情"使用（不进模型上下文）
     */
    private String toolGetTasks(JSONObject args, List<JSONObject> detailCollector) {
        User user = TokenUtils.getCurrentUser();
        Plan plan = new Plan();
        plan.setTid(user.getTid()); // 只查本租户数据
        String tid = user.getTid();
        if (args.containsKey("keyword")) plan.setName(args.getStr("keyword"));
        if (args.containsKey("statusId")) plan.setStatus(resolveDictId(tid, "status", args.getStr("statusId")));
        if (args.containsKey("typeId")) plan.setType(resolveDictId(tid, "type", args.getStr("typeId")));
        if (args.containsKey("impId")) plan.setImp(resolveDictId(tid, "imp", args.getStr("impId")));
        if (args.containsKey("attributeId")) plan.setAttribute(resolveDictId(tid, "attribute", args.getStr("attributeId")));
        if (args.containsKey("headName")) plan.setHead(resolveHeadId(tid, args.getStr("headName")));
        else if (args.containsKey("headId")) plan.setHead(args.getStr("headId"));
        if (args.containsKey("departmentId")) plan.setDid(resolveDepartmentId(tid, args.getStr("departmentId")));
        if (args.getBool("mine", false)) plan.setUid(user.getId());
        if (args.containsKey("startTime")) plan.setStartTime(args.getStr("startTime"));
        if (args.containsKey("endTime")) plan.setEndTime(args.getStr("endTime"));

        int page = args.getInt("page", 1);
        if (page < 1) page = 1;
        int pageSize = args.getInt("pageSize", 10);
        if (pageSize < 1) pageSize = 10;
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

        // 与 /plan/searchPlanByConditionPage 完全一致的数据范围控制
        AuthCheckUtils.applyPlanQueryScope(plan);

        PageInfo<Plan> pageInfo = planService.getPlanPage(plan, page, pageSize);
        // 数据兼容：部分历史数据的head字段存的是负责人姓名而非用户ID，按ID查不到时用姓名再查一次
        if (pageInfo.getTotal() == 0 && ObjectUtil.isNotEmpty(plan.getHead())) {
            User headUser = userMapper.getUserInfo(plan.getHead());
            if (headUser != null && ObjectUtil.isNotEmpty(headUser.getName())) {
                plan.setHead(headUser.getName());
                pageInfo = planService.getPlanPage(plan, page, pageSize);
            }
        }

        // head 字段存的是用户ID，批量解析成姓名便于展示
        Map<String, String> headNames = new HashMap<>();
        for (Plan p : pageInfo.getList()) {
            String headId = p.getHead();
            if (ObjectUtil.isNotEmpty(headId) && !headNames.containsKey(headId)) {
                User headUser = userMapper.getUserInfo(headId);
                headNames.put(headId,
                        headUser == null || ObjectUtil.isEmpty(headUser.getName()) ? headId : headUser.getName());
            }
        }

        JSONArray tasks = new JSONArray();
        for (Plan p : pageInfo.getList()) {
            if (detailCollector != null) {
                detailCollector.add(new JSONObject()
                        .set("type", "plan")
                        .set("id", p.getId())
                        .set("name", p.getName())
                        .set("data", JSONUtil.parseObj(p)));
            }
            tasks.add(new JSONObject()
                    .set("id", p.getId())
                    .set("name", p.getName())
                    .set("status", p.getStatusValue())
                    .set("importance", p.getImpValue())
                    .set("type", p.getTypeValue())
                    .set("head", headNames.getOrDefault(p.getHead(), p.getHead()))
                    .set("creator", p.getUsername())
                    .set("department", p.getDepartment())
                    .set("startTime", p.getStartTime())
                    .set("endTime", p.getEndTime()));
        }
        JSONObject result = JSONUtil.createObj()
                .set("total", pageInfo.getTotal())
                .set("page", page)
                .set("tasks", tasks);
        if (pageInfo.getTotal() == 0) {
            result.set("note", "未查询到符合条件的任务。请分析最可能导致无结果的条件"
                    + "（常见：负责人姓名有误、时间范围过窄、状态理解偏差），"
                    + "向用户说明并给出放宽建议，必要时用放宽后的条件重查一次");
        }
        if (pageInfo.getTotal() > tasks.size()) {
            result.set("note", "共" + pageInfo.getTotal() + "条，仅返回当前" + tasks.size()
                    + "条，可翻页(page参数)或提示用户缩小范围");
        }
        return result.toString();
    }

    /**
     * 查询会议安排（复用现有会议查询；date按天查时自动兼容单日/多日会议）
     */
    private String toolGetMeetings(JSONObject args, List<JSONObject> detailCollector) {
        User user = TokenUtils.getCurrentUser();
        int page = Math.max(1, args.getInt("page", 1));
        int pageSize = args.getInt("pageSize", 10);
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        String date = args.getStr("date");
        String startTime = args.getStr("startTime");
        String endTime = args.getStr("endTime");

        List<Meeting> meetings;
        long total;
        if (ObjectUtil.isNotEmpty(date)) {
            // 按天查：mapper已智能处理（单日会议=当天开始；多日会议=区间覆盖当天）
            Meeting query = baseMeetingQuery(args, user.getTid());
            query.setStartDate(date);
            PageInfo<Meeting> pi = meetingService.getMeetingByConditionPage(query, page, pageSize);
            meetings = pi.getList();
            total = pi.getTotal();
        } else if (ObjectUtil.isNotEmpty(startTime) && ObjectUtil.isNotEmpty(endTime)) {
            // 范围查：租户分页遍历(上限10页)，Java侧按日期重叠过滤
            List<Meeting> filtered = new ArrayList<>();
            for (int p = 1; p <= 10; p++) {
                PageInfo<Meeting> pi = meetingService.getMeetingByTenantPage(user.getTid(), p, MAX_PAGE_SIZE);
                List<Meeting> batch = pi.getList();
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                for (Meeting m : batch) {
                    if (m == null || ObjectUtil.isEmpty(m.getStartDate())) {
                        continue;
                    }
                    boolean overlap = m.getStartDate().compareTo(endTime) <= 0
                            && (ObjectUtil.isEmpty(m.getEndDate())
                            ? m.getStartDate().compareTo(startTime) >= 0
                            : m.getEndDate().compareTo(startTime) >= 0);
                    if (overlap) {
                        filtered.add(m);
                    }
                }
                if (p * MAX_PAGE_SIZE >= pi.getTotal()) {
                    break;
                }
            }
            total = filtered.size();
            int from = (page - 1) * pageSize;
            meetings = from >= filtered.size()
                    ? new ArrayList<>()
                    : new ArrayList<>(filtered.subList(from, Math.min(filtered.size(), from + pageSize)));
        } else {
            Meeting query = baseMeetingQuery(args, user.getTid());
            PageInfo<Meeting> pi = meetingService.getMeetingByConditionPage(query, page, pageSize);
            meetings = pi.getList();
            total = pi.getTotal();
        }

        JSONArray arr = new JSONArray();
        for (Meeting m : meetings) {
            if (m == null) {
                continue;
            }
            if (detailCollector != null) {
                detailCollector.add(new JSONObject()
                        .set("type", "meeting")
                        .set("id", m.getId())
                        .set("name", m.getMeetingName())
                        .set("data", JSONUtil.parseObj(m)));
            }
            String dateText = ObjectUtil.isEmpty(m.getEndDate()) || m.getEndDate().equals(m.getStartDate())
                    ? m.getStartDate() : m.getStartDate() + "至" + m.getEndDate();
            String timeText = ObjectUtil.isEmpty(m.getStartStartTime()) ? ""
                    : m.getStartStartTime() + (ObjectUtil.isEmpty(m.getEndEndTime()) ? "" : "-" + m.getEndEndTime());
            arr.add(new JSONObject()
                    .set("name", m.getMeetingName())
                    .set("date", dateText)
                    .set("time", timeText)
                    .set("status", ObjectUtil.isNotEmpty(m.getStatusValue()) ? m.getStatusValue() : m.getStatus())
                    .set("host", m.getHost())
                    .set("location", m.getLocation()));
        }
        JSONObject result = JSONUtil.createObj()
                .set("total", total)
                .set("page", page)
                .set("meetings", arr);
        if (total == 0) {
            result.set("note", "未查询到符合条件的会议。请分析原因（时间范围过窄、关键词不对）并向用户说明，可放宽条件重查");
        }
        if (total > arr.size()) {
            result.set("note", "共" + total + "场，仅返回当前" + arr.size() + "条，可翻页(page)或提示用户缩小范围");
        }
        return result.toString();
    }

    /** 会议查询的公共条件（关键词/主持人） */
    private Meeting baseMeetingQuery(JSONObject args, String tid) {
        Meeting query = new Meeting();
        query.setTid(tid);
        if (ObjectUtil.isNotEmpty(args.getStr("keyword"))) {
            query.setMeetingName(args.getStr("keyword"));
        }
        if (ObjectUtil.isNotEmpty(args.getStr("host"))) {
            query.setHost(args.getStr("host"));
        }
        return query;
    }

    /**
     * 查询重点工作（复用现有works查询；权限与网页端一致：非学院领导只看公开的）
     */
    private String toolGetWorks(JSONObject args, List<JSONObject> detailCollector) {
        User user = TokenUtils.getCurrentUser();
        Works query = new Works();
        query.setTid(user.getTid());
        String tid = user.getTid();
        if (args.containsKey("keyword")) {
            query.setName(args.getStr("keyword"));
        }
        if (args.containsKey("statusId")) {
            query.setStatus(resolveDictId(tid, "status", args.getStr("statusId")));
        }
        if (args.containsKey("typeId")) {
            query.setType(resolveDictId(tid, "type", args.getStr("typeId")));
        }
        if (args.containsKey("headName")) {
            query.setHead(args.getStr("headName")); // works.head存的是负责人姓名，直接传姓名
        }
        if (args.containsKey("departmentId")) {
            query.setDid(resolveDepartmentId(tid, args.getStr("departmentId")));
        }
        if (args.containsKey("startTime")) {
            query.setStartTime(args.getStr("startTime"));
        }
        if (args.containsKey("endTime")) {
            query.setEndTime(args.getStr("endTime"));
        }
        // 权限对齐网页端：非学院领导默认只查公开(可见范围=公开)的重点工作
        if (!RoleEnum.COLLEGE_LEADER.code.equals(user.getRid())) {
            query.setIsopen("1");
        }

        int page = Math.max(1, args.getInt("page", 1));
        int pageSize = args.getInt("pageSize", 10);
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        PageInfo<Works> pageInfo = worksService.getWorksPage(query, page, pageSize);

        JSONArray arr = new JSONArray();
        for (Works w : pageInfo.getList()) {
            if (w == null) {
                continue;
            }
            if (detailCollector != null) {
                detailCollector.add(new JSONObject()
                        .set("type", "works")
                        .set("id", w.getId())
                        .set("name", w.getName())
                        .set("data", JSONUtil.parseObj(w)));
            }
            arr.add(new JSONObject()
                    .set("name", w.getName())
                    .set("status", ObjectUtil.isNotEmpty(w.getStatusValue()) ? w.getStatusValue() : w.getStatus())
                    .set("type", ObjectUtil.isNotEmpty(w.getTypeValue()) ? w.getTypeValue() : w.getType())
                    .set("head", w.getHead())
                    .set("department", w.getDepartment())
                    .set("startTime", w.getStartTime())
                    .set("endTime", w.getEndTime()));
        }
        JSONObject result = JSONUtil.createObj()
                .set("total", pageInfo.getTotal())
                .set("page", page)
                .set("works", arr);
        if (pageInfo.getTotal() == 0) {
            result.set("note", "未查询到符合条件的重点工作。请分析原因并向用户说明，可放宽条件重查");
        }
        if (pageInfo.getTotal() > arr.size()) {
            result.set("note", "共" + pageInfo.getTotal() + "条，仅返回当前" + arr.size() + "条，可翻页(page)或提示用户缩小范围");
        }
        return result.toString();
    }

    /**
     * 按姓名模糊查询本租户用户，用于把"张三"解析成用户ID（headId）
     * 只返回 id/name/department/role，绝不返回 password 等敏感字段
     */
    private String toolGetUsers(JSONObject args) {
        String keyword = args.getStr("keyword");
        if (ObjectUtil.isEmpty(keyword)) {
            return JSONUtil.createObj().set("error", "keyword 不能为空").toString();
        }
        User user = TokenUtils.getCurrentUser();
        User query = new User();
        query.setTid(user.getTid()); // 只查本租户用户
        query.setName(keyword);
        List<User> users = userService.getUserByCondition(query);

        if (!users.isEmpty()) {
            if (users.size() > 10) {
                users = users.subList(0, 10);
            }
            JSONArray list = new JSONArray();
            for (User u : users) {
                list.add(toUserBrief(u));
            }
            JSONObject result = JSONUtil.createObj().set("users", list).set("exactMatch", true);
            if (list.size() > 1) {
                result.set("note", "匹配到多个用户，请让用户确认是哪一位（可结合科室区分）");
            }
            return result.toString();
        }

        // 未找到：取全租户用户，按姓名字符重合度处理（语音错别字常见场景）
        User allQuery = new User();
        allQuery.setTid(user.getTid());
        List<User> similar = topSimilarUsers(userService.getUserByCondition(allQuery), keyword, 5);

        // 唯一高相似候选：自动纠正并继续查询，回答中向用户说明（确定性纠错，不依赖模型自觉）
        if (!similar.isEmpty()) {
            int topScore = nameOverlap(similar.get(0).getName(), keyword);
            int secondScore = similar.size() > 1 ? nameOverlap(similar.get(1).getName(), keyword) : 0;
            if (topScore >= 2 && topScore > secondScore) {
                User best = similar.get(0);
                return JSONUtil.createObj()
                        .set("users", new JSONArray().set(toUserBrief(best)))
                        .set("exactMatch", false)
                        .set("note", "未找到与「" + keyword + "」完全匹配的用户，已自动匹配最相似的「"
                                + best.getName() + "」。先用一句话向用户说明（可能是语音识别错字），"
                                + "然后直接用该用户查询；回答末尾附【选项】行的第一项为确认项："
                                + "对的，就查「" + best.getName() + "」|不是这个人 —— 用户点\"不是这个人\"时，"
                                + "再用【选项】列出相近人选供点选。")
                        .toString();
            }
        }

        // 多个候选：通过【选项】让用户点选，不要让用户重新打字
        JSONArray list = new JSONArray();
        for (User u : similar) {
            list.add(toUserBrief(u));
        }
        return JSONUtil.createObj()
                .set("users", list)
                .set("exactMatch", false)
                .set("note", "未找到姓名与「" + keyword + "」匹配的用户。请用【选项】行把下方最相近的2~4个姓名"
                        + "列给用户点选确认（每个姓名单独作为一个选项）；若都不合适，建议用户改用科室/时间条件查询。"
                        + "不要原样输出本提示，不要让用户重新输入姓名")
                .toString();
    }

    /** 姓名与关键词的字符重合数（与topSimilarUsers的评分口径一致） */
    private int nameOverlap(String name, String keyword) {
        if (ObjectUtil.isEmpty(name) || ObjectUtil.isEmpty(keyword)) {
            return 0;
        }
        Set<Character> kwChars = new HashSet<>();
        for (char c : keyword.toCharArray()) {
            kwChars.add(c);
        }
        int score = 0;
        for (char c : name.toCharArray()) {
            if (kwChars.contains(c)) {
                score++;
            }
        }
        return score;
    }

    /**
     * 用户信息脱敏视图（绝不返回 password 等敏感字段）
     */
    private JSONObject toUserBrief(User u) {
        return JSONUtil.createObj()
                .set("id", u.getId())
                .set("name", u.getName())
                .set("department", u.getDepartment())
                .set("role", u.getRole());
    }

    /**
     * 简单字符重合度排序，取与 keyword 相似度最高的前 n 个姓名（重合度为0的丢弃）
     */
    private List<User> topSimilarUsers(List<User> users, String keyword, int n) {
        Set<Character> kwChars = new HashSet<>();
        for (char c : keyword.toCharArray()) {
            kwChars.add(c);
        }
        List<Map.Entry<Integer, User>> scored = new ArrayList<>();
        if (users != null) {
            for (User u : users) {
                if (u == null || ObjectUtil.isEmpty(u.getName())) {
                    continue;
                }
                int score = 0;
                for (char c : u.getName().toCharArray()) {
                    if (kwChars.contains(c)) {
                        score++;
                    }
                }
                if (score > 0) {
                    scored.add(new AbstractMap.SimpleEntry<>(score, u));
                }
            }
        }
        scored.sort((a, b) -> b.getKey() - a.getKey());
        List<User> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < n; i++) {
            result.add(scored.get(i).getValue());
        }
        return result;
    }

    // ==================== GLM 调用 ====================

    private JSONObject callChatCompletions(JSONArray messages) {
        JSONObject reqBody = JSONUtil.createObj()
                .set("model", model)
                .set("messages", messages)
                .set("tools", buildToolsSchema())
                .set("temperature", 0.3);
        // GLM-4.5及以上默认开启"深度思考"(先想再答，首字延迟大)；
        // 本系统防幻觉靠机制层不靠模型思考，工具型对话直接禁用以换速度
        if (model != null && (model.contains("4.5") || model.contains("5"))) {
            reqBody.set("thinking", JSONUtil.createObj().set("type", "disabled"));
        }
        String respBody = HttpRequest.post(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(reqBody.toString())
                .timeout(60000)
                .execute().body();
        log.debug("GLM响应: {}", respBody);
        JSONObject resp = JSONUtil.parseObj(respBody);
        if (resp.containsKey("error")) {
            JSONObject err = resp.getJSONObject("error");
            throw new RuntimeException("GLM接口错误: " + (err == null ? respBody : err.getStr("message")));
        }
        JSONArray choices = resp.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("GLM返回异常: " + respBody);
        }
        return choices.getJSONObject(0).getJSONObject("message");
    }

    private void appendHistory(JSONArray messages, List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int start = Math.max(0, history.size() - MAX_HISTORY);
        for (int i = start; i < history.size(); i++) {
            Map<String, Object> item = history.get(i);
            String role = item.get("role") == null ? "" : String.valueOf(item.get("role"));
            String content = item.get("content") == null ? "" : String.valueOf(item.get("content")).trim();
            if (("user".equals(role) || "assistant".equals(role)) && ObjectUtil.isNotEmpty(content)) {
                messages.add(new JSONObject().set("role", role).set("content", content));
            }
        }
    }

    private String buildSystemPrompt(User user) {
        LocalDate today = LocalDate.now();
        String[] weeks = {"一", "二", "三", "四", "五", "六", "日"};
        return "你是任务管理系统的智能助手，帮助用户查询三类信息：任务(计划)、会议安排、重点工作。三个模块共享同一对话上下文，用户可以在同一会话里切换查询模块。\n"
                + "铁律：你回答中出现的任何任务/会议/工作数据，必须来自工具的返回结果，严禁凭空编造。"
                + "对话上下文只用于补全查询条件（如时间范围），不是数据来源；没查到就说没查到。\n"
                + "今天是 " + today + " 星期" + weeks[today.getDayOfWeek().getValue() - 1] + "。"
                + "用户提到的\"今天/明天/本周/上周/本月/下月\"等相对时间，请先按今天日期换算成 yyyy-MM-dd 再传给工具。\n\n"
                + "交互方式（最高优先级，必须遵守）：\n"
                + "1. 用户没说要查什么（模块和时间都没有，如只说\"查一下\"）→ 用一句话询问+【选项】查任务|查会议|查重点工作，不要调用工具。\n"
                + "2. 说了模块但没说时间（如只说\"查会议\"）→ 用一句话询问+【选项】查本周的会议|查本月的会议|查最近一个月的会议|查全部会议（任务/重点工作同理）。\n"
                + "3. 模块和时间都有了（\"今天/本周/本月/最近一周\"等都是明确时间，换算成日期直接查，禁止再向用户确认时间；"
                + "用户说\"最近\"\"近期\"而未带时长时，默认指最近7天）→ "
                + "立即调用工具查询并在结果后附【选项】确认：是的，就查这些|改为查本月|查全部时间。\n"
                + "上下文条件继承规则：只有当用户消息是省略式追问（如\"那任务呢\"\"他的呢\"）或明确指代上文时，"
                + "才从对话上下文继承查询条件；用户自己说出模块和时间/范围的查询是独立新查询，"
                + "只使用本条消息中的条件，禁止把上一轮的人员/科室/状态带入。\n"
                + "4. 用户点选选项后：确认或切换范围→立即查询；否定（如\"不是这个人\"）→【选项】列出具体备选；不要再提问。\n"
                + "5. 选项2~4个(让用户从科室/人名等较多备选中挑选时最多10个)；每条不超过12个字；必须是可直接执行的查询表述。"
                + "禁止在回答正文中罗列类型/状态/科室/人名清单让用户挑选，备选一律通过【选项】行用|分隔给出。"
                + "系统只有任务/会议/重点工作三个模块，禁止发明不存在的模块或查询方向。\n\n"
                + "系统参考数据（当前租户的实时快照，工具参数直接传其中的中文值，禁止传参考数据之外的值或编造的id）：\n"
                + buildTenantReference(user)
                + "\n参考数据使用规则：\n"
                + "1. 状态/类型/重要程度/性质等条件，直接传中文值，如 statusId=\"进行中\"、typeId=\"人才培养/本科生\"、impId=\"重要\"。"
                + "写法必须与参考数据完全一致；用户说的值不在参考数据中时，用【选项】列出实际可用的值让用户选择，禁止硬套。\n"
                + "2. 科室条件直接传科室名，如 departmentId=\"计算机科学与技术系\"，写法与参考数据一致。\n"
                + "3. 负责人：getTasks 传 headName=姓名（【人员】项中完全同名时直接用该姓名）；"
                + "姓名不完全匹配（可能是语音错字）或不确定时，调用 getUsers 工具解析，按其返回处理。\n\n"
                + "工具使用规则：\n"
                + "1. 涉及真实数据（任务/会议/重点工作）必须调用工具查询，严禁编造。\n"
                + "2. 查任务用 getTasks；查会议安排用 getMeetings；查重点工作用 getWorks。"
                + "用户说\"会议/开会\"指meeting，说\"重点工作/重要事项/事件\"指works，说\"任务/计划\"指plan。\n"
                + "3. getMeetings：date=某一天(yyyy-MM-dd)查当天的会议(含跨天会议)；"
                + "startTime+endTime=查日期范围内的会议；host传主持人姓名；keyword为会议名称关键词。\n"
                + "4. getWorks：负责人直接传 headName(姓名)；状态/类型直接传中文值(statusId/typeId)。\n"
                + "5. 用户以第一人称查询任务(如\"我的任务\"\"我最近完成了什么\")时，getTasks 必须传 mine: true，"
                + "确保只查用户自己申报的任务。\n"
                + "6. 单次最多返回20条，若 total 大于返回条数，提醒用户可缩小范围或翻页。\n"
                + "7. 查询无结果时如实告知；工具返回 error 时如实转述，不要重试超过两次。\n\n"
                + "输入纠错与引导策略（重要，用户可能输入错误的姓名/状态/科室）：\n"
                + "a. getUsers 未找到用户时，工具会返回系统里实际存在的相近姓名(exactMatch=false)，"
                + "把这些姓名列出来让用户确认，确认后才继续查询；列表为空就请用户核对姓名，或改用科室/时间条件。\n"
                + "b. 用户说的状态/类型/重要程度在参考数据字典里找不到（如\"搁置\"\"挂起\"），不要硬套，"
                + "用【选项】列出实际可用的值让用户选择。\n"
                + "c. 用户说的科室名在参考数据里找不到时，用【选项】列出相近的实际科室名让用户确认。\n"
                + "d. 条件有效但结果为0时，指出最可能导致无结果的条件（通常是时间过窄、姓名或科室不对），"
                + "并必附【选项】给出2~4个放宽条件的具体查询（如\"查X的全部任务\"\"查X本月的任务\"）"
                + "供用户直接点选；不要只抛一句\"是否要这样查询\"之类的问话让用户打字回答，"
                + "更不要只问不给选项。\n"
                + "e. 用户提出查询以外的需求（创建/修改/删除任务或会议等写操作），说明当前仅支持查询，"
                + "引导其到系统对应页面操作。\n"
                + "f. 语音识别容错（重要）：用户输入可能来自语音转文字，常含同音/形近错别字"
                + "（如\"章三\"应为\"张三\"、\"周民香\"应为\"周敏香\"、\"健设科\"应为\"建设科\"）。"
                + "当人名/科室名/状态词与参考数据不完全匹配时：先按同音或形近在参考数据中推断真实意图；"
                + "人名仍不确定时调用 getUsers 核对；"
                + "推断唯一时直接按纠正后的中文值查询；仍不确定时用【选项】列出最可能的2~4个正确说法让用户确认；"
                + "禁止因错别字直接答复\"未查询到\"。\n\n"
                + "回答要求：用简体中文，简洁清晰；多条任务时逐行列出名称、状态、负责人、起止时间；不透露系统实现细节。";
    }

    /** 常驻参考层缓存：tid -> (参考文本, 过期时间)，与热词缓存同款模式 */
    private final Map<String, AbstractMap.SimpleEntry<String, Long>> refCache = new HashMap<>();

    /**
     * 构建当前租户的常驻参考层：字典(状态/类型等合法取值)、科室名、人员名单。
     * 只内嵌"值"不内嵌id——工具参数直接传中文值，由后端确定性解析成id，
     * 避免小模型抄写长UUID出错（实测把科室id当状态id传）。
     * 人员表过大时不内嵌（人名解析回退到 getUsers 工具），控制提示词体积。
     */
    private String buildTenantReference(User user) {
        String tid = user.getTid();
        long now = System.currentTimeMillis();
        synchronized (refCache) {
            AbstractMap.SimpleEntry<String, Long> cached = refCache.get(tid);
            if (cached != null && now < cached.getValue()) {
                return cached.getKey();
            }
        }
        StringBuilder sb = new StringBuilder();
        // ===== 字典：按field聚合成一行，只列合法取值 =====
        // 结构有两种：status/imp/attribute等是"平铺型"(每个取值自己就是根节点，无children)；
        // type是"层级型"(根=一级分类如"人才培养"，子=二级如"本科生"，展示为"人才培养/本科生")
        try {
            List<SysDict> roots = sysdictService.getAllSysdicByTid(tid);
            Map<String, StringBuilder> fieldValues = new LinkedHashMap<>();
            Map<String, String> fieldLabels = new HashMap<>();
            for (SysDict root : roots) {
                if (root == null || ObjectUtil.isEmpty(root.getField())) continue;
                fieldLabels.putIfAbsent(root.getField(),
                        ObjectUtil.isNotEmpty(root.getName()) ? root.getName() : root.getField());
                StringBuilder buf = fieldValues.computeIfAbsent(root.getField(), k -> new StringBuilder());
                if (root.getChildren() == null || root.getChildren().isEmpty()) {
                    if (ObjectUtil.isNotEmpty(root.getValue())) {
                        buf.append(root.getValue()).append("  ");
                    }
                } else {
                    for (SysDict c : root.getChildren()) {
                        if (c == null || ObjectUtil.isEmpty(c.getValue())) continue;
                        buf.append(root.getValue()).append('/').append(c.getValue()).append("  ");
                    }
                }
            }
            for (Map.Entry<String, StringBuilder> e : fieldValues.entrySet()) {
                String values = e.getValue().toString().trim();
                if (!values.isEmpty()) {
                    sb.append("【字典-").append(fieldLabels.get(e.getKey())).append("】")
                            .append(values).append('\n');
                }
            }
        } catch (Exception e) {
            log.warn("参考层-加载字典失败: {}", e.getMessage());
        }
        // ===== 科室 =====
        try {
            List<Department> departments = departmentService.getDepartmentByTid(tid);
            StringBuilder line = new StringBuilder("【科室】");
            for (Department d : departments) {
                if (d == null || ObjectUtil.isEmpty(d.getName())) continue;
                line.append(d.getName()).append("  ");
            }
            if (line.length() > "【科室】".length()) {
                sb.append(line.toString().trim()).append('\n');
            }
        } catch (Exception e) {
            log.warn("参考层-加载科室失败: {}", e.getMessage());
        }
        // ===== 人员：仅在人数可控时内嵌，过大则留给 getUsers 工具 =====
        try {
            List<User> users = userService.getAllUser(tid);
            if (users != null && users.size() <= REF_MAX_USERS) {
                StringBuilder line = new StringBuilder("【人员】");
                for (User u : users) {
                    if (u == null || ObjectUtil.isEmpty(u.getName())) continue;
                    line.append(u.getName());
                    if (ObjectUtil.isNotEmpty(u.getDepartment())) {
                        line.append('(').append(u.getDepartment()).append(')');
                    }
                    line.append("  ");
                }
                if (line.length() > "【人员】".length()) {
                    sb.append(line.toString().trim()).append('\n');
                }
            } else {
                sb.append("【人员】人员较多未内嵌，姓名解析请调用 getUsers 工具\n");
            }
        } catch (Exception e) {
            log.warn("参考层-加载人员失败: {}", e.getMessage());
        }
        String ref = sb.toString().trim();
        synchronized (refCache) {
            refCache.put(tid, new AbstractMap.SimpleEntry<>(ref, now + REF_TTL_MS));
        }
        return ref;
    }

    /**
     * 字典值→字典id 的确定性解析（工具参数侧）。
     * 兼容字典的两种结构：平铺型(status/imp/attribute，取值即根节点)与层级型(type，取值在子节点)；
     * 入参兼容完整值("人才培养/本科生")与末级值("本科生")、字典id原样放行；
     * 都匹配不上则原样返回交给SQL兜底。
     */
    private String resolveDictId(String tid, String field, String raw) {
        if (ObjectUtil.isEmpty(raw)) {
            return raw;
        }
        // "人才培养/本科生"这类带层级的写法，取末级匹配
        String tail = raw.contains("/") ? raw.substring(raw.lastIndexOf('/') + 1) : raw;
        try {
            for (SysDict root : sysdictService.getAllSysdicByTid(tid)) {
                if (root == null || !field.equals(root.getField())) {
                    continue;
                }
                if (dictNodeMatches(root, raw) || dictNodeMatches(root, tail)) {
                    return root.getId();
                }
                if (root.getChildren() != null) {
                    for (SysDict c : root.getChildren()) {
                        if (dictNodeMatches(c, raw) || dictNodeMatches(c, tail)) {
                            return c.getId();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("字典解析失败(field={}): {}", field, e.getMessage());
        }
        return raw;
    }

    /** 字典节点与入参是否匹配（比对value/name；name兼容历史"以字段名为name"的数据） */
    private boolean dictNodeMatches(SysDict node, String s) {
        return ObjectUtil.isNotEmpty(s)
                && (s.equals(node.getValue()) || s.equals(node.getName()) || s.equals(node.getId()));
    }

    /** 科室名→科室id 的确定性解析；匹配不上原样返回交给SQL兜底 */
    private String resolveDepartmentId(String tid, String raw) {
        if (ObjectUtil.isEmpty(raw)) {
            return raw;
        }
        try {
            for (Department d : departmentService.getDepartmentByTid(tid)) {
                if (d == null) continue;
                if (raw.equals(d.getId()) || raw.equals(d.getName())) {
                    return d.getId();
                }
            }
        } catch (Exception e) {
            log.warn("科室解析失败: {}", e.getMessage());
        }
        return raw;
    }

    /**
     * 姓名→用户id 的确定性解析：唯一同名用户返回其id，否则原样返回
     * （原样传姓名可兼容plan.head历史数据存姓名的行；0结果时toolGetTasks另有放宽提示）
     */
    private String resolveHeadId(String tid, String raw) {
        if (ObjectUtil.isEmpty(raw)) {
            return raw;
        }
        try {
            User query = new User();
            query.setTid(tid);
            query.setName(raw);
            List<User> users = userService.getUserByCondition(query);
            if (users != null && users.size() == 1) {
                return users.get(0).getId();
            }
        } catch (Exception e) {
            log.warn("负责人解析失败: {}", e.getMessage());
        }
        return raw;
    }

    private JSONArray buildToolsSchema() {
        JSONObject getTasksParams = JSONUtil.createObj()
                .set("type", "object")
                .set("properties", JSONUtil.createObj()
                        .set("keyword", JSONUtil.createObj().set("type", "string")
                                .set("description", "任务名称关键字，模糊匹配"))
                        .set("statusId", JSONUtil.createObj().set("type", "string")
                                .set("description", "任务状态，直接传参考数据中的中文值，如\"进行中\""))
                        .set("typeId", JSONUtil.createObj().set("type", "string")
                                .set("description", "任务类型，直接传参考数据中的中文值，如\"人才培养/本科生\""))
                        .set("impId", JSONUtil.createObj().set("type", "string")
                                .set("description", "重要程度，直接传参考数据中的中文值，如\"重要\""))
                        .set("attributeId", JSONUtil.createObj().set("type", "string")
                                .set("description", "任务性质，直接传参考数据中的中文值，如\"计划内\""))
                        .set("headName", JSONUtil.createObj().set("type", "string")
                                .set("description", "负责人姓名，直接传参考数据人员表中的姓名，优先用本参数"))
                        .set("headId", JSONUtil.createObj().set("type", "string")
                                .set("description", "负责人的用户ID（仅在getUsers解析返回了id时使用，否则用headName）"))
                        .set("departmentId", JSONUtil.createObj().set("type", "string")
                                .set("description", "科室，直接传参考数据中的科室名，如\"计算机科学与技术系\""))
                        .set("mine", JSONUtil.createObj().set("type", "boolean")
                                .set("description", "为 true 时只查当前用户自己申报的任务"))
                        .set("startTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，筛选计划结束时间不早于该日期的任务"))
                        .set("endTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，筛选计划开始时间不晚于该日期的任务"))
                        .set("page", JSONUtil.createObj().set("type", "integer")
                                .set("description", "页码，默认1"))
                        .set("pageSize", JSONUtil.createObj().set("type", "integer")
                                .set("description", "每页条数，默认10，最大20")))
                .set("required", new JSONArray());

        JSONArray tools = new JSONArray();
        tools.add(JSONUtil.createObj()
                .set("type", "function")
                .set("function", JSONUtil.createObj()
                        .set("name", "getTasks")
                        .set("description", "按条件查询当前用户有权限看到的任务列表")
                        .set("parameters", getTasksParams)));

        JSONObject getMeetingsParams = JSONUtil.createObj()
                .set("type", "object")
                .set("properties", JSONUtil.createObj()
                        .set("date", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，查询这一天的会议(自动包含覆盖该天的多日会议)"))
                        .set("startTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，与endTime配合查询日期范围内的会议"))
                        .set("endTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，范围查询的结束日期"))
                        .set("keyword", JSONUtil.createObj().set("type", "string")
                                .set("description", "会议名称关键词，模糊匹配"))
                        .set("host", JSONUtil.createObj().set("type", "string")
                                .set("description", "主持人姓名，模糊匹配"))
                        .set("page", JSONUtil.createObj().set("type", "integer")
                                .set("description", "页码，默认1"))
                        .set("pageSize", JSONUtil.createObj().set("type", "integer")
                                .set("description", "每页条数，默认10，最大20")))
                .set("required", new JSONArray());

        tools.add(JSONUtil.createObj()
                .set("type", "function")
                .set("function", JSONUtil.createObj()
                        .set("name", "getMeetings")
                        .set("description", "按条件查询会议安排(单日或日期范围，含跨天会议)")
                        .set("parameters", getMeetingsParams)));

        JSONObject getWorksParams = JSONUtil.createObj()
                .set("type", "object")
                .set("properties", JSONUtil.createObj()
                        .set("keyword", JSONUtil.createObj().set("type", "string")
                                .set("description", "重点工作名称关键词，模糊匹配"))
                        .set("statusId", JSONUtil.createObj().set("type", "string")
                                .set("description", "工作状态，直接传参考数据中的中文值"))
                        .set("typeId", JSONUtil.createObj().set("type", "string")
                                .set("description", "工作类型，直接传参考数据中的中文值"))
                        .set("headName", JSONUtil.createObj().set("type", "string")
                                .set("description", "负责人的姓名，直接传姓名"))
                        .set("departmentId", JSONUtil.createObj().set("type", "string")
                                .set("description", "科室，直接传参考数据中的科室名"))
                        .set("startTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，筛选结束时间不早于该日期的工作"))
                        .set("endTime", JSONUtil.createObj().set("type", "string")
                                .set("description", "yyyy-MM-dd，筛选开始时间不晚于该日期的工作"))
                        .set("page", JSONUtil.createObj().set("type", "integer")
                                .set("description", "页码，默认1"))
                        .set("pageSize", JSONUtil.createObj().set("type", "integer")
                                .set("description", "每页条数，默认10，最大20")))
                .set("required", new JSONArray());

        tools.add(JSONUtil.createObj()
                .set("type", "function")
                .set("function", JSONUtil.createObj()
                        .set("name", "getWorks")
                        .set("description", "按条件查询重点工作(重要事项)列表")
                        .set("parameters", getWorksParams)));
        tools.add(JSONUtil.createObj()
                .set("type", "function")
                .set("function", JSONUtil.createObj()
                        .set("name", "getUsers")
                        .set("description", "按姓名模糊查询用户，返回 id/name/department/role，用于把负责人姓名解析成用户ID（参考数据中人员表未内嵌或姓名不完全匹配时使用）")
                        .set("parameters", JSONUtil.createObj()
                                .set("type", "object")
                                .set("properties", JSONUtil.createObj()
                                        .set("keyword", JSONUtil.createObj().set("type", "string")
                                                .set("description", "用户姓名，支持模糊匹配")))
                                .set("required", new JSONArray().set("keyword")))));
        return tools;
    }
}
