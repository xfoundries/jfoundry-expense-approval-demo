package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("claim_action")
public class ClaimActionData {

    @TableId(type = IdType.INPUT)
    private String id;
    private String claimId;
    private int sequenceNo;
    private String actionType;
    private String actorId;
    private Instant actedAt;
    private String resultingState;
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }
    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public Instant getActedAt() { return actedAt; }
    public void setActedAt(Instant actedAt) { this.actedAt = actedAt; }
    public String getResultingState() { return resultingState; }
    public void setResultingState(String resultingState) { this.resultingState = resultingState; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
