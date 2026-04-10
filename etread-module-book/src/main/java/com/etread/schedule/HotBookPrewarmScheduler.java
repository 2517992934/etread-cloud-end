//package com.etread.schedule;
//
//import com.alibaba.fastjson2.JSON;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.etread.entity.BookChapter;
//import com.etread.entity.BookChapterContent;
//import com.etread.entity.BookParagraphCommentLite;
//import com.etread.mapper.BookParagraphCommentLiteMapper;
//import com.etread.service.BookChapterContentService;
//import com.etread.service.BookChapterService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Component
//public class HotBookPrewarmScheduler {
//
//    private static final String BOOK_HOT_ZSET_KEY = "book:hot";
//    private static final String CHAPTER_CONTENT_KEY = "book:chapter:content:%s";
//    private static final String COMMENT_HOT_ZSET_KEY = "comment:hot:%s";
//    private static final String COMMENT_HOT_PREWARM_KEY = "comment:hot:prewarm:%s";
//
//    private static final int HOT_BOOK_LIMIT = 50;
//    private static final int PREWARM_CHAPTER_LIMIT = 5;
//    private static final int HOT_COMMENT_LIMIT = 3;
//    private static final Duration PREWARM_TTL = Duration.ofHours(24);
//
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Autowired
//    private BookChapterService bookChapterService;
//
//    @Autowired
//    private BookChapterContentService bookChapterContentService;
//
//    @Autowired
//    private BookParagraphCommentLiteMapper commentLiteMapper;
//
//    @Scheduled(cron = "0 0 0 * * ?")
//    public void prewarmHotBooks() {
//        Set<String> hotBookIds = stringRedisTemplate.opsForZSet()
//                .reverseRange(BOOK_HOT_ZSET_KEY, 0, HOT_BOOK_LIMIT - 1L);
//        if (hotBookIds == null || hotBookIds.isEmpty()) {
//            return;
//        }
//
//        for (String bookIdStr : hotBookIds) {
//            if (bookIdStr == null || bookIdStr.isBlank()) {
//                continue;
//            }
//            Long bookId = Long.valueOf(bookIdStr);
//            prewarmBook(bookId);
//        }
//    }
//
//    private void prewarmBook(Long bookId) {
//        List<BookChapter> chapters = bookChapterService.listByBookIdPrefix(bookId);
//        if (chapters == null || chapters.isEmpty()) {
//            return;
//        }
//
//        List<BookChapter> topChapters = chapters.stream()
//                .limit(PREWARM_CHAPTER_LIMIT)
//                .collect(Collectors.toList());
//        List<Long> chapterIds = topChapters.stream()
//                .map(BookChapter::getId)
//                .collect(Collectors.toList());
//
//        List<BookChapterContent> contents = bookChapterContentService.listByChapterIds(chapterIds);
//        Map<Long, String> contentMap = contents.stream()
//                .collect(Collectors.toMap(BookChapterContent::getChapterId, BookChapterContent::getContent));
//
//        for (Long chapterId : chapterIds) {
//            String content = contentMap.get(chapterId);
//            if (content == null) {
//                continue;
//            }
//            String key = String.format(CHAPTER_CONTENT_KEY, chapterId);
//            stringRedisTemplate.opsForValue().set(key, content, PREWARM_TTL);
//            prewarmHotComments(chapterId);
//        }
//    }
//
//    private void prewarmHotComments(Long chapterId) {
//        String zsetKey = String.format(COMMENT_HOT_ZSET_KEY, chapterId);
//        Set<String> hotCommentIds = stringRedisTemplate.opsForZSet()
//                .reverseRange(zsetKey, 0, HOT_COMMENT_LIMIT - 1L);
//        if (hotCommentIds == null || hotCommentIds.isEmpty()) {
//            return;
//        }
//
//        String cacheKey = String.format(COMMENT_HOT_PREWARM_KEY, chapterId);
//        List<Long> commentIds = hotCommentIds.stream()
//                .map(Long::valueOf)
//                .collect(Collectors.toList());
//
//        List<BookParagraphCommentLite> comments = commentLiteMapper.selectList(
//                new LambdaQueryWrapper<BookParagraphCommentLite>()
//                        .in(BookParagraphCommentLite::getId, commentIds)
//                        .orderByDesc(BookParagraphCommentLite::getLikeCount)
//        );
//        if (comments == null || comments.isEmpty()) {
//            return;
//        }
//        String value = JSON.toJSONString(comments);
//        stringRedisTemplate.opsForValue().set(cacheKey, value, PREWARM_TTL);
//    }
//}
