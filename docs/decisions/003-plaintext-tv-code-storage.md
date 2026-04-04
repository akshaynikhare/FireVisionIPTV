# ADR-003: Keep TV Pairing Code in Plaintext SharedPreferences

## Status

Accepted

## Date

2026-04-04

## Context

The audit flagged that the TV pairing code is stored in plain `SharedPreferences` (`FireVisionSettings`) rather than `EncryptedSharedPreferences`. The code is a 6-digit PIN used to pair the TV device with a user account on the server.

We evaluated whether encrypting this value would meaningfully improve security.

## Decision

Keep the TV code in plaintext `SharedPreferences`. Do not migrate to `EncryptedSharedPreferences` for this field.

### Rationale

1. **Low sensitivity** — The TV code is a short-lived pairing PIN, not a credential. It identifies the device to the server but cannot be used to authenticate as a user, access account data, or perform privileged operations. The server controls what the code grants access to.

2. **Device-scoped** — The code is only meaningful on the paired device. Extracting it from one device and using it on another would just re-pair, which the legitimate user would notice (and can revoke from the web dashboard).

3. **Android sandboxing** — On a non-rooted Fire TV, SharedPreferences files are in the app's private data directory (`/data/data/com.cadnative.firevisioniptv/`), inaccessible to other apps. Root access bypasses EncryptedSharedPreferences too (KeyStore can be dumped).

4. **EncryptedSharedPreferences fragility** — The app already uses `SecurePreferences` (EncryptedSharedPreferences) for truly sensitive data. ESP has known issues with KeyStore corruption on certain devices, requiring a fallback path. Adding the TV code to ESP increases the surface area for startup failures on Fire TV sticks where KeyStore reliability is inconsistent.

5. **Server-side controls** — Rate limiting on pairing endpoints (ADR-003 in server repo), PIN expiry, and the ability to revoke pairings server-side are the primary defenses. Client-side encryption of the PIN adds minimal value on top of these.

## Alternatives Considered

### Migrate TV code to EncryptedSharedPreferences
Provides encryption at rest. Rejected: marginal security benefit vs. increased fragility on Fire TV hardware. The fallback-to-plaintext behavior in SecurePreferences would negate the encryption anyway.

### Store in Android Keystore directly
More robust than ESP for single values. Rejected: over-engineered for a non-secret identifier. Keystore operations add latency on every app launch.

## Consequences

**Positive:**
- No risk of startup failures due to KeyStore corruption on Fire TV sticks
- Simpler code path for the most frequently read preference
- Consistent with the threat model (server-side controls are the real defense)

**Negative:**
- TV code is readable by anyone with root access to the device (accepted risk — root access compromises everything anyway)
- Audit tools may continue to flag this as a finding (this ADR serves as the documented exception)
