export const SESSION_EXPIRED_MESSAGE = '登录已过期，请重新登录';

/** Spring 默认或未配置异常处理时常见的未认证英文文案 */
const UNAUTHENTICATED_ENGLISH_PATTERNS = [
  /^forbidden$/i,
  /^access denied$/i,
  /^access_denied$/i,
  /full authentication is required/i,
  /anonymous.*not allowed/i,
];

export function isUnauthenticatedEnglishMessage(text: string | undefined): boolean {
  if (!text || !text.trim()) {
    return false;
  }
  const normalized = text.trim();
  return UNAUTHENTICATED_ENGLISH_PATTERNS.some((pattern) => pattern.test(normalized));
}

/** 403 响应在本地仍有 token 时，是否应视为会话失效（兼容旧后端英文 Forbidden / Access Denied） */
export function shouldTreat403AsSessionExpired(data: unknown, hasToken: boolean): boolean {
  if (!hasToken || !data || typeof data !== 'object') {
    return false;
  }
  const payload = data as { message?: string; error?: string };
  return (
    isUnauthenticatedEnglishMessage(payload.message) || isUnauthenticatedEnglishMessage(payload.error)
  );
}
