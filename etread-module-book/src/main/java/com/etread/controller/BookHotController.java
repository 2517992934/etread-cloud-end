package com.etread.controller;

import com.etread.Result;
import com.etread.service.BookHotService;
import com.etread.vo.BookHotListVO;
import com.etread.vo.BookRecommendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/book")
public class BookHotController {

    @Autowired
    private BookHotService bookHotService;

    @GetMapping("/hotList")
    public Result<List<BookHotListVO>> getHotList(@RequestParam(required = false) Integer limit) {
        try {
            List<BookHotListVO> hotBooks = bookHotService.getHotList(limit);
            return Result.success("查询成功", hotBooks);
        } catch (Exception e) {
            log.error("error getting hot list", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/recommend")
    public Result<List<BookRecommendVO>> getRecommend(@RequestParam(required = false) Integer limit) {
        try {
            List<BookRecommendVO> recommendBooks = bookHotService.getRandomRecommend(limit);
            return Result.success("查询成功", recommendBooks);
        } catch (Exception e) {
            log.error("error getting recommend list", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
