package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReq {
    @NotNull(message = "bookId 不能为空")
    private Long bookId;

    @NotNull(message = "currentChapterId 不能为空")
    private Long currentChapterId;

    @NotNull(message = "readPercentage 不能为空")
    private Float readPercentage;
}
