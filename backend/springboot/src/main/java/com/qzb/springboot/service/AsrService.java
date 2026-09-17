package com.qzb.springboot.service;

import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 智谱 GLM-ASR 语音转文本 REST API 封装
 * 文档: https://docs.bigmodel.cn/api-reference/模型-api/语音转文本
 * 音频要求: wav/mp3，大小<=25MB，时长<=30秒(前端已按30秒自动截断)
 * 计费说明: glm-asr 为付费模型，账户需有余额或资源包，否则返回错误码1113
 */
@Service
public class AsrService {

    private static final Logger log = LoggerFactory.getLogger(AsrService.class);

    private static final String ASR_PATH = "/audio/transcriptions";
    /** 音频最大字节数(GLM限制25MB，30秒wav 16k单声道约1MB，8MB上限留足余量) */
    private static final int MAX_AUDIO_BYTES = 8 * 1024 * 1024;

    @Value("${glm.base-url}")
    private String baseUrl;
    @Value("${glm.api-key}")
    private String apiKey;
    @Value("${glm.asr-model:glm-asr-2512}")
    private String asrModel;

    /**
     * 识别 wav/mp3 音频，返回识别文本
     */
    public String recognizeWav(byte[] audio) {
        return recognizeWav(audio, null);
    }

    /**
     * 识别 wav/mp3 音频，返回识别文本
     *
     * @param hotwords 热词表(科室名/状态词/人名等，接口上限100个)，提高领域专有名词的识别准确率
     */
    public String recognizeWav(byte[] audio, java.util.List<String> hotwords) {
        if (ObjectUtil.isEmpty(apiKey)) {
            throw new IllegalStateException("GLM API Key 未配置(glm.api-key)");
        }
        if (audio == null || audio.length == 0) {
            throw new IllegalArgumentException("音频内容为空");
        }
        if (audio.length > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("音频过大，请控制在30秒以内");
        }

        // multipart/form-data: model + file(+可选hotwords)，Bearer认证(与GLM对话共用同一个Key)
        HttpRequest request = HttpRequest.post(baseUrl + ASR_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .form("model", asrModel)
                .form("file", new BytesResource(audio, "voice.wav"));
        if (hotwords != null && !hotwords.isEmpty()) {
            // hotwords为字符串数组，multipart下以JSON数组字符串形式传单个字段(实测可被正确解析)
            request.form("hotwords", JSONUtil.toJsonStr(hotwords));
        }
        String resp = request.timeout(30000)
                .execute().body();
        log.debug("GLM-ASR响应: {}", resp);

        JSONObject json = JSONUtil.parseObj(resp);
        if (json.containsKey("error")) {
            JSONObject err = json.getJSONObject("error");
            // 前缀由 AiChatController 统一添加(语音识别失败：...)
            throw new IllegalStateException(err == null ? resp
                    : (err.getStr("code", "") + " " + err.getStr("message", "未知错误")));
        }
        return json.getStr("text", "").trim();
    }
}
