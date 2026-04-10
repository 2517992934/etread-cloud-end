package com.etread.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("book_review")
public class BookReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;

    private Long userId;

    private Integer rating;

    private String content;

    private Integer likeCount;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
