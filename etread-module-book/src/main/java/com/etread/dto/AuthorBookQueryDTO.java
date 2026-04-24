package com.etread.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorBookQueryDTO {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;
}
