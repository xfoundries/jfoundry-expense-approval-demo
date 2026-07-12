package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExpenseItemMapper extends BaseMapper<ExpenseItemData> {

    @Select("select * from expense_item where claim_id = #{claimId} order by item_order, id")
    List<ExpenseItemData> selectByClaimId(@Param("claimId") String claimId);

    @Delete("delete from expense_item where claim_id = #{claimId}")
    int deleteByClaimId(@Param("claimId") String claimId);
}

