<template>
  <view class="refresh-indicator" :class="{ 'show': isRefreshing, 'has-error': hasError }">
    <view class="indicator-content">
      <!-- 刷新动画 -->
      <view v-if="isRefreshing && !hasError" class="refresh-animation">
        <view class="spinner"></view>
        <view class="dots">
          <view class="dot" v-for="i in 3" :key="i"></view>
        </view>
      </view>

      <!-- 刷新成功 -->
      <view v-else-if="refreshSuccess" class="refresh-success">
        <view class="success-icon">✓</view>
        <text class="success-text">刷新完成</text>
      </view>

      <!-- 刷新失败 -->
      <view v-else-if="hasError" class="refresh-error">
        <view class="error-icon">!</view>
        <text class="error-text">刷新失败</text>
      </view>

      <!-- 提示文本 -->
      <text v-if="tipText" class="tip-text">{{ tipText }}</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'RefreshIndicator',
  props: {
    // 是否正在刷新
    isRefreshing: {
      type: Boolean,
      default: false
    },
    // 刷新是否成功
    refreshSuccess: {
      type: Boolean,
      default: false
    },
    // 是否有错误
    hasError: {
      type: Boolean,
      default: false
    },
    // 提示文本
    tipText: {
      type: String,
      default: ''
    }
  }
}
</script>

<style lang="scss" scoped>
.refresh-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;

  &.show {
    opacity: 1;
  }

  &.has-error {
    .indicator-content {
      background: rgba(255, 77, 79, 0.95);
    }
  }
}

.indicator-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
  padding: 20rpx 30rpx;
  background: rgba(2, 60, 153, 0.95);
  border-radius: 0 0 24rpx 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10rpx);
  transition: background 0.3s ease;
}

.refresh-animation {
  display: flex;
  align-items: center;
  gap: 16rpx;

  .spinner {
    width: 40rpx;
    height: 40rpx;
    border: 4rpx solid rgba(255, 255, 255, 0.3);
    border-top-color: #ffffff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .dots {
    display: flex;
    gap: 8rpx;

    .dot {
      width: 12rpx;
      height: 12rpx;
      background: #ffffff;
      border-radius: 50%;
      animation: dotBounce 1.4s infinite ease-in-out both;

      &:nth-child(1) {
        animation-delay: -0.32s;
      }

      &:nth-child(2) {
        animation-delay: -0.16s;
      }
    }
  }
}

.refresh-success {
  display: flex;
  align-items: center;
  gap: 12rpx;

  .success-icon {
    width: 40rpx;
    height: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(82, 196, 26, 0.9);
    border-radius: 50%;
    color: #ffffff;
    font-size: 28rpx;
    font-weight: bold;
  }

  .success-text {
    color: #ffffff;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.refresh-error {
  display: flex;
  align-items: center;
  gap: 12rpx;

  .error-icon {
    width: 40rpx;
    height: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 77, 79, 0.9);
    border-radius: 50%;
    color: #ffffff;
    font-size: 32rpx;
    font-weight: bold;
  }

  .error-text {
    color: #ffffff;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.tip-text {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.8);
  font-size: 24rpx;
  text-align: center;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes dotBounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>