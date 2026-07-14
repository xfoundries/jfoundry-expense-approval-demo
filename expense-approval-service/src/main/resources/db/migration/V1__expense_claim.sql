create table expense_claim (
    id varchar(36) primary key,
    claimant_id varchar(100) not null,
    title varchar(200) not null,
    state varchar(40) not null,
    total_amount decimal(19, 2) not null,
    finance_approval_required boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    submitted_at timestamp with time zone,
    completed_at timestamp with time zone,
    version bigint not null
);

create table expense_item (
    id varchar(36) primary key,
    claim_id varchar(36) not null,
    item_order integer not null,
    expense_date date not null,
    category varchar(40) not null,
    amount decimal(19, 2) not null,
    description varchar(500) not null,
    receipt_reference varchar(500),
    constraint fk_expense_item_claim foreign key (claim_id)
        references expense_claim(id) on delete cascade
);

create index idx_expense_item_claim
    on expense_item(claim_id, item_order);

create table claim_action (
    id varchar(80) primary key,
    claim_id varchar(36) not null,
    sequence_no integer not null,
    action_type varchar(40) not null,
    actor_id varchar(100) not null,
    acted_at timestamp with time zone not null,
    resulting_state varchar(40) not null,
    reason varchar(500),
    constraint uq_claim_action_sequence unique (claim_id, sequence_no),
    constraint fk_claim_action_claim foreign key (claim_id)
        references expense_claim(id) on delete cascade
);

create index idx_claim_action_claim
    on claim_action(claim_id, sequence_no);

create index idx_expense_claim_monthly_approval
    on expense_claim(claimant_id, state, completed_at);
