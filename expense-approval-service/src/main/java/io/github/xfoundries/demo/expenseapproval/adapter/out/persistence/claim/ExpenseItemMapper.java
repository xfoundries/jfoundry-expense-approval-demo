package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ExpenseItemMapper extends BaseMapper<ExpenseItemData> {

    default List<ExpenseItemData> selectByClaimId(String claimId) {
        return selectList(new LambdaQueryWrapper<ExpenseItemData>()
                .eq(ExpenseItemData::getClaimId, claimId)
                .orderByAsc(ExpenseItemData::getItemOrder, ExpenseItemData::getId));
    }

    default int deleteByClaimId(String claimId) {
        return delete(new LambdaQueryWrapper<ExpenseItemData>()
                .eq(ExpenseItemData::getClaimId, claimId));
    }
}
