package com.etread.vo;

import com.etread.entity.BookChapter;
import com.etread.entity.BookInfo;
import lombok.Data;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.util.Date;
import java.util.List;

@Data
public class AddToShelfVo {
    String message;
    List<BookChapter> bookChapter;
    BookInfo bookInfo;
    Date addTime;
}
