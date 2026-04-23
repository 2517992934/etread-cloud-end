package com.etread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etread.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookChapterMapper extends BaseMapper<BookChapter> {
    @Select("SELECT COALESCE(SUM(word_count), 0) FROM book_chapter WHERE book_id = #{bookId}")
    Long sumWordCountByBookId(@Param("bookId") Long bookId);

    @Update("UPDATE book_chapter SET sort_order = sort_order + 100 WHERE book_id = #{bookId} AND sort_order >= #{fromSortOrder}")
    int shiftSortOrders(@Param("bookId") Long bookId, @Param("fromSortOrder") Integer fromSortOrder);
}
