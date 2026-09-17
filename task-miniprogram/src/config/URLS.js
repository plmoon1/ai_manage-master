/**
 * 服务器IP地址配置类
 * 统一管理系统所有服务器地址和端口配置
 */

// ==================== 核心IP配置（修改这里即可） ====================
// SERVER_IP 默认跟随当前访问页面的主机名动态推导(局域网部署)：
//   本机访问 http://localhost:8080 → API指向 http://localhost:9090
//   手机/其他电脑访问 http://10.239.170.249:8080 → API自动指向 http://10.239.170.249:9090
// 如需固定指向某台服务器，改成对应IP字符串即可(如 '10.239.170.249')
const resolveServerIp = () => {
    if (typeof location !== 'undefined' && location.hostname &&
        location.hostname !== '0.0.0.0' && location.hostname !== '[::1]') {
        return location.hostname
    }
    return 'localhost'
}

const CORE_CONFIG = {
    // 服务器基础IP地址（统一配置，一处修改全局生效）
    SERVER_IP: resolveServerIp(),
    // 🔑 主服务器IP（局域网部署：默认动态跟随访问主机）

    // 端口配置
    PORTS: {
        BACKEND: '9090',                // 后端API端口
        FRONTEND: '8081'                // 前端H5端口
    }
};
// =====================================================================

const SERVER_CONFIG = {
    // 核心配置（引用统一配置）
    CORE: CORE_CONFIG,

    // 环境类型
    ENV: {
        DEVELOPMENT: 'development',
        PRODUCTION: 'production'
    },

    // 服务器配置（自动从核心配置生成）
    SERVER: {
        // 后端API服务器配置
        get BACKEND_HOST() {
            return CORE_CONFIG.SERVER_IP;
        },
        get BACKEND_PORT() {
            return CORE_CONFIG.PORTS.BACKEND;
        },

        // 前端H5开发服务器配置
        FRONTEND_HOST: '0.0.0.0',  // 允许所有网络接口访问
        get FRONTEND_PORT() {
            return CORE_CONFIG.PORTS.FRONTEND;
        }
    },

    // 完整的URL配置（自动从核心配置生成）
    URLS: {
        // 开发环境后端API地址：走同源 /api，由 devServer 代理转发到后端(vue.config.js)。
        // 原因：前端开启HTTPS后，浏览器禁止HTTPS页面直连HTTP后端(混合内容拦截)；
        // 同源相对路径在开发代理和生产nginx(backend/nginx.conf 的 /api)下行为一致
        get devBaseURL() {
            return '/api';
        },

        // 生产环境后端API地址：同样走同源 /api，由 nginx 转发
        get proBaseURL() {
            return '/api';
        },

        // 前端本地访问地址
        get localURL() {
            return `http://localhost:${CORE_CONFIG.PORTS.FRONTEND}`;
        },

        // 前端网络访问地址（根据实际网络IP自动生成）
        getNetworkURL() {
            return `http://${CORE_CONFIG.SERVER_IP}:${CORE_CONFIG.PORTS.FRONTEND}`;
        },

        // 获取当前网络IP
        getNetworkIP() {
            return CORE_CONFIG.SERVER_IP;
        }
    },

    // API路径配置
    API_PATHS: {
        LOGIN: '/login',
        SYS_DICT: '/sysdict/getAllSysdicByTid',
        PLAN: '/plan',
        WORKS: '/works',
        FILES: '/files',
        TENANT: '/tenant',
        DEPARTMENT: '/department',
        USER: '/user',
        LOG: '/log',
        ROLE: '/role',
        COLLEGE: '/college',
        MEETING: '/meeting',
        ADMIN: '/admin'
    },

    // 获取完整的API URL
    getApiUrl(path = '') {
        const baseURL = process.env.NODE_ENV === 'production'
            ? this.URLS.proBaseURL
            : this.URLS.devBaseURL;
        return baseURL + path;
    },

    // 获取后端服务器地址（不含协议）
    getBackendServer() {
        return `${this.SERVER.BACKEND_HOST}:${this.SERVER.BACKEND_PORT}`;
    },

    // 获取前端服务器地址（不含协议）
    getFrontendServer() {
        return `${this.SERVER.FRONTEND_HOST}:${this.SERVER.FRONTEND_PORT}`;
    },

    // 获取服务器IP
    getServerIP() {
        return CORE_CONFIG.SERVER_IP;
    },

    // 设置服务器IP（运行时动态修改）
    setServerIP(ip) {
        CORE_CONFIG.SERVER_IP = ip;
    }
};

// 向后兼容：保持原有URLS结构
const URLS = {
    get devBaseURL() {
        return SERVER_CONFIG.URLS.devBaseURL; // 使用http://localhost:9090
    },
    get proBaseURL() {
        return SERVER_CONFIG.URLS.proBaseURL; // 使用http://localhost:9090
    }
};

export default URLS;
export { SERVER_CONFIG, CORE_CONFIG };