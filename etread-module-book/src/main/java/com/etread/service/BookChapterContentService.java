package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.entity.BookChapterContent;

import java.util.List;

public interface BookChapterContentService extends IService<BookChapterContent> {
    /**
     * 根据章节ID列表批量删除内容
     */
    boolean removeByChapterIds(List<Long> chapterIds);
}
