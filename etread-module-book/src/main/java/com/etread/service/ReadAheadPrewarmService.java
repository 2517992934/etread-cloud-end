package com.etread.service;

import java.util.List;

public interface ReadAheadPrewarmService {

    void prewarmNextChaptersAsync(List<Long> chapterIds);
}
