package com.etread.controller;

import com.etread.Result;
import com.etread.component.Tokencheck;
import com.etread.dto.BookInfoDTO;
import com.etread.dto.BookUploadDTO;
import com.etread.service.impl.BookChapterServiceImpl;
import com.etread.service.impl.BookInfoServiceImpl;
import com.etread.service.impl.BookParseServiceImpl;

import com.etread.service.impl.UserBookshelfServiceImpl;
import com.etread.vo.UploadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//书的上传
@RestController
@RequestMapping("/book")
public class UploadController {
    @Autowired private BookInfoServiceImpl bookInfoService;
    @Autowired private BookParseServiceImpl bookParseService;
    @Autowired private Tokencheck tokencheck;
    @Autowired private BookChapterServiceImpl bookChapterServiceImpl;
    @Autowired private UserBookshelfServiceImpl userBookshelfServiceImpl;

    @PostMapping("/upload")
    public Result<UploadVO> uploadet(@RequestHeader("token") String token,BookUploadDTO uploadDTO) {
       BookInfoDTO bookInfoDTO=bookInfoService.buildBookInfoDTO(uploadDTO,token);
       if(bookInfoDTO!=null){
           bookParseService.parseBookConcurrently(bookInfoDTO);
           //把书加入上传者书架
           userBookshelfServiceImpl.addToShelf(uploadDTO.getPublisher(),bookInfoDTO.getBookid());
           UploadVO uploadVO=new UploadVO();
           uploadVO.setBookInfo(bookInfoDTO);
           uploadVO.setBookChapter(bookChapterServiceImpl.listByBookIdPrefix(bookInfoDTO.getBookid()));
           return Result.success("上传成功",uploadVO);
       }else{
           UploadVO uploadVO=new UploadVO();
           return Result.error("上传失败");
       }
    }
    @PostMapping("/delete")
    public Result<UploadVO> delete(@RequestHeader("token") String token,BookInfoDTO bookInfoDTO) {
        if(bookInfoDTO!=null&&bookInfoDTO.getBookid()!=null){
            if(tokencheck.checkToken(token,bookInfoDTO.getBookid())){
                bookInfoService.deleteBook(bookInfoDTO.getBookid());
                bookChapterServiceImpl.removeByBookId(bookInfoDTO.getBookid());

                UploadVO uploadVO=new UploadVO();
                return Result.success("删除成功",uploadVO);
            }else{
                throw new RuntimeException("只有上传者有权删除书");
            }

        }else{
            UploadVO uploadVO=new UploadVO();
            return Result.error("数据错误");
        }

    }
    @PostMapping("/updateinfo")
    public Result<UploadVO> updatebookinfo(@RequestHeader("token") String token,BookInfoDTO bookInfoDTO){
        if(bookInfoDTO!=null&&bookInfoDTO.getBookid()!=null){
            if(tokencheck.checkToken(token,bookInfoDTO.getBookid())){
                bookInfoService.updateBook(bookInfoDTO);
                UploadVO uploadVO=new UploadVO();
                return Result.success("更新成功",uploadVO);
            }else{
                UploadVO uploadVO=new UploadVO();

                return Result.error("认证错误");
            }
        }else{
            UploadVO uploadVO=new UploadVO();
            return Result.error("数据错误");
        }
    }



}
