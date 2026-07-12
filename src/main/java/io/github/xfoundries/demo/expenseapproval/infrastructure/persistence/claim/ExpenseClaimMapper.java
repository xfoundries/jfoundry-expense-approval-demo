package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

public interface ExpenseClaimMapper extends BaseMapper<ExpenseClaimData> {

    @Update("""
            update expense_claim
               set claimant_id = #{claimantId},
                   title = #{title},
                   state = #{state},
                   total_amount = #{totalAmount},
                   finance_approval_required = #{financeApprovalRequired},
                   created_at = #{createdAt},
                   updated_at = #{updatedAt},
                   submitted_at = #{submittedAt},
                   completed_at = #{completedAt},
                   version = version + 1
             where id = #{id}
               and version = #{version}
            """)
    int updateWithVersion(ExpenseClaimData data);
}
