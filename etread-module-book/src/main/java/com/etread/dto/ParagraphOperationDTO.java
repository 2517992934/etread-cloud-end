package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParagraphOperationDTO {

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotBlank(message = "action 不能为空")
    private String action;

    @NotBlank(message = "targetParagraphId 不能为空")
    private String targetParagraphId;

    /**
     * UPDATE / INSERT_AFTER 时使用。
     * 允许纯文本，也允许 img 标签等富文本片段。
     */
    private String newText;
}
