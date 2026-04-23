package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorChapterCreateDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotBlank(message = "章节标题不能为空")
    private String chapterTitle;
}
