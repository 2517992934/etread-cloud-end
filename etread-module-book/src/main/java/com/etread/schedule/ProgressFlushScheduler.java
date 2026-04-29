package com.etread.schedule;

import com.etread.service.ProgressSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProgressFlushScheduler {

    private static final long FLUSH_INTERVAL_MS = 300000L;

    @Autowired
    private ProgressSyncService progressSyncService;

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    public void flushReadProgress() {
        try {
            log.info("starting progress flush cycle...");
            progressSyncService.flushProgressToDB();
            log.info("progress flush cycle completed successfully");
        } catch (Exception e) {
            log.error("error during progress flush cycle", e);
        }
    }
}
