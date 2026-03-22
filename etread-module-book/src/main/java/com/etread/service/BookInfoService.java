package com.etread.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.etread.dto.BookInfoDTO;
import com.etread.dto.BookUploadDTO;
import com.etread.entity.BookInfo;

/**
 * 书籍信息业务接口
 * 职责：管理 book_info 表，继承 MyBatis-Plus 的 IService 获得 CRUD 能力
 */
public interface BookInfoService extends IService<BookInfo> {
    boolean updateStatus(BookInfoDTO bookInfo);
    BookInfoDTO buildBookInfoDTO(BookUploadDTO book, String token);
    boolean updateBook(BookInfoDTO bookInfoDTO);

    /**
     * 删除书籍
     */
    boolean deleteBook(Long bookId);
    /**
     * 根据书的id统计字数
     */
    long countwordbyBookId(Long bookId);
}