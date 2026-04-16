package com.etread.controller;

import com.alibaba.fastjson2.JSON;
import com.etread.Result;
import com.etread.component.Tokencheck;
import com.etread.dto.BookShelfDTO;
import com.etread.dto.UserDTO;
import com.etread.entity.BookInfo;
import com.etread.service.impl.BookInfoServiceImpl;
import com.etread.service.impl.UserBookshelfServiceImpl;
import com.etread.utils.RedisUtil;
import com.etread.vo.AddToShelfVo;
import com.etread.vo.RemoveFromShelfVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/*
*关于添加与移除书架书的controller
*一键查询书架也在此处
*/
@RestController
@RequestMapping("/book")
public class BookShelfController {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private Tokencheck tokencheck;
    @Autowired
    private UserBookshelfServiceImpl userBookshelfService;
    @Autowired
    private BookInfoServiceImpl bookInfoService;
    @PostMapping("/shelf/add")
    public Result<AddToShelfVo> addToShelf(@RequestHeader("token")String token, BookShelfDTO bookShelfDTO) {
        if(bookShelfDTO.getBookId()==null){
            return Result.error("不存在书籍的加入书架请求");
        }
       Long userTd= tokencheck.getUserId(token);
       if(userBookshelfService.addToShelf(userTd,bookShelfDTO.getBookId())){
           AddToShelfVo addToShelfVo = new AddToShelfVo();
           addToShelfVo.setBookInfo(bookInfoService.getBookById(bookShelfDTO.getBookId()));
           addToShelfVo.setAddTime(new Date());
           return Result.success("加入成功",addToShelfVo);
       }else{
           return Result.error("加入失败，请稍后再试");
       }
    }
    @PostMapping("/shelf/remove")
    public Result<RemoveFromShelfVo> RemoveFromShelf(@RequestHeader("token")String token, BookShelfDTO bookShelfDTO) {
        if(bookShelfDTO.getBookId()==null){
            return Result.error("不存在书籍的移出书架请求");
        }
        Long userTd= tokencheck.getUserId(token);
        if(userBookshelfService.removeFromShelf(userTd,bookShelfDTO.getBookId())){
            RemoveFromShelfVo removeFromShelfVo = new RemoveFromShelfVo();
            removeFromShelfVo.setResult("移除成功");
            return Result.success("移除成功",removeFromShelfVo);
        }else{
            return Result.error("操作失败，请稍后再试");
        }
    }
    @GetMapping("/bookshelf/{userId}")
    public Result<List<BookInfo>> listSubscribedBooks(@PathVariable Long userId) {
        List<BookInfo> books = userBookshelfService.listSubscribedBooksByUserId(userId);
        return Result.success("查询成功", books);
    }

}
