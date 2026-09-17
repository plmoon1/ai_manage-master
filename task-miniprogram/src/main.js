import Vue from 'vue'
import uView from '@/uni_modules/uview-ui'
import App from './App'
import './uni.promisify.adaptor'
import './utils/request' // 引入请求拦截器
import BottomNav from './components/BottomNav' // 引入底部导航组件

Vue.config.productionTip = false
Vue.use(uView)

// 注册全局组件
Vue.component('BottomNav', BottomNav)
App.mpType = 'app'

const app = new Vue({
  ...App
})
app.$mount()
