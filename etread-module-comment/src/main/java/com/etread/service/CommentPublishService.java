package com.etread.service;

import com.etread.dto.CommentPublishDTO;
import com.etread.vo.ParagraphCommentVO;

public interface CommentPublishService {

    ParagraphCommentVO publish(CommentPublishDTO dto);
}
