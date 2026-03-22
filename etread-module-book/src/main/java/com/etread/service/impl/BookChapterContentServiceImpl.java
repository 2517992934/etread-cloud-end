package com.etread.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.entity.BookChapterContent;
import com.etread.mapper.BookChapterContentMapper;
import com.etread.service.BookChapterContentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookChapterContentServiceImpl extends ServiceImpl<BookChapterContentMapper, BookChapterContent> implements BookChapterContentService {

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
        return this.lambdaQuery()
                .in(BookChapterContent::getChapterId, chapterIds)
                .list();
    }
}
