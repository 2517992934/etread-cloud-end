package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.BookChapterContent;
import com.etread.mapper.BookChapterContentMapper;
import com.etread.service.BookChapterContentService;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class BookChapterContentServiceImpl extends ServiceImpl<BookChapterContentMapper, BookChapterContent> implements BookChapterContentService {
    private static final String CHAPTER_CONTENT_KEY = "book:chapter:content:%s";
    private static final long CHAPTER_CONTENT_TTL = 10L;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public boolean removeByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return true;
        }
        LambdaQueryWrapper<BookChapterContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BookChapterContent::getChapterId, chapterIds);
        return this.remove(wrapper);
    }

    @Override
    public List<BookChapterContent> listByBookIdPrefix(Long bookId) {
        String bookIdStr = String.valueOf(bookId);
        String prefix;
        if (bookIdStr.length() >= 6) {
            prefix = bookIdStr.substring(0, 6);
        } else {
            prefix = String.format("%-6s", bookIdStr).replace(' ', '0');
        }

        long prefixNum = Long.parseLong(prefix);
        long start = prefixNum * 10000L;
        long end = start + 9999L;

        return this.lambdaQuery()
                .between(BookChapterContent::getChapterId, start, end)
                //dont need to order because sort depends
                .list();
    }

    @Override
    public List<BookChapterContent> listByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }

        Map<Long, BookChapterContent> resultMap = new LinkedHashMap<>();
        List<Long> missIds = new ArrayList<>();

        // 1. 先查 Redis
        for (Long chapterId : chapterIds) {
            if (chapterId == null) {
                continue;
            }
            String key = String.format(CHAPTER_CONTENT_KEY, chapterId);
            String cachedContent = redisUtil.get(key);
            if (cachedContent != null) {
                BookChapterContent content = new BookChapterContent();
                content.setChapterId(chapterId);
                content.setContent(cachedContent);
                
                resultMap.put(chapterId, content);
            } else {
                missIds.add(chapterId);
            }
        }

        // 2. Redis 没命中的，再查数据库
        if (!missIds.isEmpty()) {
            List<BookChapterContent> dbContents = this.lambdaQuery()
                    .in(BookChapterContent::getChapterId, missIds)
                    .list();

            if (dbContents != null && !dbContents.isEmpty()) {
                for (BookChapterContent content : dbContents) {
                    if (content.getChapterId() == null) {
                        continue;
                    }

                    resultMap.put(content.getChapterId(), content);

                    // 3. 回填 Redis
                    if (content.getContent() != null) {
                        String key = String.format(CHAPTER_CONTENT_KEY, content.getChapterId());
                        redisUtil.set(key, content.getContent(), CHAPTER_CONTENT_TTL, TimeUnit.MINUTES);
                    }
                }
            }
        }

        // 4. 按传入顺序返回
        List<BookChapterContent> result = new ArrayList<>();
        for (Long chapterId : chapterIds) {
            BookChapterContent content = resultMap.get(chapterId);
            if (content != null) {
                result.add(content);
            }
        }

        return result;
    }
}
