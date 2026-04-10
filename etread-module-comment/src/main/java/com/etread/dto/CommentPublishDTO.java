package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentPublishDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotBlank(message = "paragraphId 不能为空")
    private String paragraphId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /**
     * 根评论时可为空或传 0。
     */
    private Long parentId;

    /**
     * 回复某位用户时使用，可为空。
     */
    private Long replyToUserId;

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

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getReplyToUserId() {
        return replyToUserId;
    }

    public void setReplyToUserId(Long replyToUserId) {
        this.replyToUserId = replyToUserId;
    }
}
