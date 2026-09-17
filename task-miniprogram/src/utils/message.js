/**
 * 消息提示工具
 * 统一管理提示信息
 */

export const Message = {
  success(title, duration = 2000) {
    uni.showToast({
      title,
      icon: 'success',
      duration
    })
  },

  error(title, duration = 2000) {
    uni.showToast({
      title,
      icon: 'none',
      duration
    })
  },

  warning(title, duration = 2000) {
    uni.showToast({
      title,
      icon: 'none',
      duration
    })
  },

  info(title, duration = 2000) {
    uni.showToast({
      title,
      icon: 'none',
      duration
    })
  }
}

export default Message
