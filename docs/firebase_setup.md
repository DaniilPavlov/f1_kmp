# Firebase Console setup (Auth + Firestore)

**Separate** Firebase project for KMP (do **not** reuse `f1-kotlin`).

Suggested project id: `f1-kmp`  
Android package: `com.example.f1_kmp`  
iOS bundle: `com.example.f1kmp`  

`composeApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist` are gitignored  
(CI uses `tool/ci/google-services.stub.json` for Android).

Already used in the app: Core, Analytics, Crashlytics, Remote Config.

Profile / Predictor need **Auth (email/password)** + **Cloud Firestore** on this new project.

## Create project and apps

1. [Firebase Console](https://console.firebase.google.com/) → Add project (e.g. `f1-kmp`).
2. Add **Android** app with package `com.example.f1_kmp`.
   - Add **debug** SHA-1 (see below).
   - Download `google-services.json` → `composeApp/google-services.json` (replace any CI stub copy).
3. Add **iOS** app with bundle id `com.example.f1kmp`.
   - Download `GoogleService-Info.plist` → `iosApp/iosApp/GoogleService-Info.plist`.

## Enable for Profile / Predictor

1. **Authentication → Sign-in method → Email/Password**
   - Enable Email/Password (not passwordless).
   - Do not enable Google / Apple / Anonymous for v1.
   - Templates → Email address verification — keep default or customize.

2. **Cloud Firestore**
   - Create database (production mode).
   - Prefer a region close to users (e.g. `eur3` / `europe-west`).
   - Rules (Console → Firestore → Rules → Publish):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() {
      return request.auth != null;
    }
    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }
    function isVerified() {
      return isSignedIn() && request.auth.token.email_verified == true;
    }

    match /users/{uid} {
      allow read, write: if isOwner(uid);
    }
    match /users/{uid}/seasons/{document=**} {
      allow read, write: if isOwner(uid) && isVerified();
    }

    match /nicknames/{normalized} {
      allow read: if isVerified();
      allow create: if isVerified()
        && request.resource.data.uid == request.auth.uid
        && request.resource.data.keys().hasOnly(['uid', 'nickname']);
      allow update, delete: if isVerified()
        && resource.data.uid == request.auth.uid;
    }

    match /leaderboards/{year}/entries/{uid} {
      allow read: if isVerified();
      allow create, update: if isVerified()
        && request.auth.uid == uid
        && request.resource.data.keys().hasAll(['nickname', 'totalPoints'])
        && request.resource.data.nickname is string
        && request.resource.data.totalPoints is int;
      allow delete: if isVerified() && request.auth.uid == uid;
    }
  }
}
```

3. **App Check** — optional / skipped for v1.

## Data shape

```
users/{uid}
  email, emailVerified, createdAt
  nickname?, nicknameNormalized?, leaderboardOptIn?, leaderboardOptInAt?

users/{uid}/seasons/{year}
  weekends: { "{round}": { … } }
  updatedAt

nicknames/{normalizedNickname}
  uid, nickname

leaderboards/{year}/entries/{uid}
  nickname, totalPoints, updatedAt
```

## Abuse hardening (in app)

- Registration sends email verification.
- Predictor cloud features gated until `emailVerified`.
- Leaderboard requires nickname + explicit opt-in; leave removes the public entry.
- Before Firestore calls that need `email_verified` claim, the app refreshes the ID token.

## Debug SHA-1 (Android `DEVELOPER_ERROR`)

```bash
keytool -list -v -alias androiddebugkey \
  -keystore ~/.android/debug.keystore -storepass android -keypass android
```

Console → Project settings → Your apps → `com.example.f1_kmp` → Add fingerprint →
download a fresh `google-services.json` if prompted.

## Not needed yet

App Check, Storage, Cloud Messaging, Anonymous Auth.
