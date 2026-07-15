package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ClaimActionMapper extends BaseMapper<ClaimActionData> {

    default List<ClaimActionData> selectByClaimId(String claimId) {
        return selectList(new LambdaQueryWrapper<ClaimActionData>()
                .eq(ClaimActionData::getClaimId, claimId)
                .orderByAsc(ClaimActionData::getSequenceNo, ClaimActionData::getId));
    }
}
