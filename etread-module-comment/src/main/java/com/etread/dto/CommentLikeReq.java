package com.etread.dto;

import jakarta.validation.constraints.NotNull;

public class CommentLikeReq {

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotNull(message = "commentId 不能为空")
    private Long commentId;

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
}
