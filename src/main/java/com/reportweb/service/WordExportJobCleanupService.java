package com.reportweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordExportJobCleanupService {

    private final WordExportJobService wordExportJobService;

    @Value("${app.word-export-job.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "${app.word-export-job.cleanup-cron:0 30 3 * * *}")
    public void cleanupExpiredJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(Math.max(retentionDays, 1));
        int deleted = wordExportJobService.cleanupFinishedJobsBefore(threshold);
        if (deleted > 0) {
            log.info("Cleaned {} expired word export jobs before {}", deleted, threshold);
        }
    }
}
