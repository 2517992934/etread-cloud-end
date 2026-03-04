package com.etread.controller;

import com.etread.Result;
import com.etread.dto.BookInfoDTO;
import com.etread.dto.BookUploadDTO;
import com.etread.service.impl.BookInfoServiceImpl;
import com.etread.service.impl.BookParseServiceImpl;
import com.etread.vo.UploadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book")
public class UploadController {
    @Autowired private BookInfoServiceImpl bookInfoService;
    @Autowired private BookParseServiceImpl bookParseService;
    @PostMapping("/upload")
    public Result<UploadVO> uploadet(@RequestHeader("token") String token,BookUploadDTO uploadDTO) {
       BookInfoDTO bookInfoDTO=bookInfoService.buildBookInfoDTO(uploadDTO,token);
       if(bookInfoDTO!=null){
           bookParseService.parseBookConcurrently(bookInfoDTO);
           UploadVO uploadVO=new UploadVO();
           uploadVO.setUpload_message("上传成功！");
           return Result.success("上传成功",uploadVO);
       }else{
           UploadVO uploadVO=new UploadVO();
           uploadVO.setUpload_message("上传失败，请重试");
           return Result.error("上传失败");
       }



    }

}
