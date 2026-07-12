package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ClaimActionMapper extends BaseMapper<ClaimActionData> {

    @Select("select * from claim_action where claim_id = #{claimId} order by sequence_no, id")
    List<ClaimActionData> selectByClaimId(@Param("claimId") String claimId);

    @Delete("delete from claim_action where claim_id = #{claimId}")
    int deleteByClaimId(@Param("claimId") String claimId);
}

