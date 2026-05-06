# Implementation Log

## 2026-05-07

Files changed:

- `src/main/resources/db/changelog/db.changelog-master.xml`
- `src/main/resources/db/changelog/changes/019-add-admin-notification-campaigns.xml`
- `src/main/java/com/tourguide/admin/contenteditor/ContentEditorController.java`
- `src/main/java/com/tourguide/admin/contenteditor/dto/AdminSendNotificationRequest.java`
- `src/main/java/com/tourguide/admin/contenteditor/dto/AdminSendNotificationResponse.java`
- `src/main/java/com/tourguide/notification/*`
- `src/main/java/com/tourguide/user/UserRepository.java`

What was done:

- Added admin/editor mobile notification send endpoint.
- Added notification campaign table and entity.
- Extended mobile inbox notifications with deep link, JSON data, campaign id, and push status fields.
- Extended device tokens with provider, active flag, last seen, and update timestamps.
- Added push sender interface plus logging FCM stub.

Important decisions:

- Dashboard sends notifications to active `TOURIST` users.
- Inbox records are created even if a user has no active device token.
- Real FCM delivery is isolated behind `PushNotificationSender`.

Follow-up tasks:

- Add Firebase Admin SDK sender.
- Add campaign history endpoints.
