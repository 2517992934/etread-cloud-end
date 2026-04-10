package com.etread.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etread.Result;
import com.etread.component.BookUserResolver;
import com.etread.dto.BookReviewAddDTO;
import com.etread.dto.BookReviewAddReq;
import com.etread.entity.BookReview;
import com.etread.service.BookReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/book")
@Validated
public class BookReviewController {

    @Autowired
    private BookReviewService bookReviewService;

    @Autowired
    private BookUserResolver bookUserResolver;

    @PostMapping("/review/add")
    public Result<BookReview> add(@RequestHeader("token") String token,
                                  BookReviewAddReq req) {
        Long userId = bookUserResolver.requireUserId(token);
        BookReviewAddDTO dto = new BookReviewAddDTO();
        dto.setBookId(req.getBookId());
        dto.setUserId(userId);
        dto.setRating(req.getRating());
        dto.setContent(req.getContent());
        BookReview review = bookReviewService.addReview(dto);
        return Result.success("发布书评成功", review);
    }

    @GetMapping("/review/list/{bookId}")
    public Result<Page<BookReview>> list(@PathVariable Long bookId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Page<BookReview> result = bookReviewService.listByBookId(bookId, page, size);
        return Result.success("查询成功", result);
    }
}
