# Multi-user backend — client notes

**Status:** planning only. The Android app on this branch is unchanged; this
document records the contract the client will build against so app work does not
start from assumptions.

- Backend branch: `feat/multi-user`
- Backend test origin: `https://testalexa.synthora.in`
- Production origin: `https://alexa.synthora.in` (unchanged, single-owner)
- Backend admin panel: `https://testalexa.synthora.in/admin/` (deployed and
  smoke-tested on this branch; not linked from the app)

## What exists today

1. **Admin identities.** The backend can create, disable, delete and restore
   users; assign `owner`/`admin`/`user` roles; verify email addresses; send
   invitations; reset passwords; and issue magic sign-in links.
2. **Isolated storage.** Users, sessions, email tokens, encrypted provider
   credentials, preferences and audit events have their own tables.
3. **No client-facing account API yet.** The current app continues to use the
   existing `API_KEY` + single-owner `/login/`/`/remote/` contract. The test
   deployment keeps that contract working under the same API key.
4. **No playback scoping yet.** Playlists, history, likes, queues and current
   playback are still shared. Do not assume two accounts see different music.

## Client roadmap

| Phase | App work | Gate |
| --- | --- | --- |
| 0 (current) | None. Keep using the existing owner configuration. | Backend Phase 1 deployed on the test origin. |
| 1 | Freeze the auth/profile contract with the backend. | Backend Phase 3 API merged. |
| 2 | Add a debug-only account layer: sign-in, token storage, refresh, sign-out. | Contract tests against `testalexa`. |
| 3 | Move user-visible profile and provider status into the account screen. | Per-user YouTube/Amazon linking available. |
| 4 | Scope playlists, history, likes and queues to the signed-in account. | Backend Phase 2 data migration rehearsed on a production copy. |
| 5 | Remove the debug flag and ship the account release. | Security review, backup/restore drill, rollback plan. |

No app code changes start before Phase 1, so the current release keeps pointing
at production.

## Non-goals on this branch

- No change to `alexa.synthora.in`.
- No change to the Alexa skill, Lambda, interaction model or
  `lambda/api_key.py`.
- No production migration.
- No client code changes until the Phase 2/3 API contract is accepted.

## Planned client contract (Phase 3)

The exact payloads will be frozen with the backend implementation. The intended
shape:

### Sign in

```http
POST /api/v1/auth/login
Content-Type: application/json

{"identifier": "user@synthora.in", "password": "…", "device_name": "Pixel 9"}
```

Response (short-lived access token + rotating refresh token):

```json
{
  "user": {"id": "…", "username": "…", "display_name": "…", "role": "user"},
  "access_token": "…",
  "expires_in": 900,
  "refresh_token": "…"
}
```

### Refresh / sign out

```http
POST /api/v1/auth/refresh   {"refresh_token": "…"}
POST /api/v1/auth/logout    Authorization: Bearer <access>
```

### Current account

```http
GET /api/v1/me
Authorization: Bearer <access>
```

### Profile and provider accounts

```http
PATCH /api/v1/me            {"display_name": "…"}
GET   /api/v1/me/providers
POST  /api/v1/me/providers/youtube/connect
DELETE /api/v1/me/providers/youtube
```

Provider secrets never travel back to the app; the backend stores encrypted
credentials and only exposes status/metadata.

### Playback scoping (Phase 2)

Once scoped, every existing playback endpoint will accept the authenticated
user (bearer token or session cookie) and operate on that user's data. The
legacy `API_KEY` device path remains for the Alexa integration. The app should
not send both an API key and a user token to the same endpoint.

## Migration strategy for the app

1. Keep the current release pointed at production.
2. Add the account/session layer behind a debug-only or remote feature flag.
3. Mint a token only when the user explicitly signs in; anonymous mode keeps
   using the existing owner configuration.
4. Migrate local playlists/history to the server account only after Phase 2
   scoping is live and a dry-run import has been verified.
5. Cut over only after the backend's backup/restore drill and security review.

## Testing the test deployment

The test host is reachable at `https://testalexa.synthora.in`. A debug build can
point at it with:

```properties
ALEXA_SESSION_BASE_URL=https://testalexa.synthora.in
ALEXA_BASE_URL=https://testalexa.synthora.in
ALEXA_API_KEY=<test deployment API_KEY>
```

Never ship those test values in a release build. The production Lambda and API
key are intentionally not reused.

## Open questions

- Invitation-only onboarding vs. self-service registration.
- Whether Google OAuth should be the primary sign-in for YouTube linking.
- How to represent an account on shared Alexa devices without changing the
  skill.
- Offline behavior when a refresh token expires while the app is backgrounded.
- Data export/deletion requirements for personal data.

Answers belong in the backend roadmap
(`docs/multi-user/ROADMAP.md` in `youtube-music-alexa-skill`) before client
implementation starts.
