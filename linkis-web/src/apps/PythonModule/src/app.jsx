import { defineRuntimeConfig } from '@fesjs/fes';
import { FMessage } from '@fesjs/fes-design';
import PageLoading from '@/letgo/components/pageLoading.vue';
import { useSharedLetgoGlobal } from '@/letgo/useLetgoGlobal';

export default defineRuntimeConfig({
  render: async (lastRender) => {
    return lastRender();
  },
  beforeRender: {
    loading: <PageLoading />,
    async action () {
      const global = useSharedLetgoGlobal();
      await global.beforeRender?.();
      const userInfo = global.$context.userInfo;
    }
  },

  request: {
    mergeRequest: true,
    requestInterceptors: async (config) => {
      if (config.headers) {
        config.headers['Content-language'] = localStorage.getItem('locale') || 'zh-CN';
      }
      return config;
    },
    errorHandler (error) {
      if (error.response) {
        // 请求成功发出且服务器也响应了状态码，但状态代码超出了 2xx 的范围
        console.log(error.response.data);
        console.log(error.response.status);
        console.log(error.response.headers);
        FMessage.error(error.response?.data?.message || '服务器错误');
        if ([401, 403].includes(error.response.status)) {
          window.parent.postMessage('Unauhorized', '*');
        }
      } else if (error.request) {
        // 请求已经成功发起，但没有收到响应
        // `error.request` 在浏览器中是 XMLHttpRequest 的实例，
        // 而在node.js中是 http.ClientRequest 的实例
        console.log(error.request);
      } else if (error.type) {
        // 插件异常
        console.log(error.msg);
      } else {
        // 发送请求时出了点问题
        console.log('Error', error.message);
      }
      console.log(error.config);
    }
  }
});
