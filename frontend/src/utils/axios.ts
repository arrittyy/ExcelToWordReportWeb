import axios from 'axios';
import { message } from 'antd';
import {
  SESSION_EXPIRED_MESSAGE,
  shouldTreat403AsSessionExpired,
} from './authErrors';

/** 扩展 Axios 请求配置（拦截器中读取） */
declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 404 时不弹全局「资源不存在」 */
    suppressNotFoundMessage?: boolean;
    /** 5xx 时不弹全局「服务器错误」（可选/探测类请求由调用方 catch 处理） */
    suppressServerErrorMessage?: boolean;
    /** 403 时不弹全局「没有权限」（探测类请求由调用方 catch 处理） */
    suppressForbiddenMessage?: boolean;
  }
}

// 开发环境必须用相对路径 /api，走 vite.config.ts 里 proxy 到本机 8080，避免浏览器直连 8080 触发 CORS。
// 生产环境用 /api，由 Nginx 等反代到后端。
const getApiBaseURL = () => '/api';

/** 项目详情等 JSON 接口默认 30s 不够时用（与 Nginx proxy_read_timeout 对齐部署） */
export const JSON_FETCH_LONG_TIMEOUT_MS = 120000;

/** Word 导出/下载类路径：超时提示保留「第三方报告」等说明 */
function isWordHeavyApiUrl(url: string | undefined): boolean {
  if (!url) return false;
  return /word-export-jobs|generate-word|generate-summary-word|generate-third-party-word|detection-notification-word|batch-generate-word/i.test(
    url,
  );
}

function handleSessionExpired(): void {
  message.error(SESSION_EXPIRED_MESSAGE);
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  window.location.href = '/login';
}

const apiClient = axios.create({
  baseURL: getApiBaseURL(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      // 调试日志（开发环境）
      if (import.meta.env.DEV) {
        console.log('🔑 [API Request] Token:', token.substring(0, 20) + '...', 'URL:', config.url);
      }
    } else {
      console.warn('⚠️ [API Request] No token found');
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      
      if (status === 401) {
        const reqUrl = error.config?.url || '';
        const isLoginAttempt = /\/auth\/login\b/.test(reqUrl);
        if (isLoginAttempt) {
          // 登录页的账号/密码错误也是 401，交给页面展示具体文案，勿清 token、勿整页跳转（否则 message 易被冲掉）
          return Promise.reject(error);
        }
        handleSessionExpired();
      } else if (status === 403) {
        console.error('❌ [API Error] 403 Forbidden:', {
          url: error.config?.url,
          method: error.config?.method,
          hasToken: !!localStorage.getItem('token'),
          response: data
        });
        if (!error.config?.suppressForbiddenMessage) {
          const hasToken = !!localStorage.getItem('token');
          if (shouldTreat403AsSessionExpired(data, hasToken)) {
            handleSessionExpired();
          } else {
            const msg =
              typeof data?.message === 'string' && data.message.trim()
                ? data.message
                : '没有权限执行此操作';
            message.error(msg);
          }
        }
      } else if (status === 404) {
        if (!error.config?.suppressNotFoundMessage) {
          message.error('请求的资源不存在');
        }
      } else if (status >= 500) {
        if (!error.config?.suppressServerErrorMessage) {
          message.error('服务器错误，请稍后重试');
        }
      } else if (data && data.message) {
        message.error(data.message);
      }
    } else if (
      error.code === 'ECONNABORTED' ||
      (typeof error.message === 'string' && /timeout/i.test(error.message))
    ) {
      const reqUrl = error.config?.url || '';
      if (isWordHeavyApiUrl(reqUrl)) {
        message.error(
          '请求超时。第三方报告若附图很多、文件很大，生成与下载可能需十余分钟；请稍后重试，并在 Nginx 上将 proxy_read_timeout / proxy_send_timeout 调到与前端 Word 导出超时一致（参见 deploy/应用更新指南）',
        );
      } else {
        message.error(
          '请求超时。项目或报告数据量较大、或网络较慢时，加载可能较慢；请稍后重试。若经常超时，请让管理员调大接口与 Nginx proxy_read_timeout。',
        );
      }
    } else if (error.request) {
      message.error('网络错误，请检查您的连接');
    } else {
      message.error('请求失败');
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
