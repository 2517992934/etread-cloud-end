package com.etread.component;

import com.alibaba.fastjson2.JSON;
import com.etread.dto.UserDTO;
import com.etread.mapper.BookInfoMapper;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/*
上传书时的检验，验证上传者等于用户，防止冒名上传
 */
@Component
public class Tokencheck {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    BookInfoMapper bookInfoMapper;
    public boolean checkToken(String token, Long bookid) {
      String fullKey = "login:token:" + token;
      String userJson = redisUtil.get(fullKey);
      Long publisher =bookInfoMapper.selectById(bookid).getPublisher();

      if (userJson != null) {
          UserDTO userDTO = JSON.parseObject(userJson, UserDTO.class);
          if (userDTO.getUser_id().equals(publisher)) {
              return true;
          }else {
              return false;
          }
      } else {
          throw new RuntimeException("token 不存在或已过期");
      }

    }
    public Long getUserId(String token) {
        String fullKey = "login:token:" + token;
        String userJson = redisUtil.get(fullKey);
        Long userId = JSON.parseObject(userJson, UserDTO.class).getUser_id();
        return userId;
    }
}
