package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.entity.BookChapter;

import java.util.List;

public interface BookChapterService extends IService<BookChapter> {
    /**
     * 根据书籍ID级联删除章节及内容
     */
    boolean removeByBookId(Long bookId);
    /**
     * 根据书的id统计字数
     */
    List<BookChapter> listByBookIdPrefix(Long bookId);
}
