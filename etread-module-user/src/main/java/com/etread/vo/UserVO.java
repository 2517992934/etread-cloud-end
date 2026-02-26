package com.etread.vo;

import lombok.Data;

@Data
public class UserVO {
    private String id;        // 注意：前端处理 Long 类型可能会精度丢失，建议转 String
    private String account;
    private String nickname;
    private String avatar;
    private String token;  // 如果登录接口直接返回用户信息，可以把 token 放这
}