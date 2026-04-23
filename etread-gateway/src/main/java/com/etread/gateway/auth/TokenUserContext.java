package com.etread.gateway.auth;

import lombok.Data;

@Data
public class TokenUserContext {

    private Long userId;
    private String account;
    private String nickname;
    private String avatar;

}
