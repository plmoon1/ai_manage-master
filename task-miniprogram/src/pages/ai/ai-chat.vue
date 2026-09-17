<template>
  <view class="container">
    <u-navbar
      title="AI助手"
      :bgColor="'#1a73e8'"
      :titleStyle="{ color: '#fff', fontWeight: 'bold' }"
      :autoBack="true"
      backText="返回"
      :placeholder="true"
    >
    </u-navbar>

    <!-- 消息列表 -->
    <scroll-view
      scroll-y
      class="chat-list"
      :scroll-top="scrollTop"
    >
      <view class="chat-tip" v-if="messages.length === 0">
        你好，我是任务管理助手，可以问我任务相关的问题\n例如：张三本周有哪些任务 / 我最近三个月完成了什么
      </view>

      <view
        v-for="(m, index) in messages"
        :key="index"
        :id="'msg-' + index"
        class="msg-row"
        :class="m.role === 'user' ? 'row-right' : 'row-left'"
      >
        <view class="bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
          <!-- 思考过程区块（AI消息，且有思考步骤时展示） -->
          <view v-if="m.role === 'assistant' && m.thinking && m.thinking.length" class="think-box">
            <view class="think-header" @tap="toggleThink(index)">
              <text class="think-dot" :class="m.thinkingDone ? 'dot-done' : 'dot-thinking'"></text>
              <text class="think-title">{{ m.thinkingDone ? '已完成思考' : '思考中…' }}</text>
              <text class="think-arrow">{{ m.thinkOpen ? '︿' : '﹀' }}</text>
            </view>
            <view v-show="m.thinkOpen" class="think-steps">
              <view v-for="(s, si) in m.thinking" :key="si" class="think-step">
                <text class="step-dot">✦</text>
                <text class="step-text">{{ s }}</text>
              </view>
            </view>
          </view>
          <view v-if="m.content" class="bubble-text">
            <template v-for="(seg, si) in linkify(m)">
              <text
                v-if="seg.task"
                :key="'lk' + si"
                class="task-link"
                @tap.stop="goTaskDetail(seg.task)"
              >{{ seg.text }}</text>
              <text v-else :key="'tx' + si">{{ seg.text }}</text>
            </template>
          </view>
          <!-- 澄清选项（AI提问时给出可点击选项，点即发送，语音用户免打字） -->
          <view v-if="m.role === 'assistant' && m.options && m.options.length" class="option-chips">
            <text
              v-for="(opt, oi) in m.options"
              :key="oi"
              class="option-chip"
              :class="{ 'chip-disabled': loading || !sessionId }"
              @tap="tapOption(opt)"
            >{{ opt }}</text>
          </view>
        </view>
        <!-- 删除这一轮（仅用户消息显示，且需已落库拿到消息id） -->
        <text
          v-if="m.role === 'user' && m.id && !loading"
          class="round-del"
          @tap.stop="removeRound(index)"
        >删除</text>
      </view>

      <view class="chat-bottom" id="chat-bottom"></view>
    </scroll-view>

    <!-- 录音提示层（微信式：左滑取消，右滑转文字，松手发送） -->
    <view
      class="recognize-mask"
      v-if="recording"
      @touchend.prevent="onVoiceEnd"
      @mouseup.prevent="onVoiceEnd"
    >
      <view class="recognize-box" :class="{ 'recognize-cancel': voiceCancel, 'recognize-totext': voiceToText }">
        <view class="recognize-icons">
          <view class="rec-side" :class="{ 'rec-side-active': voiceCancel }">
            <u-icon name="close" size="40" color="#fff"></u-icon>
            <text class="rec-side-label">取消</text>
          </view>
          <view class="recognize-icon">
            <u-icon name="mic" size="72" color="#fff" class="mic-pulse"></u-icon>
          </view>
          <view class="rec-side" :class="{ 'rec-side-active': voiceToText }">
            <text class="rec-char">文</text>
            <text class="rec-side-label">转文字</text>
          </view>
        </view>
        <text class="recognize-title">{{ voiceTitle }}</text>
        <text class="recognize-hint">{{ voiceCancel || voiceToText ? '滑回中间可继续录音' : '左滑取消 · 右滑转文字 · 最长30秒' }}</text>
      </view>
    </view>

    <!-- 历史会话弹层 -->
    <view class="history-mask" v-if="showHistory" @tap="showHistory = false">
      <view class="history-panel" @tap.stop>
        <view class="history-header">
          <text class="history-title">历史对话</text>
          <text class="history-close" @tap="showHistory = false">关闭</text>
        </view>
        <scroll-view scroll-y class="history-list">
          <view v-if="sessions.length === 0" class="history-empty">暂无历史对话</view>
          <view
            v-for="s in sessions"
            :key="s.id"
            class="history-item"
            @tap="loadSession(s)"
          >
            <view class="history-info">
              <text class="history-item-title">{{ s.title || '新对话' }}</text>
              <text class="history-item-time">{{ s.updateTime }}</text>
            </view>
            <text class="history-del" @tap.stop="removeSession(s)">删除</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 工具栏 -->
    <view class="chat-toolbar">
      <text class="toolbar-btn" @tap="newChat">+ 新对话</text>
      <text class="toolbar-btn" @tap="openHistory">历史对话</text>
      <text v-if="sessionId" class="toolbar-btn toolbar-danger" @tap="deleteCurrentSession">删除记录</text>
    </view>

    <!-- 输入栏（微信式：左侧图标切换 输入框/按住说话） -->
    <view class="input-bar">
      <!-- 语音/键盘 切换图标 -->
      <view class="bar-icon" @tap="toggleVoiceMode">
        <u-icon :name="voiceMode ? 'edit-pen' : 'mic'" size="34" color="#666"></u-icon>
      </view>

      <!-- 文字输入模式 -->
      <input
        v-if="!voiceMode"
        class="chat-input"
        v-model="inputText"
        placeholder="输入问题…"
        confirm-type="send"
        :disabled="loading"
        @confirm="sendText"
      />
      <button v-if="!voiceMode" class="send-btn" :disabled="loading" @tap="sendText">发送</button>

      <!-- 语音模式：整条"按住说话"（按住录音，左滑取消，右滑转文字，松手发送） -->
      <view
        v-else
        class="hold-talk"
        :class="{ 'hold-talk-active': recording && !voiceCancel && !voiceToText, 'hold-talk-cancel': voiceCancel, 'hold-talk-totext': voiceToText }"
        @touchstart.prevent="onVoiceStart"
        @touchmove.prevent="onVoiceMove"
        @touchend.prevent="onVoiceEnd"
        @touchcancel="onVoiceEnd"
        @mousedown.prevent="onVoiceStart"
        @mouseup.prevent="onVoiceEnd"
      >{{ holdTalkLabel }}</view>
    </view>
  </view>
