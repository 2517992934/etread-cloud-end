package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AuthorBookCreateDTO {

    @NotBlank(message = "书名不能为空")
    private String title;

    @NotBlank(message = "作者名不能为空")
    private String author;

    private String coverUrl;

    private String description;

    private List<String> tags;
}
