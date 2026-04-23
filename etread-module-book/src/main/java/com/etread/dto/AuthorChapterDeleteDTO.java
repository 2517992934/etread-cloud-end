package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorChapterDeleteDTO {

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;
}
