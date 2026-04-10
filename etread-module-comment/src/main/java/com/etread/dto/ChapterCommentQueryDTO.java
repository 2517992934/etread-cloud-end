package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterCommentQueryDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    /**
     * 热评榜返回条数，后续查询热评时使用。
     */
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getHotLimit() {
        return hotLimit;
    }

    public void setHotLimit(Integer hotLimit) {
        this.hotLimit = hotLimit;
    }
}
