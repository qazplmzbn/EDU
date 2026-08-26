package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileCategoryStat;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentProfileCategoryStatMapper {
    @Insert("INSERT INTO student_profile_category_stat(user_id,category_type,period_type,total_count,strong_count,weak_count,average_score,coverage_rate,calculated_at) VALUES(#{userId},#{categoryType},'current',#{totalCount},#{strongCount},#{weakCount},#{averageScore},#{coverageRate},NOW(3)) ON DUPLICATE KEY UPDATE total_count=VALUES(total_count),strong_count=VALUES(strong_count),weak_count=VALUES(weak_count),average_score=VALUES(average_score),coverage_rate=VALUES(coverage_rate),calculated_at=NOW(3)")
    int upsert(@Param("userId") Long userId,@Param("categoryType") String categoryType,@Param("totalCount") int totalCount,
               @Param("strongCount") int strongCount,@Param("weakCount") int weakCount,@Param("averageScore") BigDecimal averageScore,@Param("coverageRate") BigDecimal coverageRate);
    @Select("SELECT * FROM student_profile_category_stat WHERE user_id=#{userId} AND period_type='current' ORDER BY category_type")
    List<StudentProfileCategoryStat> selectCurrent(@Param("userId") Long userId);
}
