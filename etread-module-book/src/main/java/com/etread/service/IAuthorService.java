package com.etread.service;

import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.ParagraphOperationDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import com.etread.vo.ParagraphOperationResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface IAuthorService {

    BookInfo createBookDraft(AuthorBookCreateDTO dto, Long authorUserId);

    BookChapter createChapter(AuthorChapterCreateDTO dto, Long authorUserId);

    BookChapter insertChapter(AuthorChapterInsertDTO dto, Long authorUserId);

    void deleteChapter(AuthorChapterDeleteDTO dto, Long authorUserId);

    String uploadEditorImage(MultipartFile file) throws Exception;

    ParagraphOperationResultVO operateParagraph(ParagraphOperationDTO dto, Long authorUserId) throws Exception;
}
