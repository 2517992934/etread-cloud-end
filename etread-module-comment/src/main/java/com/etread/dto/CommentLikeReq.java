package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentLikeReq {

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotNull(message = "commentId 不能为空")
    private Long commentId;

}
