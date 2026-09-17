package com.qzb.springboot.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.service.AiChatService;
import com.qzb.springboot.service.AsrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * AI 助手接口
 * 不在 WebConfig 排除名单中，自动经过 JwtInterceptor 统一鉴权，
 * TokenUtils 在工具执行时可取到当前用户身份与权限。
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    @Resource
    private AiChatService aiChatService;
    @Resource
    private AsrService asrService;

    /**
     * 语音识别：前端录音(wav 16kHz 16bit 单声道，30秒内)上传，返回识别文本。
     * 自动携带本租户热词表(字典值/科室名/人名)，降低专有名词的识别错字率
     */
    @PostMapping("/asr")
    public Result asr(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success(asrService.recognizeWav(file.getBytes(), aiChatService.buildAsrHotwords()));
        } catch (Exception e) {
            log.error("语音识别异常", e);
            return Result.error("500", "语音识别失败：" + e.getMessage());
        }
    }

    /**
     * @param body {"message":"本周我有哪些任务",
     *              "sessionId":"可选，继续已有会话时传入",
     *              "history":[{"role":"user","content":"..."},{"role":"assistant","content":"..."}]}
     * @return data: {"sessionId":"会话ID","answer":"回答"}
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> body) {
        String message = body.get("message") == null ? null : body.get("message").toString().trim();
        if (ObjectUtil.isEmpty(message)) {
            return Result.error("400", "消息不能为空");
        }
        if (message.length() > 2000) {
            message = message.substring(0, 2000);
        }
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> history = (List<Map<String, Object>>) body.get("history");
            String sessionId = body.get("sessionId") == null ? null : body.get("sessionId").toString();
            boolean clarifyReply = Boolean.TRUE.equals(body.get("clarifyReply"));
            return Result.success(aiChatService.chat(message, history, sessionId, clarifyReply));
        } catch (Exception e) {
            log.error("AI对话异常", e);
            return Result.error("500", "AI 服务异常：" + e.getMessage());
        }
    }

    /**
     * 流式对话(SSE)：思考过程与最终答案以事件流推送
     * 事件格式 data: {"type":"step|done|error", ...}
     * 同步在请求线程执行，保证 JwtInterceptor 填充的用户上下文(ThreadLocal)可用
     */
    @PostMapping("/chat/stream")
    public void chatStream(@RequestBody Map<String, Object> body, HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            String message = body.get("message") == null ? null : body.get("message").toString().trim();
            if (ObjectUtil.isEmpty(message)) {
                writer.write("data: " + JSONUtil.createObj().set("type", "error")
                        .set("content", "消息不能为空") + "\n\n");
                writer.flush();
                return;
            }
            if (message.length() > 2000) {
                message = message.substring(0, 2000);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> history = (List<Map<String, Object>>) body.get("history");
            String sessionId = body.get("sessionId") == null ? null : body.get("sessionId").toString();
            boolean clarifyReply = Boolean.TRUE.equals(body.get("clarifyReply"));
            final PrintWriter out = writer;
            aiChatService.chatStream(message, history, sessionId, event -> {
                out.write("data: " + event + "\n\n");
                out.flush();
            }, clarifyReply);
        } catch (Exception e) {
            log.error("AI流式对话接口异常", e);
            if (writer != null) {
                try {
                    writer.write("data: " + JSONUtil.createObj().set("type", "error")
                            .set("content", "AI 服务异常，请稍后重试") + "\n\n");
                    writer.flush();
                } catch (Exception ignore) {
                    // 客户端已断开，忽略
                }
            }
        }
    }

    /**
     * 我的会话列表（按最近活跃倒序）
     */
    @GetMapping("/sessions")
    public Result sessions(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "20") Integer pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        return Result.success(aiChatService.listSessions(pageNum, pageSize));
    }

    /**
     * 加载某个会话的全部消息（只能看自己的）
     */
    @GetMapping("/messages")
    public Result messages(@RequestParam String sid) {
        try {
            return Result.success(aiChatService.loadMessages(sid));
        } catch (Exception e) {
            return Result.error("403", e.getMessage());
        }
    }

    /**
     * 删除会话（软删会话 + 物理删除消息）
     */
    @DeleteMapping("/session")
    public Result deleteSession(@RequestParam String sid) {
        aiChatService.deleteSession(sid);
        return Result.success();
    }

    /**
     * 删除某一轮问答（mid=该轮用户消息ID，连带其后到下一轮之前的回答一并物理删除；
     * 若删完会话已无消息，则连会话一起删除）
     */
    @DeleteMapping("/message")
    public Result deleteMessage(@RequestParam String sid, @RequestParam String mid) {
        try {
            aiChatService.deleteMessageRound(sid, mid);
            return Result.success();
        } catch (Exception e) {
            return Result.error("403", e.getMessage());
        }
    }
}
