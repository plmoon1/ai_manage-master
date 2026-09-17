<template>
  <view class="aibao-float" :style="{ bottom: bottom }" @tap="goAi">
    <!-- 把吉祥物图片放到 /static/aibao.png 即自动替换占位样式 -->
    <image
      v-if="imgOk"
      class="aibao-img"
      src="/static/aibao.png"
      mode="aspectFit"
      @error="imgOk = false"
    ></image>
    <view v-else class="aibao-placeholder">
      <text class="aibao-emoji">🤖</text>
      <text class="aibao-name">爱宝</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'aibao-float',
  props: {
    // 距底部位置，页面有其他悬浮按钮时可传入更高的值避开
    bottom: {
      type: String,
      default: '140rpx'
    }
  },
  data() {
    return {
      imgOk: true
    }
  },
  methods: {
    goAi() {
      uni.navigateTo({
        url: '/pages/ai/ai-chat'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.aibao-float {
  position: fixed;
  right: 30rpx;
  bottom: 140rpx;
  width: 110rpx;
  height: 110rpx;
  z-index: 998;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: aibao-float-in 0.4s ease;
}

.aibao-img {
  width: 110rpx;
  height: 110rpx;
  filter: drop-shadow(0 6rpx 12rpx rgba(0, 0, 0, 0.18));
  cursor: pointer;
}

.aibao-placeholder {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a73e8, #4d9fff);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(26, 115, 232, 0.4);
  cursor: pointer;
}

.aibao-emoji {
  font-size: 40rpx;
  line-height: 1;
}

.aibao-name {
  font-size: 20rpx;
  color: #fff;
  margin-top: 4rpx;
}

@keyframes aibao-float-in {
  from {
    opacity: 0;
    transform: translateY(30rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
