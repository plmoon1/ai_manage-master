<template>
  <view class="tabbar-container">
    <view class="tabbar-content">

      <view class="tab-item" :class="{ active: activePath === '/pages/index/index' }" @tap="switchTab('/pages/index/index')">
        <view class="icon-box">
          <u-icon
            :name="activePath === '/pages/index/index' ? 'file-text-fill' : 'file-text'"
            :color="activePath === '/pages/index/index' ? '#0210fc' : '#909399'"
            size="28">
          </u-icon>
        </view>
        <text class="tab-label">任务</text>
      </view>

      <view class="tab-item" :class="{ active: activePath === '/pages/meeting/meeting' }" @tap="switchTab('/pages/meeting/meeting')">
        <view class="icon-box">
          <u-icon
            :name="activePath === '/pages/meeting/meeting' ? 'calendar-fill' : 'calendar'"
            :color="activePath === '/pages/meeting/meeting' ? '#0210fc' : '#909399'"
            size="28">
          </u-icon>
        </view>
        <text class="tab-label">会议</text>
      </view>

      <view class="tab-item" :class="{ active: activePath === '/pages/works/works' }" @tap="switchTab('/pages/works/works')">
        <view class="icon-box">
          <u-icon
            :name="activePath === '/pages/works/works' ? 'star-fill' : 'star'"
            :color="activePath === '/pages/works/works' ? '#0210fc' : '#909399'"
            size="28">
          </u-icon>
        </view>
        <text class="tab-label">重点工作</text>
      </view>

      <view class="tab-item" :class="{ active: activePath === '/pages/user/user' }" @tap="switchTab('/pages/user/user')">
        <view class="icon-box">
          <u-icon
            :name="activePath === '/pages/user/user' ? 'account-fill' : 'account'"
            :color="activePath === '/pages/user/user' ? '#0210fc' : '#909399'"
            size="28">
          </u-icon>
        </view>
        <text class="tab-label">我的</text>
      </view>

    </view>
    <view class="safe-area-bottom"></view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activePath: ''
    };
  },
  methods: {
    updateActivePath() {
      const pages = getCurrentPages();
      if (pages.length > 0) {
        const currPage = pages[pages.length - 1];
        // H5端可以通过这种方式获取当前路径
        this.activePath = '/' + (currPage.route || currPage.__route__);
      }
    },
    switchTab(url) {
      if (this.activePath === url) return;

      // 使用 redirectTo 替换当前页面
      uni.redirectTo({
        url: url,
        success: () => {
          this.activePath = url;
        }
      });
    }
  },
  // 监听路由变化，防止用户点击浏览器返回键时，高亮不更新
  watch: {
    activePath(newVal) {
      console.log('Current Active:', newVal);
    }
  },
  // 组件挂载时更新路径
  mounted() {
    this.updateActivePath();
  },
  // 页面显示时更新激活状态
  onShow() {
    this.updateActivePath();
  }
}
</script>

<style lang="scss" scoped>
.tabbar-container {
  position: fixed;
  bottom: 1rpx; // 距离底部高度，产生悬浮感
  left: 30rpx;
  right: 30rpx;
  z-index: 999;
}

.tabbar-content {
  height: 110rpx;
  background: rgba(248, 247, 247, 0.911);
  backdrop-filter: blur(30px);
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
  
  /* --- 关键修改：用投影模拟白色边框 --- */
  /* 第一层：0扩散，给一个纯白色的“外圈”，模拟 4px 的边框 */
  /* 第二层：正常的阴影，用来把白色边框衬托出来 */
  box-shadow: 
    0 0 0 6rpx #FFFFFF, 
    0 10rpx 30rpx rgba(0, 0, 0, 0.2);
    
  /* 确保不会被父容器切掉边框 */
  margin: 10rpx 0; 
  box-sizing: border-box; /* 必须加 */
  border: 0.5rpx solid #FFFFFF !important;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  transition: all 0.2s ease;
}

  .icon-box {
    font-size: 32rpx;
    height: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8rpx;
    color: #909399; // 默认灰色
  }

  .tab-label {
    font-size: 22rpx;
    color: #909399;
    font-weight: 500;
  }

  // 激活状态：蓝色
  .active {
    .icon-box {
      color: #0046f5;
      transform: scale(1.05); // 激活时微放大
    }
    .tab-label {
      color: #0046f5;
    }
  }

  .active {
    opacity: 0.7; // 点击时的反馈
  }


.safe-area-bottom {
  // 适配 iOS 底部横条
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>