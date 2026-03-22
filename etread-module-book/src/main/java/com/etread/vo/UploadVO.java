package com.etread.vo;

import com.etread.dto.BookInfoDTO;
import com.etread.entity.BookChapter;

import lombok.Data;

import java.util.List;


@Data
public class UploadVO {
    String message;
    List<BookChapter> bookChapter;
    BookInfoDTO bookInfo;
}
