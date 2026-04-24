package com.etread.vo;

import lombok.Data;

@Data
public class AuthorChapterDetailVO {

    private Long chapterId;

    private Long bookId;

    private String chapterTitle;

    private Integer sortOrder;

    private Integer wordCount;

    private String content;
}
