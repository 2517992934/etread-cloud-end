package com.etread.controller;

import com.etread.Result;
import com.etread.component.BookUserResolver;
import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorBookPublishDTO;
import com.etread.dto.AuthorBookQueryDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.AuthorChapterQueryDTO;
import com.etread.dto.ParagraphOperationDTO;
import com.etread.vo.AuthorChapterDetailVO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import com.etread.service.IAuthorService;
import com.etread.vo.ParagraphOperationResultVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/book")
public class AuthorController {

    @Autowired
    private IAuthorService authorService;

    @Autowired
    private BookUserResolver bookUserResolver;

    @PostMapping("/author/create")
    public Result<BookInfo> createBook(@RequestHeader("token") String token,
                                        AuthorBookCreateDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        String authorName = bookUserResolver.requireNickname(token);
        BookInfo bookInfo = authorService.createBookDraft(dto, authorUserId, authorName);
        return Result.success("书籍草稿创建成功", bookInfo);
    }

    @PostMapping("/author/drafts")
    public Result<List<BookInfo>> listMyDraftBooks(@RequestHeader("token") String token) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        List<BookInfo> drafts = authorService.listMyDraftBooks(authorUserId);
        return Result.success("获取草稿书成功", drafts);
    }

    @PostMapping("/author/publish")
    public Result<BookInfo> publishBook(@RequestHeader("token") String token,
                                         AuthorBookPublishDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookInfo bookInfo = authorService.publishBook(dto, authorUserId);
        return Result.success("书籍上架成功", bookInfo);
    }

    @PostMapping("/author/chapter/create")
    public Result<BookChapter> createChapter(@RequestHeader("token") String token,
                                             AuthorChapterCreateDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookChapter chapter = authorService.createChapter(dto, authorUserId);
        return Result.success("章节创建成功", chapter);
    }

    @PostMapping("/author/chapters")
    public Result<List<BookChapter>> listMyBookChapters(@RequestHeader("token") String token,
                                                         AuthorBookQueryDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        List<BookChapter> chapters = authorService.listMyBookChapters(dto, authorUserId);
        return Result.success("获取章节目录成功", chapters);
    }

    @PostMapping("/author/chapter/detail")
    public Result<AuthorChapterDetailVO> getMyChapterDetail(@RequestHeader("token") String token,
                                                             AuthorChapterQueryDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        AuthorChapterDetailVO detail = authorService.getMyChapterDetail(dto, authorUserId);
        return Result.success("获取章节内容成功", detail);
    }

    @PostMapping("/author/chapter/insert")
    public Result<BookChapter> insertChapter(@RequestHeader("token") String token,
                                              AuthorChapterInsertDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookChapter chapter = authorService.insertChapter(dto, authorUserId);
        return Result.success("插播章节成功", chapter);
    }

    @PostMapping("/author/chapter/delete")
    public Result<String> deleteChapter(@RequestHeader("token") String token,
                                       AuthorChapterDeleteDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        authorService.deleteChapter(dto, authorUserId);
        return Result.success("章节删除成功", "ok");
    }

    @PostMapping("/author/image/upload")
    public Result<String> uploadImage(@RequestHeader("token") String token,
                                      @RequestParam("bookId") Long bookId,
                                      @RequestParam("file") MultipartFile file) throws Exception {
        Long authorUserId = bookUserResolver.requireUserId(token);
        String imageUrl = authorService.uploadEditorImage(bookId, file, authorUserId);
        return Result.success("图片上传成功", imageUrl);
    }

    @PostMapping("/author/paragraph/operate")
    public Result<ParagraphOperationResultVO> operateParagraph(@RequestHeader("token") String token,
                                                               ParagraphOperationDTO dto) throws Exception {
        Long authorUserId = bookUserResolver.requireUserId(token);
        ParagraphOperationResultVO result = authorService.operateParagraph(dto, authorUserId);
        return Result.success("段落操作成功", result);
    }
}
