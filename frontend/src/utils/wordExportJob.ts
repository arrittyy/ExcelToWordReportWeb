import { projectService, type WordExportJob } from '@/services/projectService';

const POLL_INTERVAL_MS = 3000;
const MAX_POLL_COUNT = 1200; // up to 60 minutes

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function waitForWordExportJob(projectId: number, jobId: string): Promise<WordExportJob> {
  let pollCount = 0;
  while (pollCount < MAX_POLL_COUNT) {
    const job = await projectService.getWordExportJob(projectId, jobId);
    if (job.status === 'SUCCEEDED' || job.status === 'FAILED') {
      return job;
    }
    pollCount += 1;
    await sleep(POLL_INTERVAL_MS);
  }
  throw new Error('导出任务等待超时，请稍后在任务状态中重试下载');
}
