package com.etread.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookChapterSyncDTO {
    private Long bookId;
    private Long chapterId;
    private List<Long> chapterIds;
}
