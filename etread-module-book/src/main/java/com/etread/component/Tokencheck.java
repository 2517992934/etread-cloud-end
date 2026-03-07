package com.etread.component;

import com.alibaba.fastjson2.JSON;
import com.etread.dto.UserDTO;
import com.etread.mapper.BookInfoMapper;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Tokencheck {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    BookInfoMapper bookInfoMapper;
    public boolean checkToken(String token, Long bookid) {
      String fullKey = "login:token:" + token;
      String userJson = redisUtil.get(fullKey);
      String publisher =bookInfoMapper.selectById(bookid).getPublisher();

      if (userJson != null) {
          UserDTO userDTO = JSON.parseObject(userJson, UserDTO.class);
          if (userDTO.getAccount().equals(publisher)) {
              return true;
          }else {
              return false;
          }
      } else {
          throw new RuntimeException("token 不存在或已过期");
      }

    }
}
