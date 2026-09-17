const path = require('path')
const fs = require('fs')
const os = require('os')

// 自动探测本机局域网IP，用于 devServer.public（启动横幅展示给别人访问的地址）。
// vue-cli 自带的探测在 Win11 上会失败（它依赖已被微软移除的 wmic 命令，显示 Network: unavailable），
// 这里先走它同款的"默认网关网卡"探测，失败再退回网卡枚举并排除常见虚拟网卡。
const detectLanIp = () => {
  // 虚拟网卡关键词（VMware/VirtualBox/Hyper-V/MobaXterm/WSL 等），这些地址给局域网用户访问是无效的
  const VIRTUAL_NIC = /vmware|vmnet|virtualbox|vethernet|hyper-v|loopback|moba|wsl|tap/i
  const isPrivateV4 = (ip) => /^(10[.]|172[.](1[6-9]|2[0-9]|3[0-1])[.]|192[.]168[.])/.test(ip)

  // 方案一：找默认网关所在的网卡（跨网络环境最准，但 Win11 无 wmic 时会抛异常）
  try {
    const gateway = require('default-gateway').v4.sync()
    const nic = gateway && os.networkInterfaces()[gateway.interface]
    const hit = nic && nic.find(i => i.family === 'IPv4' && !i.internal && isPrivateV4(i.address))
    if (hit) return hit.address
  } catch (_) { /* 走方案二 */ }

  // 方案二：枚举物理网卡，取第一个私网 IPv4
  for (const [name, addrs] of Object.entries(os.networkInterfaces())) {
    if (VIRTUAL_NIC.test(name)) continue
    const hit = addrs && addrs.find(i => i.family === 'IPv4' && !i.internal && isPrivateV4(i.address))
    if (hit) return hit.address
  }
  return undefined
}

// ==================== 核心IP配置（修改这里即可） ====================
const CORE_CONFIG = {
  // 服务器基础IP地址（统一配置，一处修改全局生效）
  SERVER_IP: 'localhost',          // 🔑 主服务器IP（本机部署）

  // 端口配置
  PORTS: {
    BACKEND: '9090',                // 后端API端口
    FRONTEND: '8081'                // 前端H5端口
  }
};
// =====================================================================

const LAN_IP = detectLanIp()

// 生成后端服务器URL
const getBackendUrl = () => `http://${CORE_CONFIG.SERVER_IP}:${CORE_CONFIG.PORTS.BACKEND}`

// 代理配置：与生产环境 nginx 保持一致，/api 前缀转发时剥掉
const generateProxyConfig = (target) => ({
  '/api': {
    target: target,
    changeOrigin: true,
    secure: false,
    pathRewrite: { '^/api': '' }
  }
})

module.exports = {
  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    }
  },
  devServer: {
    host: '0.0.0.0',
    port: 8080,               // manifest.json的devServer已移到这里统一维护(uni-app以manifest优先会覆盖此文件)
    // 对外访问地址：探测到局域网IP就动态填入（不写死，换网络环境自动跟随），
    // 启动横幅的 Network 行即别人在局域网访问的链接；探测失败则留空回退默认行为
    ...(LAN_IP ? { public: `${LAN_IP}:8080` } : {}),
    disableHostCheck: true,
    proxy: generateProxyConfig(getBackendUrl()),
    // HTTPS(自签名证书)：手机等局域网设备访问需安全上下文才能使用麦克风(语音输入)。
    // 注意：webpack-dev-server 3的https只认文件内容(Buffer)不认路径字符串；
    // 证书SAN含局域网IP(10.239.170.249)；Android浏览器点一次"继续访问"即可，
    // iOS需把dev-certs/dev-cert.pem安装为受信任的证书描述文件
    https: {
      key: fs.readFileSync(path.resolve(__dirname, 'dev-certs/dev-cert.key')),
      cert: fs.readFileSync(path.resolve(__dirname, 'dev-certs/dev-cert.pem'))
    }
  }
}
