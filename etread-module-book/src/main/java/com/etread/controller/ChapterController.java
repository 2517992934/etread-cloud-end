package com.etread.controller;

import com.etread.Result;
import com.etread.dto.BookChapterSyncDTO;
import com.etread.entity.BookChapter;
import com.etread.entity.BookChapterContent;
import com.etread.service.BookChapterContentService;
import com.etread.service.BookChapterService;
import com.etread.service.ReadAheadPrewarmService;
import com.etread.vo.ChapterSyncVO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/book")
public class ChapterController {

    @Autowired
    private BookChapterService bookChapterService;

    @Autowired
    private BookChapterContentService bookChapterContentService;

    @Autowired
    private ReadAheadPrewarmService readAheadPrewarmService;

    @PostMapping("/chapter/catalog")
    public Result<ChapterSyncVO> catalog(@RequestHeader("token") String token, BookChapterSyncDTO bookChapterSyncDTO) {
        if (bookChapterSyncDTO.getBookId() == null || bookChapterSyncDTO.getBookId() == null) {
            return Result.error("bookId 不能为空");
        }
        List<BookChapter> chapters = bookChapterService.listByBookIdPrefix(bookChapterSyncDTO.getBookId());
        ChapterSyncVO chapterSyncVO = new ChapterSyncVO();
        chapterSyncVO.setBookChapters(chapters);
        return Result.success("请求成功", chapterSyncVO);
    }

    @PostMapping("/chapter/contents")
    public Result<ChapterSyncVO> contents(@RequestHeader("token") String token, BookChapterSyncDTO bookChapterSyncDTO) {
        if (bookChapterSyncDTO == null || bookChapterSyncDTO.getChapterIds() == null || bookChapterSyncDTO.getChapterIds().isEmpty()) {
            return Result.error("chapterIds 不能为空"+bookChapterSyncDTO.getChapterIds());
        }
        List<BookChapterContent> contents = bookChapterContentService.listByChapterIds(bookChapterSyncDTO.getChapterIds());
        //根据当前请求的章节列表，后台异步预加载后续章节内容，提升下一次阅读速度
        readAheadPrewarmService.prewarmNextChaptersAsync(bookChapterSyncDTO.getChapterIds());
        ChapterSyncVO chapterSyncVO = new ChapterSyncVO();
        chapterSyncVO.setContents(contents);
        return Result.success("请求成功", chapterSyncVO);
    }
}
