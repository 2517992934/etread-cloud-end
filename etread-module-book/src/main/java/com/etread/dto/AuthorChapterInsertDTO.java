package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorChapterInsertDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "anchorChapterId 不能为空")
    private Long anchorChapterId;

    @NotBlank(message = "position 不能为空")
    private String position;

    @NotBlank(message = "章节标题不能为空")
    private String chapterTitle;
}
