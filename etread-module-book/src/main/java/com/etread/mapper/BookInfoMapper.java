package com.etread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etread.dto.BookSearchDTO;
import com.etread.entity.BookInfo;
import com.etread.vo.BookInfoBaseVO;
import com.etread.vo.BookSearchVo;
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
    @Select("""
    <script>
    SELECT DISTINCT bi.*
    FROM book_info bi
    WHERE bi.status = 1

    <if test="dto.id != null">
        AND bi.id = #{dto.id}
    </if>

    <if test="dto.title != null and dto.title != ''">
        AND bi.title LIKE CONCAT('%', #{dto.title}, '%')
    </if>

    <if test="dto.author != null and dto.author != ''">
        AND bi.author LIKE CONCAT('%', #{dto.author}, '%')
    </if>

    <if test="dto.publisher != null and dto.publisher != ''">
        AND bi.publisher = #{dto.publisher}
    </if>

    <if test="dto.minwordcount != null">
        AND bi.word_count &gt;= #{dto.minwordcount}
    </if>

    <if test="dto.maxwordcount != null">
        AND bi.word_count &lt;= #{dto.maxwordcount}
    </if>

    <if test="dto.minscore != null">
        AND (
            CASE
                WHEN bi.rating_count IS NULL OR bi.rating_count = 0 THEN 0
                ELSE bi.total_score * 1.0 / bi.rating_count
            END
        ) &gt;= #{dto.minscore}
    </if>

    <if test="dto.maxscore != null">
        AND (
            CASE
                WHEN bi.rating_count IS NULL OR bi.rating_count = 0 THEN 0
                ELSE bi.total_score * 1.0 / bi.rating_count
            END
        ) &lt;= #{dto.maxscore}
    </if>

    <if test="dto.tags != null and dto.tags.size() > 0">
        AND bi.id IN (
            SELECT btr.book_id
            FROM book_tag_relation btr
            INNER JOIN book_tag bt ON btr.tag_id = bt.id
            WHERE bt.tag_name IN
            <foreach collection="dto.tags" item="tag" open="(" separator="," close=")">
                #{tag}
            </foreach>
            GROUP BY btr.book_id
            HAVING COUNT(DISTINCT bt.tag_name) = #{dto.tagCount}
        )
    </if>

    <if test="dto.updatetime != null">
        AND bi.update_time &lt; #{dto.updatetime}
    </if>

    <choose>
        <when test="dto.updatetime != null">
            ORDER BY bi.update_time DESC, bi.id DESC
        </when>
        <otherwise>
            ORDER BY
                CASE
                    WHEN bi.rating_count IS NULL OR bi.rating_count = 0 THEN 0
                    ELSE bi.total_score * 1.0 / bi.rating_count
                END DESC,
                bi.id DESC
        </otherwise>
    </choose>
    </script>
    """)
    Page<BookInfo> searchBooks(Page<BookInfo> page, @Param("dto") BookSearchDTO dto);
}
