package com.etread.controller;

import com.etread.Result;
import com.etread.component.BookUserResolver;
import com.etread.dto.AuthorBookCreateDTO;
import com.etread.dto.AuthorChapterCreateDTO;
import com.etread.dto.AuthorChapterDeleteDTO;
import com.etread.dto.AuthorChapterInsertDTO;
import com.etread.dto.ParagraphOperationDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import com.etread.service.IAuthorService;
import com.etread.vo.ParagraphOperationResultVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/author")
public class AuthorController {

    @Autowired
    private IAuthorService authorService;

    @Autowired
    private BookUserResolver bookUserResolver;

    @PostMapping("/book/create")
    public Result<BookInfo> createBook(@RequestHeader("token") String token,
                                       @RequestBody @Valid AuthorBookCreateDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookInfo bookInfo = authorService.createBookDraft(dto, authorUserId);
        return Result.success("书籍草稿创建成功", bookInfo);
    }

    @PostMapping("/chapter/create")
    public Result<BookChapter> createChapter(@RequestHeader("token") String token,
                                             @RequestBody @Valid AuthorChapterCreateDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookChapter chapter = authorService.createChapter(dto, authorUserId);
        return Result.success("章节创建成功", chapter);
    }

    @PostMapping("/chapter/insert")
    public Result<BookChapter> insertChapter(@RequestHeader("token") String token,
                                             @RequestBody @Valid AuthorChapterInsertDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        BookChapter chapter = authorService.insertChapter(dto, authorUserId);
        return Result.success("插播章节成功", chapter);
    }

    @PostMapping("/chapter/delete")
    public Result<String> deleteChapter(@RequestHeader("token") String token,
                                        @RequestBody @Valid AuthorChapterDeleteDTO dto) {
        Long authorUserId = bookUserResolver.requireUserId(token);
        authorService.deleteChapter(dto, authorUserId);
        return Result.success("章节删除成功", "ok");
    }

    @PostMapping("/image/upload")
    public Result<String> uploadImage(@RequestHeader("token") String token,
                                      @RequestParam("file") MultipartFile file) throws Exception {
        bookUserResolver.requireUserId(token);
        String imageUrl = authorService.uploadEditorImage(file);
        return Result.success("图片上传成功", imageUrl);
    }

    @PostMapping("/chapter/paragraph/operate")
    public Result<ParagraphOperationResultVO> operateParagraph(@RequestHeader("token") String token,
                                                               @RequestBody @Valid ParagraphOperationDTO dto) throws Exception {
        Long authorUserId = bookUserResolver.requireUserId(token);
        ParagraphOperationResultVO result = authorService.operateParagraph(dto, authorUserId);
        return Result.success("段落操作成功", result);
    }
}
