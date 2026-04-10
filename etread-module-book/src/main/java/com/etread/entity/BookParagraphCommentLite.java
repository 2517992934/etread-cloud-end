package com.etread.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("book_paragraph_comment")
public class BookParagraphCommentLite {

    private Long id;
    private Long bookId;
    private Long chapterId;
    private String paragraphId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Long parentId;
    private Long replyToUserId;
    private Date createTime;
}
