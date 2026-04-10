package com.etread.service;

import com.etread.dto.ChapterCommentQueryDTO;
import com.etread.vo.ChapterCommentResultVO;

public interface CommentQueryService {

    ChapterCommentResultVO queryChapterComments(ChapterCommentQueryDTO dto);
}
