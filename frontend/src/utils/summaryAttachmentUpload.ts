/** 与后端 ProjectsController SUMMARY_ATTACHMENT_MAX_BYTES 一致 */
export const SUMMARY_ATTACHMENT_MAX_BYTES = 50 * 1024 * 1024;

function fileExtensionLower(name: string): string {
  const i = name.lastIndexOf('.');
  if (i < 0) {
    return '';
  }
  return name.slice(i).toLowerCase();
}

/** @returns 错误文案，通过校验则返回 null */
export function validateSummaryNotificationFile(file: File): string | null {
  const ext = fileExtensionLower(file.name);
  if (ext !== '.docx' && ext !== '.pdf') {
    return '仅支持上传 docx、pdf 文件';
  }
  if (file.size > SUMMARY_ATTACHMENT_MAX_BYTES) {
    return '附件大小不能超过 50MB';
  }
  return null;
}

/** @returns 错误文案，通过校验则返回 null */
export function validateThirdPartyFullFile(file: File): string | null {
  const ext = fileExtensionLower(file.name);
  if (ext !== '.pdf') {
    return '第三方报告完整版仅支持上传 PDF 文件';
  }
  if (file.size > SUMMARY_ATTACHMENT_MAX_BYTES) {
    return '附件大小不能超过 50MB';
  }
  return null;
}
