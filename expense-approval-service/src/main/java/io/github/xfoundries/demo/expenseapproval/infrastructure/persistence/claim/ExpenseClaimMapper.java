package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.math.BigDecimal;
import java.time.Instant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExpenseClaimMapper extends BaseMapper<ExpenseClaimData> {

    @Select("""
            select coalesce(sum(total_amount), 0)
            from expense_claim
            where claimant_id = #{employeeId}
              and state = 'APPROVED'
              and completed_at >= #{fromInclusive}
              and completed_at < #{toExclusive}
            """)
    BigDecimal sumApprovedAmount(
            @Param("employeeId") String employeeId,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);
}
