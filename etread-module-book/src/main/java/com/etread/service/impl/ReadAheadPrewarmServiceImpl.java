package com.etread.service.impl;

import com.etread.entity.BookChapter;
import com.etread.entity.BookChapterContent;
import com.etread.service.BookChapterContentService;
import com.etread.service.BookChapterService;
import com.etread.service.ReadAheadPrewarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class ReadAheadPrewarmServiceImpl implements ReadAheadPrewarmService {

    private static final String CHAPTER_CONTENT_KEY = "book:chapter:content:%s";
    private static final Duration READ_AHEAD_TTL = Duration.ofMinutes(10);

    @Autowired
    private BookChapterService bookChapterService;

    @Autowired
    private BookChapterContentService bookChapterContentService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("bookParseExecutor")
    private Executor executor;

    @Override
    public void prewarmNextChaptersAsync(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return;
        }
        for (Long chapterId : chapterIds) {
            CompletableFuture.runAsync(() -> prewarmNextChapters(chapterId), executor);
        }
    }

    private void prewarmNextChapters(Long chapterId) {
        if (chapterId == null) {
            return;
        }
        BookChapter current = bookChapterService.getById(chapterId);
        if (current == null || current.getSortOrder() == null) {
            return;
        }

        List<BookChapter> nextChapters = bookChapterService.lambdaQuery()
                .eq(BookChapter::getBookId, current.getBookId())
                .gt(BookChapter::getSortOrder, current.getSortOrder())
                .orderByAsc(BookChapter::getSortOrder)
                .last("LIMIT 2")
                .list();

        if (nextChapters == null || nextChapters.isEmpty()) {
            return;
        }

        List<Long> nextChapterIds = nextChapters.stream()
                .map(BookChapter::getId)
                .collect(Collectors.toList());
        List<BookChapterContent> contents = bookChapterContentService.listByChapterIds(nextChapterIds);
        for (BookChapterContent content : contents) {
            if (content.getChapterId() == null || content.getContent() == null) {
                continue;
            }
            String key = String.format(CHAPTER_CONTENT_KEY, content.getChapterId());
            Boolean exists = stringRedisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                continue;
            }
            stringRedisTemplate.opsForValue().set(key, content.getContent(), READ_AHEAD_TTL);
        }
    }
}
