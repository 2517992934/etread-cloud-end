package com.etread.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etread.dto.BookInfoDTO;
import com.etread.dto.BookUploadDTO;
import com.etread.dto.UserDTO;
import com.etread.entity.BookInfo;
import com.etread.mapper.BookInfoMapper;
import com.etread.service.BookInfoService;
import com.etread.utils.IdGenerator;
import com.etread.utils.MinioUtil;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;

import java.io.File;

@Service
public class BookInfoServiceImpl extends ServiceImpl<BookInfoMapper, BookInfo> implements BookInfoService {
    @Autowired private MinioUtil minioUtil;
    @Autowired private RedisUtil redisUtil;

    /**
     * 存储封面和头像
     * 为书赋予id
     * @param bookInfo
     * @return
     */
    @Override
    public boolean updateStatus(BookInfoDTO bookInfo) {
        Long bookid = bookInfo.getBookid();
        int status = bookInfo.getStatus();
        String errorMsg=bookInfo.getError_msg();
        return this.lambdaUpdate()
                .eq(BookInfo::getId, bookid)
                .set(BookInfo::getStatus, status)
                .set(errorMsg != null, BookInfo::getErrorMsg, errorMsg)
                .update();

    }
    public BookInfoDTO buildBookInfoDTO(BookUploadDTO book,String token) {
        MultipartFile file = book.getFile();
        File localTempFile =null;
        Long bookid= IdGenerator.generateId();
        BookInfoDTO result = new BookInfoDTO();
        String fileUrl=null;
        String cover_url=null;
        try{
            String userJson = redisUtil.get(token);
            if (userJson != null) {
                // 反序列化为 UserDTO 对象
                UserDTO userDTO = JSON.parseObject(userJson, UserDTO.class);
                // 获取 account
                String account = userDTO.getAccount();
                book.setPublisher(account);
                System.out.println("account: " + account);
            } else {
                // key 不存在或已过期
                System.out.println("未找到用户信息");
                throw new RuntimeException("key 不存在或已过期");
            }
        }catch(Exception e) {
            throw new RuntimeException("上传者非法");
        }
        try {
            fileUrl = minioUtil.uploadFile(file, "books");
            MultipartFile cover=book.getCover();
            if(cover!=null){
                byte[] bytes = cover.getBytes();
                String contentType=cover.getContentType();
                String originalFilename = file.getOriginalFilename();
                String objectName = "cover/" + System.currentTimeMillis() + "_" + originalFilename;
                cover_url = minioUtil.uploadBytes(bytes, objectName, contentType, "contentpicture");
            }else{
                throw new RuntimeException("头像出问题了");
            }
        }catch(Exception e) {
            throw new RuntimeException("error occured while uploading file");
        }
        BookInfo bookInfo=transferdtotodao(book);
        bookInfo.setOriginalFileUrl(fileUrl);
        bookInfo.setCoverUrl(cover_url);
        bookInfo.setId(bookid);

        this.save(bookInfo);
        result=transferdtotodto(book);
        result.setBookid(bookid);
        result.setCover_url(cover_url);
        result.setFilename(file.getOriginalFilename());
        try {
            // 1. 🌟 凭 URL 提取 File！(调用你刚写好的下载魔法)
            localTempFile = minioUtil.downloadToTemp("books",fileUrl);
            result.setFile(localTempFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;


    }
    public BookInfo transferdtotodao(BookUploadDTO book) {
        BookInfo bookInfo = new BookInfo();
        bookInfo.setTitle(book.getTitle());
        bookInfo.setAuthor(book.getAuthor());
        bookInfo.setTags(String.join(",", book.getTags()));
        bookInfo.setDescription(book.getDescription());
        bookInfo.setPublisher(book.getPublisher());
        bookInfo.setPublisher(book.getPublisher());
        bookInfo.setStatus(0);
        return bookInfo;
    }
    public BookInfoDTO transferdtotodto(BookUploadDTO book) {
        BookInfoDTO bookInfoDTO = new BookInfoDTO();
        bookInfoDTO.setTitle(book.getTitle());
        bookInfoDTO.setAuthor(book.getAuthor());
        bookInfoDTO.setTags(book.getTags());
        bookInfoDTO.setDescription(book.getDescription());
        bookInfoDTO.setStatus(0);
        return bookInfoDTO;
    }
}
