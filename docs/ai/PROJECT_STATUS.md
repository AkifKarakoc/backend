# Project Status

Last updated: 2026-05-07

## Implemented

- User-facing notification inbox endpoints under `/notifications`.
- Mobile device token registration under `/notifications/device-token`.
- Admin/editor mobile notification sending under `/admin/editor/notifications`.
- Notification campaign persistence for dashboard-originated sends.
- Per-user notification records now support campaign links, deep links, structured data, and push delivery status.

## Missing

- Real Firebase Cloud Messaging integration. The current sender is a logging stub.
- Admin campaign history/list/detail endpoints.
- Retry handling for failed push deliveries.

## Known Issues

- Users without active device tokens still receive inbox records, but no phone push is sent.

## Next Recommended Tasks

- Replace `LoggingPushNotificationSender` with a Firebase Admin SDK implementation.
- Add dashboard campaign history after the compose flow is stable.
