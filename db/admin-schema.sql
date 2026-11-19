-- Additive admin tables only. No existing customer, billing or exam table is altered.
-- Apply once to an existing MySQL database before running with schema validation.
create table admin_audit (id bigint not null auto_increment, action varchar(255) not null, actor varchar(255) not null, after_json longtext, before_json longtext, created_at datetime(6) not null, entity_id varchar(255), entity_type varchar(255) not null, reason varchar(1000) not null, primary key (id)) engine=InnoDB;
create table admin_credentials (id bigint not null, enabled bit not null, password_hash varchar(255) not null, touched_at datetime(6), valid_after datetime(6), version bigint not null, primary key (id)) engine=InnoDB;
create table admin_plan_settings (code varchar(255) not null, access_days integer, enabled bit not null, price_id varchar(255), quota integer not null, title varchar(255), version bigint not null, primary key (code)) engine=InnoDB;
create table admin_sessions (token_hash varchar(64) not null, created_at datetime(6), expires_at datetime(6), admin_id bigint not null, primary key (token_hash)) engine=InnoDB;
create table admin_subscription_state (id bigint not null, touched_at datetime(6), version bigint not null, primary key (id)) engine=InnoDB;
create table admin_user_state (id bigint not null, enabled bit not null, touched_at datetime(6), valid_after datetime(6), version bigint not null, primary key (id)) engine=InnoDB;
create index idx_admin_audit_created on admin_audit (created_at);
create index idx_admin_session_expiry on admin_sessions (expires_at);
alter table admin_sessions add constraint FK6nnftjmd6aehr1jc1sbii4g22 foreign key (admin_id) references admins (id);
