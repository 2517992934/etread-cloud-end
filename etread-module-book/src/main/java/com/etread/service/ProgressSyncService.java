package com.etread.service;

import com.etread.dto.ProgressDTO;
import com.etread.vo.ProgressVO;

public interface ProgressSyncService {
    ProgressVO bufferProgress(Long userId, Long bookId, Long currentChapterId, Float readPercentage);
    ProgressVO getProgress(Long userId, Long bookId);
    void flushProgressToDB();
}
