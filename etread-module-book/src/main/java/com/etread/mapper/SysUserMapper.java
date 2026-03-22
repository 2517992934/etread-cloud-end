package com.etread.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper {

    @Select("SELECT user_id FROM sys_user WHERE account = #{account} LIMIT 1")
    Long selectUserIdByAccount(@Param("account") String account);
}

