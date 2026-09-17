<template>
  <view class="refresh-scroll-container">
    <scroll-view
      class="refresh-scroll"
      :scroll-y="scrollY"
      :scroll-x="scrollX"
      :upper-threshold="upperThreshold"
      :refresher-enabled="refresherEnabled"
      :refresher-triggered="isRefreshing"
      :refresher-threshold="refresherThreshold"
      :refresher-default-style="refresherDefaultStyle"
      :refresher-background="refresherBackground"
      :enhanced="enhanced"
      :bounces="bounces"
      :show-scrollbar="showScrollbar"
      @scroll="onScroll"
      @scrolltoupper="onScrollToUpper"
      @refresherrefresh="onRefresh"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    >
      <slot></slot>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'RefreshScrollView',
  props: {
    // 滚动方向
    scrollY: {
      type: Boolean,
      default: true
    },
    scrollX: {
      type: Boolean,
      default: false
    },
    // 下拉刷新相关
    refresherEnabled: {
      type: Boolean,
      default: true
    },
    refresherThreshold: {
      type: Number,
      default: 100
    },
    // 触发刷新的阈值（距离顶部的距离）
    upperThreshold: {
      type: Number,
      default: 50
    },
    // 增强特性
    enhanced: {
      type: Boolean,
      default: true
    },
    bounces: {
      type: Boolean,
      default: false
    },
    // 样式配置
    refresherDefaultStyle: {
      type: String,
      default: 'black'
    },
    refresherBackground: {
      type: String,
      default: '#f8f9fb'
    },
    showScrollbar: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      // 刷新状态
      isRefreshing: false,
      // 防误触相关
      touchStartTime: 0,
      touchStartY: 0,
      isPulling: false,
      pullDistance: 0,
      // 优化相关
      lastScrollTop: 0,
      isNearTop: false,
      refreshDisabled: false // 临时禁用刷新
    }
  },
  methods: {
    // 滚动事件处理
    onScroll(e) {
      const scrollTop = e.detail.scrollTop

      // 判断是否接近顶部
      this.isNearTop = scrollTop <= this.upperThreshold

      // 向下滚动时重新启用刷新
      if (scrollTop > this.lastScrollTop && this.isNearTop) {
        this.refreshDisabled = false
      }

      this.lastScrollTop = scrollTop

      // 向父组件传递滚动事件
      this.$emit('scroll', e)
    },

    // 滚动到顶部
    onScrollToUpper(e) {
      // 滚动到顶部时重新启用刷新
      this.refreshDisabled = false
      this.$emit('scrolltoupper', e)
    },

    // 触摸开始
    onTouchStart(e) {
      const touch = e.touches[0]
      this.touchStartTime = Date.now()
      this.touchStartY = touch.clientY
      this.isPulling = false
      this.pullDistance = 0
    },

    // 触摸移动
    onTouchMove(e) {
      if (!this.refresherEnabled || this.isRefreshing) return

      const touch = e.touches[0]
      const currentY = touch.clientY
      const pullDistance = currentY - this.touchStartY

      // 只有向下拉且距离顶部很近时才认为是下拉刷新手势
      if (pullDistance > 0 && this.isNearTop) {
        // 简化判断：只要向下拉且距离顶部很近，就认为是下拉手势
        this.isPulling = true
        this.pullDistance = pullDistance

        // 简化防误触逻辑：只有在极短时间且极大距离时才认为是误触
        const pullDuration = Date.now() - this.touchStartTime
        if (pullDuration < 100 && pullDistance > 100) {
          // 极快速拉大距离，可能是误触
          this.refreshDisabled = true
          this.isPulling = false
        }
        // 移除了对 pullDistance < 30 的限制，让小距离下拉也能触发
      } else {
        this.isPulling = false
        this.pullDistance = 0
      }
    },

    // 触摸结束
    onTouchEnd(e) {
      // 短暂延迟后重置状态
      setTimeout(() => {
        this.isPulling = false
        this.pullDistance = 0
        this.touchStartTime = 0
        this.touchStartY = 0
      }, 100)
    },

    // 下拉刷新
    async onRefresh() {
      console.log('触发下拉刷新，当前状态:', {
        refreshDisabled: this.refreshDisabled,
        isPulling: this.isPulling,
        isNearTop: this.isNearTop,
        pullDistance: this.pullDistance
      })

      // 简化刷新条件：只有在明确禁用时才阻止刷新
      if (this.refreshDisabled) {
        console.log('刷新被禁用，跳过刷新')
        // 停止刷新动画
        this.isRefreshing = true
        setTimeout(() => {
          this.isRefreshing = false
        }, 300)
        return
      }

      console.log('开始执行下拉刷新')
      this.isRefreshing = true
      this.refreshDisabled = true

      try {
        // 向父组件通知刷新事件，等待父组件完成
        await this.$emit('refresh')

        console.log('刷新完成')
      } catch (error) {
        console.error('刷新失败:', error)
      } finally {
        // 延迟关闭刷新状态
        setTimeout(() => {
          this.isRefreshing = false
          // 等待一段时间后重新启用刷新
          setTimeout(() => {
            this.refreshDisabled = false
          }, 500)
        }, 500)
      }
    },

    // 对外暴露的刷新完成方法（供父组件调用）
    refreshComplete() {
      this.isRefreshing = false
      setTimeout(() => {
        this.refreshDisabled = false
      }, 500)
    },

    // 对外暴露的刷新开始方法
    refreshStart() {
      this.isRefreshing = true
      this.refreshDisabled = true
    }
  }
}
</script>

<style lang="scss" scoped>
.refresh-scroll-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.refresh-scroll {
  width: 100%;
  height: 100%;
}
</style>