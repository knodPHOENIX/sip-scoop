# Sip & Scoop

A personal Android app for tracking daily water and protein, with home-screen widgets.

## Get the APK with GitHub (no installs needed)

1. Sign in at github.com (create a free account if you don't have one).
2. Click **+** (top right) → **New repository**. Name it `sip-and-scoop`, choose **Private**, and click **Create repository**.
3. On the next page click **uploading an existing file**.
4. Unzip `SipAndScoop.zip` on your computer. Open the `SipAndScoop` folder, select everything inside it
   (including the `.github` folder) and drag it all onto the GitHub page. Click **Commit changes**.
5. Open the **Actions** tab. A run called "Build APK" starts by itself and takes about 5–8 minutes.
   A green tick means it worked.
6. Open the **Releases** link on the repository's main page (right side), open the newest build
   and download **SipAndScoop.apk**. Do this on your phone, or send the file to your phone.
7. Open the APK on your phone. Android will ask you to allow installs from this source (your browser
   or Files app). Allow it and tap **Install**.

### If the Actions tab shows nothing
The `.github` folder can be hidden on Mac and was probably skipped. In the repository click
**Add file → Create new file**, type `.github/workflows/build.yml` as the name, paste in the contents of
that file from the zip, and click **Commit changes**.

### Adding the widget
Long-press your home screen → **Widgets** → **Sip & Scoop**. Pick the wide (4×2) or compact (2×2) one.
Its buttons add your first quick-add amount (+250 ml and +10 g unless you changed them).

### Updating later
Upload changed files the same way; a new build appears under Releases. It installs over the old app
and keeps your data, because every build is signed with the same key (`app/personal.keystore`).
Keep the repository private, since that key is in it.
