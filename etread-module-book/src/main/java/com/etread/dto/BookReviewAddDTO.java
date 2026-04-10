package com.etread.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookReviewAddDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 10, message = "评分最高 10 分")
    private Integer rating;

    private String content;


}
