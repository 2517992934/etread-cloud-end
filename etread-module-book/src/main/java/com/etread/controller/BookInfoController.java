package com.etread.controller;

import com.etread.Result;
import com.etread.mapper.BookInfoMapper;
import com.etread.vo.BookInfoBaseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book")
public class BookInfoController {

    @Autowired
    private BookInfoMapper bookInfoMapper;

    @GetMapping("/info/{bookId}")
    public Result<BookInfoBaseVO> info(@PathVariable Long bookId) {
        BookInfoBaseVO vo = bookInfoMapper.selectBaseById(bookId);
        if (vo == null) {
            return Result.error("书籍不存在");
        }
        Integer totalScore = vo.getTotalScore();
        Integer ratingCount = vo.getRatingCount();
        if (totalScore == null || ratingCount == null || ratingCount == 0) {
            vo.setAverageScore(0D);
        } else {
            //count avg
            vo.setAverageScore(totalScore / (double) ratingCount);
        }
        return Result.success("查询成功", vo);
    }
}
