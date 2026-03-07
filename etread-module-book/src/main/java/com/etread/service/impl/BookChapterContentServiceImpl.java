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
}