</template>

<script>
import Recorder from 'recorder-core'
import 'recorder-core/src/engine/wav'
import { aiApi } from '@/api/index'
import config from '@/config'

export default {
  data() {
    return {
      messages: [],      // [{role: 'user'|'assistant', content}]
      inputText: '',
      loading: false,
      recording: false,  // 是否正在录音
      voiceMode: false,  // 微信式输入栏：true=语音模式(显示"按住说话")，false=键盘模式
      voiceCancel: false,// 录音中手指左滑，松手后将取消而非发送
      voiceToText: false,// 录音中手指右滑，松手后识别成文字填入输入框（不直接发送）
      touchStartX: 0,    // 录音起始触点X，用于计算左滑取消/右滑转文字
      touchStartY: 0,    // 录音起始触点Y（保留备用）
      scrollTop: 0,      // 滚动位置(每次递增以强制触发滚动)
      sessionId: null,   // 当前会话ID(后端落库返回)
      showHistory: false,
      sessions: [],      // 历史会话列表
      rec: null,         // recorder-core 实例
      recTimer: null,    // 60秒自动停止定时器
      typeTimers: []     // 答案打字机定时器（页面卸载时清理）
    }
  },

  computed: {
    // "按住说话"按钮的多态文案（微信式）
    holdTalkLabel() {
      if (this.voiceCancel) return '松开手指，取消'
      if (this.voiceToText) return '松开手指，转为文字'
      if (this.recording) return '松开 结束'
      return '按住 说话'
    },
    // 录音浮层主文案
    voiceTitle() {
      if (this.voiceCancel) return '松开手指，取消'
      if (this.voiceToText) return '松开手指，转为文字'
      return '松开手指，发送'
    }
  },

  onLoad() {
    // 默认恢复最近一次对话
    this.restoreLastSession()
  },

  onUnload() {
    if (this.recTimer) {
      clearTimeout(this.recTimer)
      this.recTimer = null
    }
    this.typeTimers.forEach((t) => clearInterval(t))
    this.typeTimers = []
    if (this.rec) {
      try {
        this.rec.close()
      } catch (e) { /* ignore */ }
      this.rec = null
    }
  },

  methods: {
    // ==================== 语音识别（H5录音 + 后端GLM-ASR） ====================
    // 微信式输入栏：切换 键盘/语音 模式
    toggleVoiceMode() {
      if (this.recording) return // 录音中禁止切换，避免状态错乱
      this.voiceCancel = false
      this.voiceToText = false
      this.voiceMode = !this.voiceMode
    },

    onVoiceStart(e) {
      if (this.loading || this.recording) return
      // 记录起始触点，touchmove里据此计算"左滑取消/右滑转文字"（微信交互）
      const t = e && e.touches && e.touches[0]
      this.touchStartX = t ? t.clientX : (e ? e.clientX : 0)
      this.touchStartY = t ? t.clientY : (e ? e.clientY : 0)
      this.voiceCancel = false
      this.voiceToText = false
      // GLM-ASR要求：wav/mp3，时长30秒内（recorder-core输出 wav 16000Hz 16bit 单声道）
      const rec = Recorder({
        type: 'wav',
        sampleRate: 16000,
        bitRate: 16
      })
      rec.open(
        () => {
          this.rec = rec
          this.recording = true
          rec.start()
          // GLM-ASR限制30秒，到时自动提交
          this.recTimer = setTimeout(() => this.finishVoice(), 30000)
        },
        (msg, isUserNotAllow) => {
          console.error('录音打开失败:', msg, '用户拒绝:', isUserNotAllow)
          uni.showToast({
            title: isUserNotAllow ? '请允许网页使用麦克风' : '录音不可用：' + msg,
            icon: 'none',
            duration: 3000
          })
        }
      )
    },

    // 录音中水平滑动：左滑超阈值→取消态，右滑超阈值→转文字态，滑回中间→恢复发送
    onVoiceMove(e) {
      if (!this.recording) return
      const t = e && e.touches && e.touches[0]
      if (!t) return
      const dx = t.clientX - this.touchStartX
      if (dx < -80) {          // 左滑取消
        this.voiceCancel = true
        this.voiceToText = false
      } else if (dx > 80) {    // 右滑转文字
        this.voiceToText = true
        this.voiceCancel = false
      } else {                 // 中间区域：松手直接发送
        this.voiceCancel = false
        this.voiceToText = false
      }
    },

    onVoiceEnd() {
      if (!this.recording) return
      if (this.voiceCancel) {
        this.cancelVoice()
      } else {
        // 转文字态松手：识别结果填入输入框而非直接发送
        this.finishVoice(this.voiceToText)
      }
    },

    // 取消本次录音：停止并丢弃音频，不调识别
    cancelVoice() {
      if (this.recTimer) {
        clearTimeout(this.recTimer)
        this.recTimer = null
      }
      const rec = this.rec
      this.recording = false
      this.voiceCancel = false
      this.voiceToText = false
      if (!rec) return
      rec.stop(
        () => { rec.close() }, // 成功回调里直接丢弃blob
        () => { rec.close() }
      )
      this.rec = null
      uni.showToast({ title: '已取消', icon: 'none' })
    },

    finishVoice(toText) {
      if (this.recTimer) {
        clearTimeout(this.recTimer)
        this.recTimer = null
      }
      const rec = this.rec
      if (!rec) return
      this.recording = false
      this.voiceCancel = false
      this.voiceToText = false
      rec.stop(
        (blob, duration) => {
          rec.close()
          this.rec = null
          if (duration < 500) {
            uni.showToast({ title: '说话时间太短', icon: 'none' })
            return
          }
          this.uploadVoice(blob, toText)
        },
        (msg) => {
          rec.close()
          this.rec = null
          uni.showToast({ title: '录音失败：' + msg, icon: 'none' })
        }
      )
    },

    async uploadVoice(blob, toText) {
      try {
        const formData = new FormData()
        formData.append('file', blob, 'voice.wav')
        const res = await aiApi.asr(formData)
        const text = res && res.data ? String(res.data).trim() : ''
        if (!text) {
          uni.showToast({ title: '未识别到语音内容', icon: 'none' })
          return
        }
        if (toText) {
          // 转文字：填入输入框，切回键盘模式，用户可编辑后手动发送
          this.inputText = text
          this.voiceMode = false
          uni.showToast({ title: '已转为文字', icon: 'none' })
        } else {
          this.send(text)
        }
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      }
    },

    // ==================== 对话 ====================
    sendText() {
      const text = this.inputText.trim()
      if (!text || this.loading) return
      this.inputText = ''
      this.send(text)
    },

    // 删除某一轮问答：确认后调后端物理删除，本地同步移除该轮（用户消息+其回答）
    removeRound(index) {
      const m = this.messages[index]
      if (!m || m.role !== 'user' || !m.id || this.loading) return
      uni.showModal({
        title: '删除这轮对话',
        content: '确定删除这条提问和AI的回答吗？删除后不可恢复',
        success: async (r) => {
          if (!r.confirm) return
          try {
            await aiApi.deleteMessage(this.sessionId, m.id)
            // 本地移除该轮：用户消息及其后到下一轮用户消息前的全部消息
            let end = index + 1
            while (end < this.messages.length && this.messages[end].role !== 'user') end++
            this.messages.splice(index, end - index)
            // 删空了：后端已连带删除会话，重置会话状态
            if (this.messages.length === 0) {
              this.sessionId = null
              this.scrollTop = 0
            }
            uni.showToast({ title: '已删除', icon: 'success' })
          } catch (e) {
            // 错误提示由 request 拦截器统一处理
          }
        }
      })
    },
    async send(message, opts) {
      const options = opts || {}
      // history 只带最近 10 条，且不含本次消息（后端会拼接 message）
      const history = this.messages
        .filter((m) => m.content)
        .slice(-10)
        .map((m) => ({ role: m.role, content: m.content }))

      this.messages.push({ id: null, role: 'user', content: message })
      // AI 占位消息：含思考过程容器（乐观推入第一步，体验即时）
      this.messages.push({
        id: null,
        role: 'assistant',
        content: '',
        options: [],
        tasks: [],
        thinking: ['正在理解你的问题…'],
        thinkingDone: false,
        thinkOpen: true
      })
      const aiIndex = this.messages.length - 1
      this.loading = true
      this.scrollToBottom()

      try {
        const token = uni.getStorageSync('token')
        const resp = await fetch(config.baseURL + '/ai/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token || ''
          },
          body: JSON.stringify({
            message,
            history,
            sessionId: this.sessionId,
            clarifyReply: !!options.clarifyReply
          })
        })
        if (!resp.ok || !resp.body) {
          throw new Error('HTTP ' + resp.status)
        }

        // 流式读取 SSE 帧（以空行分隔，data: 开头）
        const reader = resp.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buf = ''
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buf += decoder.decode(value, { stream: true })
          let idx
          while ((idx = buf.indexOf('\n\n')) !== -1) {
            const frame = buf.slice(0, idx)
            buf = buf.slice(idx + 2)
            const dataLine = frame.split('\n').find((l) => l.startsWith('data:'))
            if (!dataLine) continue
            let evt
            try {
              evt = JSON.parse(dataLine.slice(5).trim())
            } catch (e) {
              continue
            }
            this.handleAiEvent(aiIndex, evt)
          }
        }
      } catch (e) {
        const m = this.messages[aiIndex]
        m.thinkingDone = true
        m.thinkOpen = false
        if (!m.content) m.content = '请求失败，请稍后重试'
      } finally {
        this.loading = false
        this.scrollToBottom()
      }
    },

    // 处理后端 SSE 事件：step=思考步骤 / done=最终答案 / error
    handleAiEvent(aiIndex, evt) {
      const m = this.messages[aiIndex]
      if (evt.type === 'step') {
        m.thinking.push(evt.content)
        this.scrollToBottom()
      } else if (evt.type === 'done') {
        if (evt.sessionId) this.sessionId = evt.sessionId
        // 回填该轮消息id（供逐轮删除用）：用户消息在AI占位消息的前一条
        const um = this.messages[aiIndex - 1]
        if (um && um.role === 'user' && evt.userMsgId) um.id = evt.userMsgId
        if (evt.aiMsgId) m.id = evt.aiMsgId
        // 澄清选项（需求模糊时AI给出可点击选项）
        m.options = Array.isArray(evt.options) ? evt.options : []
        // 本轮涉及的任务（含完整对象）：回答中的任务名渲染为可点击链接
        m.tasks = Array.isArray(evt.tasks) ? evt.tasks : []
        m.thinkingDone = true
        m.thinkOpen = false
        this.typeAnswer(aiIndex, evt.answer || '(空回复)')
      } else if (evt.type === 'error') {
        m.thinkingDone = true
        m.thinkOpen = false
        m.content = evt.content || '请求失败，请稍后重试'
        this.scrollToBottom()
      }
    },

    // 把回答文本按任务名切分成片段：命中任务名的部分渲染成可点击链接
    linkify(m) {
      const content = m.content || ''
      const tasks = (m.tasks || []).filter((t) => t && t.name)
      if (!tasks.length || !content) return [{ text: content }]
      // 收集所有任务名在文本中的出现位置
      const marks = []
      tasks.forEach((t) => {
        const esc = t.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        const re = new RegExp(esc, 'g')
        let mm
        while ((mm = re.exec(content)) !== null) {
          marks.push({ start: mm.index, end: mm.index + t.name.length, task: t })
          if (mm.index === re.lastIndex) re.lastIndex++
        }
      })
      if (!marks.length) return [{ text: content }]
      marks.sort((a, b) => a.start - b.start)
      const segs = []
      let pos = 0
      for (const mk of marks) {
        if (mk.start < pos) continue // 重叠的跳过，保留先出现的
        if (mk.start > pos) segs.push({ text: content.slice(pos, mk.start) })
        segs.push({ text: content.slice(mk.start, mk.end), task: mk.task })
        pos = mk.end
      }
      if (pos < content.length) segs.push({ text: content.slice(pos) })
      return segs
    },

    // 跳转详情：按条目类型路由到对应详情页（传参方式与各业务页面一致，传完整对象）
    goTaskDetail(t) {
      if (!t || !t.data) return
      let url = ''
      if (t.type === 'meeting') {
        url = '/pages/meeting-detail/meeting-detail?meeting=' + encodeURIComponent(JSON.stringify(t.data))
      } else if (t.type === 'works') {
        url = '/pages/work-detail/work-detail?work=' + encodeURIComponent(JSON.stringify(t.data))
      } else {
        url = '/pages/task-detail/task-detail?task=' + encodeURIComponent(JSON.stringify(t.data))
      }
      uni.navigateTo({ url })
    },

    // 点击澄清选项：作为一条新的用户消息发送（语音用户免打字）。
    // 带上clarifyReply标记，后端会禁止AI再次追问、直接用默认条件查询
    tapOption(opt) {
      if (this.loading) return
      // 会话ID未就绪(打开页面后历史对话恢复完成前的窗口期)：此时点选会以胶囊文字
      // 新开一个碎片会话，拦截并提示
      if (!this.sessionId) {
        uni.showToast({ title: '对话加载中，请稍后再点选', icon: 'none' })
        return
      }
      this.send(opt, { clarifyReply: true })
    },

    // 答案打字机效果
    typeAnswer(aiIndex, fullText) {
      const m = this.messages[aiIndex]
      let i = 0
      const timer = setInterval(() => {
        i = Math.min(fullText.length, i + 3)
        m.content = fullText.slice(0, i)
        this.scrollToBottom()
        if (i >= fullText.length) {
          clearInterval(timer)
          this.typeTimers = this.typeTimers.filter((t) => t !== timer)
        }
      }, 30)
      this.typeTimers.push(timer)
    },

    // 展开/收起思考过程
    toggleThink(index) {
      const m = this.messages[index]
      if (m && m.thinking && m.thinking.length) {
        m.thinkOpen = !m.thinkOpen
      }
    },

    scrollToBottom() {
      this.$nextTick(() => {
        // 属性方案兜底：值随消息数递增，变化才会触发scroll-view滚动
        this.scrollTop = this.messages.length * 99999 + 99999
        // H5主方案：对底部锚点scrollIntoView。uni的scroll-top属性在值不变时不重复触发
        // (打字机阶段消息数不变、内容持续长高)，且实际滚动容器是哪层DOM由uni实现决定，
        // scrollIntoView由浏览器自动滚动所有可滚动的祖先，最可靠
        if (typeof document !== 'undefined') {
          const anchor = document.querySelector('#chat-bottom')
          if (anchor && anchor.scrollIntoView) {
            anchor.scrollIntoView({ block: 'end' })
            // 打字机/DOM更新可能晚一帧，下一帧再校准一次
            requestAnimationFrame(() => anchor.scrollIntoView({ block: 'end' }))
          }
        }
      })
    },

    // 开启新对话：清空当前上下文并重置会话
    newChat() {
      if (this.loading) return
      this.messages = []
      this.sessionId = null
      this.scrollTop = 0
    },

    // 删除当前会话记录：服务端软删会话+物理删消息，成功后清空界面
    deleteCurrentSession() {
      if (!this.sessionId || this.loading) return
      uni.showModal({
        title: '删除记录',
        content: '确定删除当前这段对话记录吗？删除后不可恢复',
        success: async (r) => {
          if (!r.confirm) return
          try {
            await aiApi.deleteSession(this.sessionId)
            this.sessionId = null
            this.messages = []
            this.scrollTop = 0
            uni.showToast({ title: '已删除', icon: 'success' })
          } catch (e) {
            // 错误提示由 request 拦截器统一处理
          }
        }
      })
    },

    // ==================== 历史会话 ====================
    // 把落库消息映射为页面消息：解析tasks JSON（有则任务名可点击跳详情）、
    // 解析options JSON（有则历史消息下方的胶囊按钮仍可点）
    mapHistoryMsg(m) {
      let tasks = []
      if (m.tasks) {
        try {
          const parsed = typeof m.tasks === 'string' ? JSON.parse(m.tasks) : m.tasks
          if (Array.isArray(parsed)) tasks = parsed
        } catch (e) { /* 旧数据或损坏JSON，忽略 */ }
      }
      let options = []
      if (m.options) {
        try {
          const parsed = typeof m.options === 'string' ? JSON.parse(m.options) : m.options
          if (Array.isArray(parsed)) options = parsed
        } catch (e) { /* 旧数据或损坏JSON，忽略 */ }
      }
      return { id: m.id, role: m.role, content: m.content, tasks, options }
    },

    // 进入页面时默认恢复最近一次对话
    restoreLastSession() {
      aiApi.sessions({ pageNum: 1, pageSize: 1 }).then((res) => {
        const list = (res && res.data && res.data.list) || []
        if (list.length === 0 || this.sessionId) return
        const last = list[0]
        aiApi.messages(last.id).then((r) => {
          const msgs = (r && r.data) || []
          if (msgs.length === 0 || this.sessionId) return
          this.messages = msgs.map((m) => this.mapHistoryMsg(m))
          this.sessionId = last.id
          this.scrollToBottom()
        })
      }).catch(() => {})
    },

    openHistory() {
      this.showHistory = true
      aiApi.sessions({ pageNum: 1, pageSize: 50 }).then((res) => {
        this.sessions = (res && res.data && res.data.list) || []
      }).catch(() => {})
    },

    loadSession(s) {
      aiApi.messages(s.id).then((res) => {
        const list = (res && res.data) || []
        this.messages = list.map((m) => this.mapHistoryMsg(m))
        this.sessionId = s.id
        this.showHistory = false
        this.scrollToBottom()
      })
    },

    removeSession(s) {
      uni.showModal({
        title: '删除对话',
        content: '确定删除「' + (s.title || '新对话') + '」吗？删除后不可恢复',
        success: async (r) => {
          if (!r.confirm) return
          await aiApi.deleteSession(s.id)
          this.sessions = this.sessions.filter((x) => x.id !== s.id)
          if (this.sessionId === s.id) {
            this.sessionId = null
            this.messages = []
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  /* 100dvh：动态视口高度，随手机浏览器地址栏收起/展开自适应，
     修复"输入栏被浏览器工具栏挡住、要上滑才能看到"的问题；
     100vh 是旧浏览器兜底（移动端 100vh 含地址栏后方区域，会偏高） */
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  overflow: hidden;
}

.chat-list {
  flex: 1;
  min-height: 0; /* 关键：允许 flex 子项收缩，内部滚动才生效 */
  overflow-y: auto;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.chat-tip {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  line-height: 1.8;
  padding: 60rpx 40rpx;
  white-space: pre-line;
}

.msg-row {
  display: flex;
  flex-wrap: wrap; /* 允许"删除"入口换行到气泡下方 */
  margin-bottom: 20rpx;

  &.row-right {
    justify-content: flex-end;
  }

  &.row-left {
    justify-content: flex-start;
  }
}

/* 回答中可点击的任务名链接 */
.task-link {
  color: #1a73e8;
  text-decoration: underline;
}

/* 澄清选项胶囊按钮 */
.option-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 16rpx;
}

.option-chip {
  padding: 10rpx 26rpx;
  border: 2rpx solid #1a73e8;
  border-radius: 40rpx;
  background-color: #fff;
  color: #1a73e8;
  font-size: 26rpx;
  line-height: 1.5;
}

.option-chip.chip-disabled {
  opacity: 0.5;
}

/* 删除这一轮的入口（用户消息气泡下方，灰色小字） */
.round-del {
  width: 100%;
  text-align: right;
  font-size: 22rpx;
  color: #9aa3af;
  padding: 4rpx 8rpx 0 0;
}

.bubble {
  max-width: 76%;
  padding: 18rpx 24rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.7;
  word-break: break-all;
  white-space: pre-wrap;
}

.bubble-user {
  background-color: #1a73e8;
  color: #fff;
  border-top-right-radius: 4rpx;
}

.bubble-ai {
  background-color: #fff;
  color: #333;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.06);
  border-top-left-radius: 4rpx;
}

.bubble-loading {
  color: #999;
}

/* 思考过程区块 */
.think-box {
  margin-bottom: 12rpx;
  padding: 14rpx 20rpx;
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f0fe 100%);
  border: 1rpx solid #d6e4ff;
  border-radius: 12rpx;
}

.think-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.think-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  flex-shrink: 0;

  &.dot-thinking {
    background: #1a73e8;
    animation: think-pulse 1s ease-in-out infinite;
  }

  &.dot-done {
    background: #52c41a;
  }
}

.think-title {
  font-size: 24rpx;
  color: #1a73e8;
  font-weight: 600;
}

.think-arrow {
  margin-left: auto;
  font-size: 20rpx;
  color: #88a8e8;
}

.think-steps {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.think-step {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  animation: step-in 0.3s ease;
}

.step-dot {
  font-size: 20rpx;
  color: #4d9fff;
  line-height: 34rpx;
  flex-shrink: 0;
}

.step-text {
  font-size: 24rpx;
  color: #5a6a85;
  line-height: 34rpx;
}

@keyframes think-pulse {
  0%, 100% { opacity: 0.35; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.15); }
}

@keyframes step-in {
  from { opacity: 0; transform: translateY(8rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-bottom {
  height: 20rpx;
}

.recognize-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.recognize-box {
  background-color: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 48rpx 56rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  min-width: 420rpx;
  max-width: 70%;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;

  /* 左滑取消态：红色提示（微信同款） */
  &.recognize-cancel {
    background-color: rgba(229, 72, 77, 0.92);
  }

  /* 右滑转文字态：蓝色提示 */
  &.recognize-totext {
    background-color: rgba(26, 115, 232, 0.92);
  }
}

/* 左右滑动提示区：✕取消 —— 🎤 —— 文转文字 */
.recognize-icons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48rpx;
}

.rec-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  opacity: 0.45; // 未激活时半透明，微信同款弱化效果

  &.rec-side-active {
    opacity: 1;
  }
}

.rec-side-label {
  font-size: 22rpx;
}

/* "文"字图标（转文字） */
.rec-char {
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  border: 2rpx solid #fff;
  border-radius: 8rpx;
}

.recognize-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 录音中麦克风呼吸动画 */
.mic-pulse {
  animation: mic-pulse 1.2s ease-in-out infinite;
}

@keyframes mic-pulse {
  0%, 100% { opacity: 0.55; transform: scale(0.92); }
  50% { opacity: 1; transform: scale(1.08); }
}

.recognize-title {
  font-size: 30rpx;
  font-weight: 600;
}

.recognize-hint {
  font-size: 24rpx;
  color: #ddd;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  box-shadow: 0 -2rpx 8rpx rgba(0, 0, 0, 0.06);
}

.chat-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 28rpx;
  padding: 12rpx 24rpx 0;
  background-color: #f0f2f5;
}

.toolbar-btn {
  font-size: 24rpx;
  color: #1a73e8;
  padding: 6rpx 4rpx;
}

.toolbar-danger {
  color: #e5484d;
}

.history-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.history-panel {
  width: 100%;
  max-height: 70vh;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.history-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.history-close {
  font-size: 26rpx;
  color: #999;
}

.history-list {
  flex: 1;
  max-height: calc(70vh - 100rpx);
}

.history-empty {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  padding: 80rpx 0;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 26rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.history-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  overflow: hidden;
}

.history-item-title {
  font-size: 28rpx;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item-time {
  font-size: 22rpx;
  color: #aaa;
}

.history-del {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #e54d42;
  padding: 8rpx 0 8rpx 20rpx;
}

/* 左侧语音/键盘切换图标（微信式）：小一号更精致，颜色柔和不抢输入框焦点 */
.bar-icon {
  flex-shrink: 0;
  width: 60rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 语音模式整条"按住说话"按钮（微信式） */
.hold-talk {
  flex: 1;
  height: 68rpx;
  line-height: 68rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
  background-color: #fff;
  border: 1rpx solid #dcdfe6;
  border-radius: 10rpx;
  user-select: none;

  &.hold-talk-active {
    background-color: #dcdfe6; // 按下态，微信同款变灰
  }

  &.hold-talk-cancel {
    background-color: #fde3e3;
    border-color: #e5484d;
    color: #e5484d;
  }

  &.hold-talk-totext {
    background-color: #e8f0fe;
    border-color: #1a73e8;
    color: #1a73e8;
  }
}

.chat-input {
  flex: 1;
  height: 68rpx;
  padding: 0 20rpx;
  background-color: #fff;
  border: 1rpx solid #dcdfe6;
  border-radius: 10rpx;
  font-size: 28rpx;
}

.send-btn {
  flex-shrink: 0;
  margin: 0;
  padding: 0 26rpx;
  height: 68rpx;
  line-height: 68rpx;
  font-size: 26rpx;
  color: #fff;
  background-color: #1a73e8;
  border-radius: 10rpx;

  &[disabled] {
    background-color: #9bbff7;
    color: #fff;
  }
}
</style>
