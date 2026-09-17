import URLS from "@/config/URLS";
/**
 * API 配置文件
 * 统一管理后端接口地址
 */



// 开发环境配置
const development = {
  baseURL: URLS.devBaseURL, // 开发环境API地址
  // baseURL:'/api',
  apiTimeout: 30000  // 修复：增加到30秒，防止ClientAbortException（原15秒太短）
}

// 生产环境配置
const production = {

  baseURL: URLS.proBaseURL, // 生产环境API地址
  // baseURL:'/api',
  apiTimeout: 30000  // 修复：增加到30秒，防止ClientAbortException（原15秒太短）
}

// 根据环境自动选择配置
const ENV = process.env.NODE_ENV === 'production' ? 'production' : 'development'

const config = {
  development,
  production
}

// 导出当前环境的配置对象
const currentConfig = config[ENV]

export default currentConfig


