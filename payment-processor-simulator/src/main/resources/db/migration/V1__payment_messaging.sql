create table jfoundry_outbox_event (
    event_id varchar(64) not null,
    topic varchar(255) not null,
    payload_key varchar(255),
    payload_type varchar(500) not null,
    payload_json text not null,
    aggregate_type varchar(255),
    aggregate_id varchar(255),
    aggregate_version bigint,
    status varchar(32) not null,
    retry_count int not null default 0,
    error_message varchar(2000),
    occurred_at timestamp not null,
    last_attempt_at timestamp,
    next_retry_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    claimed_at timestamp,
    claimed_by varchar(100),
    claim_token varchar(36),
    primary key (event_id)
);

create index idx_outbox_status_retry on jfoundry_outbox_event (status, next_retry_at);
create index idx_outbox_claim on jfoundry_outbox_event (status, claimed_at);
create index idx_outbox_cleanup on jfoundry_outbox_event (status, occurred_at, event_id);
create index idx_outbox_claim_token on jfoundry_outbox_event (claim_token);
create index idx_outbox_aggregate
    on jfoundry_outbox_event (aggregate_type, aggregate_id, aggregate_version);

create table jfoundry_inbox_message (
    id varchar(64) not null,
    message_id varchar(128) not null,
    consumer_name varchar(255) not null,
    status varchar(32) not null,
    processed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    error_message varchar(2000),
    primary key (id),
    constraint uk_inbox_consumer_message unique (consumer_name, message_id)
);

create index idx_inbox_processed_at on jfoundry_inbox_message (processed_at);
