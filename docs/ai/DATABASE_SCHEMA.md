# Database Schema

Last updated: 2026-05-07

## notification_campaigns

Dashboard-originated mobile notification send record.

- `id uuid` primary key
- `title varchar(255)` not null
- `body text` not null
- `type varchar(50)` not null
- `target_type varchar(50)` not null
- `target_payload jsonb`
- `deep_link varchar(500)`
- `data jsonb`
- `status varchar(30)` not null, default `DRAFT`
- `created_by uuid` not null, FK `users.id`
- `sent_at timestamp`
- `total_recipients integer` not null, default `0`
- `success_count integer` not null, default `0`
- `failure_count integer` not null, default `0`
- `is_active boolean` not null, default `true`
- `created_at timestamp` not null, default `CURRENT_TIMESTAMP`
- `updated_at timestamp`

Indexes:

- `idx_notification_campaigns_created_at`
- `idx_notification_campaigns_status`

## notifications

Per-user mobile inbox notification record.

Existing core columns include `id`, `user_id`, `type`, `title`, `body`, `is_read`, and `created_at`.

Added columns:

- `campaign_id uuid`, FK `notification_campaigns.id`
- `deep_link varchar(500)`
- `data jsonb`
- `push_status varchar(30)` not null, default `PENDING`
- `push_error text`
- `sent_at timestamp`

Indexes:

- `idx_notifications_campaign`
- Existing user listing/unread indexes remain unchanged.

## device_tokens

Mobile push token record.

Existing core columns include `id`, `user_id`, `token`, `platform`, and `created_at`.

Added columns:

- `provider varchar(30)` not null, default `FCM`
- `is_active boolean` not null, default `true`
- `last_seen_at timestamp` not null, default `CURRENT_TIMESTAMP`
- `updated_at timestamp`

Indexes:

- `idx_device_tokens_user_active`

## Latest Seed Data Summary

- No seed data changed.
