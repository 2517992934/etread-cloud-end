package com.etread.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AuthorBookCreateDTO {

    @NotBlank(message = "书名不能为空")
    private String title;

    /**
     * 封面文件，创建草稿时直接上传到 MinIO。
     */
    private MultipartFile cover;

    private String description;

    private List<String> tags;
}
