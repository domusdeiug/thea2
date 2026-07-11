# Notif Spike — Setup & What To Look For

This is a minimal Android app with ONE job: prove that `NotificationListenerService`
can actually capture notification content (sender/title/text) from apps like
WhatsApp and TikTok on a real device. Nothing else. No AI, no database, no styling.

---

## 1. Push to GitHub

```
git init
git add .
git commit -m "notif spike"
git remote add origin <your-repo-url>
git push -u origin main
```

## 2. Let GitHub Actions build it — no IDE needed at all

This repo includes `.github/workflows/build-apk.yml`, which builds the debug APK
automatically in the cloud every time you push to `main`. This is the easiest
path given a weak laptop — nothing runs locally, nothing runs in a cloud IDE,
GitHub's own servers do the build.

- Once you've pushed, go to your repo on GitHub → **Actions** tab.
- You should see a workflow run start automatically ("Build Debug APK").
  If it doesn't start on its own, click into the workflow and use
  **"Run workflow"** (the `workflow_dispatch` trigger is there for this).
- Wait for it to finish (a few minutes — first run is slower since it downloads
  Gradle and dependencies).
- Click into the completed run → scroll to **Artifacts** → download
  `notif-spike-debug-apk`. This downloads a `.zip` containing `app-debug.apk`.
- Unzip it — you now have the APK.

## 3. Get the APK onto your phone

- Easiest: email it to yourself, or upload to Google Drive / WhatsApp-to-yourself,
  and download directly on the phone.
- Or: plug the phone into your laptop via USB and copy the file over.

## 4. Install the APK on your phone

- You'll likely need to allow "install from unknown sources" for whatever app
  you're using to open the APK file (file manager, browser download, etc.) —
  Android will prompt you for this on first install.
- Install it. It'll show up as "Notif Spike".

## 5. Run the test

1. Open the app.
2. Tap **"Grant Notification Access."** This opens Android's system settings —
   you'll see a list of apps requesting notification access. Find **"Notif Spike"**
   and toggle it ON. Android may show a warning dialog about the permission being
   sensitive — accept it.
3. Go back into the Notif Spike app (press back or reopen it).
4. Now trigger some real notifications:
   - Have someone WhatsApp you, or message yourself from another number/WhatsApp Web
   - Open TikTok and let a like/comment/DM notification come in
   - Send yourself an SMS or email
5. Come back to the Notif Spike app (or it may already be showing them if it's
   still open — it live-refreshes).
6. You should see a growing list, newest at the top, each entry showing:
   - The app's package name (e.g. `com.whatsapp`, `com.zhiliaoapp.musically` for TikTok)
   - Title
   - Text (the actual message content, if captured)

---

## What you're checking for (this is the actual point of the exercise)

**✅ Good sign:** You see entries like
`com.whatsapp | Title: John | Text: hey are you around today?`
— this means real message content is being captured. The core mechanic works.

**⚠️ Partial/concerning sign:** You see the app and title, but Text just says
something generic like `"New message"` or is blank. This usually means your
phone's **lock-screen notification privacy setting** is hiding content (Settings →
Notifications → sometimes per-app, sometimes global "hide sensitive content on
lock screen"). Check that setting — for many phones this defaults to hiding
content, and the user (or you, as the phone owner) would need to allow full
content to show. Worth testing with the screen unlocked vs. locked.

**❌ Bad sign / needs investigation:** Nothing shows up at all after enabling
permission and triggering notifications. Could mean:
- The permission wasn't actually granted (double check step 2)
- The app got killed in the background — some phones (Xiaomi, Huawei, Samsung
  especially) aggressively kill background services by default. Check
  Settings → Battery → App battery usage → Notif Spike → set to "unrestricted"
  or disable battery optimization for this app specifically.
- WhatsApp/TikTok notification settings on your phone have notifications
  disabled for that app entirely (unlikely if you can see them in your
  notification shade normally, but worth ruling out)

**Also worth testing:** leave the app installed and NOT open for an hour, doing
normal phone use, then check if notifications from that period were still
captured. This tests whether Android kills the listener service when the app
isn't in the foreground — a real risk mentioned in the build plan, and this
is the cheapest way to find out early.

---

## Once this works

If you're seeing real message content flowing in reliably (including after the
app's been in the background a while), the riskiest unknown in the whole project
is validated. Report back what you saw — especially any of the ⚠️ or ❌ cases —
and we'll move to the next step (local Room database storage, replacing this
in-memory list).
