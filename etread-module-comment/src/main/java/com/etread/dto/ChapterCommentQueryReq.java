package com.etread.dto;

import jakarta.validation.constraints.NotNull;

public class ChapterCommentQueryReq {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    private Integer hotLimit = 3;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getHotLimit() {
        return hotLimit;
    }

    public void setHotLimit(Integer hotLimit) {
        this.hotLimit = hotLimit;
    }
}
