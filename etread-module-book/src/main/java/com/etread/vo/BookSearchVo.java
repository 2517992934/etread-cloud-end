package com.etread.vo;

import com.etread.entity.BookInfo;
import lombok.Data;

import java.util.List;
@Data
public class BookSearchVo {
    private List<BookInfo> ResultBooks;
}
