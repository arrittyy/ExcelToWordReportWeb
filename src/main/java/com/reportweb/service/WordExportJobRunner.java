package com.reportweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WordExportJobRunner {

    private final WordExportJobService wordExportJobService;

    @Async("wordExportTaskExecutor")
    public void run(String jobId) {
        try {
            wordExportJobService.executeJob(jobId);
        } catch (Exception ex) {
            log.error("Unexpected async runner error for word export job {}", jobId, ex);
        }
    }
}
