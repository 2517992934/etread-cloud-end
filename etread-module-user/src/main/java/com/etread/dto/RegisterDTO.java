package com.etread.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDTO {

    private String user_id;

    @NotBlank(message = "密码不能为空哦！")
    @Size(min = 6, message = "密码最少要6位哦！")
    private String password;
    @NotBlank(message = "昵称为空？")
    @Size(min = 3,max = 20,message = "昵称大小要在3-20喔")
    private String nickname;

    // 用来接收前端传来的头像文件
    private MultipartFile avatarFile;
    @NotBlank(message = "账号为空？")
    @Size(min = 8,max = 20,message = "账号大小要在8-20喔")
    private String account;
}