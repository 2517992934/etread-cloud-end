package com.etread.vo;

import com.etread.entity.BookChapter;
import com.etread.entity.BookChapterContent;
import lombok.Data;

import java.util.List;
@Data
public class ChapterSyncVO {
    private List<BookChapter> bookChapters;
    private List<BookChapterContent> contents;
}
