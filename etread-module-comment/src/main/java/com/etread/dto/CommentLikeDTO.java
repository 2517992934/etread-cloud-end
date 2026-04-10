package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentLikeDTO {

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotNull(message = "commentId 不能为空")
    private Long commentId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
