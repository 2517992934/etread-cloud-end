package com.etread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etread.entity.BookInfo;
import com.etread.vo.BookInfoBaseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookInfoMapper extends BaseMapper<BookInfo> {

    @Select("""
        SELECT
            id,
            title,
            author,
            cover_url AS coverUrl,
            description,
            word_count AS wordCount,
            total_score AS totalScore,
            rating_count AS ratingCount
        FROM book_info
        WHERE id = #{bookId}
        """)
    BookInfoBaseVO selectBaseById(@Param("bookId") Long bookId);
}
