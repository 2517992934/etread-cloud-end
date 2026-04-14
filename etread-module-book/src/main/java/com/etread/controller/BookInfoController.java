package com.etread.controller;

import com.etread.Result;
import com.etread.dto.BookSearchDTO;
import com.etread.mapper.BookInfoMapper;
import com.etread.service.impl.BookInfoServiceImpl;
import com.etread.vo.BookInfoBaseVO;
import com.etread.vo.BookSearchVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@RestController
@RequestMapping("/book")
public class BookInfoController {

    @Autowired
    private BookInfoMapper bookInfoMapper;
    @Autowired
    private BookInfoServiceImpl bookInfoService;
    @Autowired
    private BookInfoServiceImpl bookInfoServiceImpl;

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
            vo.setTotalScore(null);
        }
        return Result.success("查询成功", vo);
    }

    @PostMapping("/info/search")
    public Result<BookSearchVo> search(@RequestHeader("token") String token, BookSearchDTO searchDTO,
                                             @RequestParam(defaultValue = "1")int page , @RequestParam(defaultValue = "10")int size) {
        BookSearchVo vo =new BookSearchVo();
        vo.setResultBooks(bookInfoServiceImpl.searchBook(searchDTO,page,size).getRecords());
        if (vo.getResultBooks() != null) {
            return Result.success("搜索成功",vo);
        }else{
            return Result.error("服务器出错了");
        }
    }

}
