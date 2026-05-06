# API Reference

Last updated: 2026-05-07

## Send Mobile Notification

- Method: `POST`
- Path: `/admin/editor/notifications`
- Auth: `SUPERADMIN` or `CONTENT_EDITOR`

Request:

```json
{
  "title": "Yeni rota eklendi",
  "body": "İzmir için yeni yürüyüş rotalarını keşfet.",
  "type": "SYSTEM",
  "targetType": "ALL_TOURISTS",
  "deepLink": "tourguide://routes",
  "data": {
    "screen": "routes"
  }
}
```

Selected users:

```json
{
  "title": "Görev hatırlatması",
  "body": "Yarım kalan görevini tamamlamayı unutma.",
  "type": "SYSTEM",
  "targetType": "SPECIFIC_USERS",
  "targetUserIds": ["8f063732-6d22-4e73-a1eb-f5da318aa6f8"]
}
```

Response:

```json
{
  "campaignId": "25dfd75d-2107-4da2-98d8-9b1b9578c925",
  "status": "SENT",
  "totalRecipients": 128,
  "successCount": 90,
  "failureCount": 0
}
```

Errors:

- `400` validation failure or no matching active tourist recipients.
- `401/403` missing or insufficient admin auth.

## Register Device Token

- Method: `POST`
- Path: `/notifications/device-token`
- Auth: authenticated user

Request:

```json
{
  "token": "fcm-device-token",
  "platform": "ANDROID",
  "provider": "FCM"
}
```

Response: `201 Created`

## List Notifications

- Method: `GET`
- Path: `/notifications?page=0&size=20`
- Auth: authenticated user

Response now includes optional `deepLink` and `data` fields for mobile routing.
