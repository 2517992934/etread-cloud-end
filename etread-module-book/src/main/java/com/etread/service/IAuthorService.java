package com.etread.service;

import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorBookPublishDTO;
import com.etread.dto.AuthorBookQueryDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.AuthorChapterQueryDTO;
import com.etread.dto.ParagraphOperationDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import com.etread.vo.AuthorChapterDetailVO;
import com.etread.vo.ParagraphOperationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IAuthorService {

    BookInfo createBookDraft(AuthorBookCreateDTO dto, Long authorUserId, String authorName);

    List<BookInfo> listMyDraftBooks(Long authorUserId);

    BookInfo publishBook(AuthorBookPublishDTO dto, Long authorUserId);

    BookChapter createChapter(AuthorChapterCreateDTO dto, Long authorUserId);

    List<BookChapter> listMyBookChapters(AuthorBookQueryDTO dto, Long authorUserId);

    AuthorChapterDetailVO getMyChapterDetail(AuthorChapterQueryDTO dto, Long authorUserId);

    BookChapter insertChapter(AuthorChapterInsertDTO dto, Long authorUserId);

    void deleteChapter(AuthorChapterDeleteDTO dto, Long authorUserId);

    String uploadEditorImage(Long bookId, MultipartFile file, Long authorUserId) throws Exception;

    ParagraphOperationResultVO operateParagraph(ParagraphOperationDTO dto, Long authorUserId) throws Exception;
}
