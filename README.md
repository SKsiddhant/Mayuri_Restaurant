# Mayuri Restaurant

A restaurant ordering app with two parts:

- **Android app** (`app/`): Kotlin + Jetpack Compose + Firebase (Auth and Firestore). Customers browse the menu, order and track orders; the owner manages the menu and order status.
- **Web prototype** (`src/`, `index.html`, `mayuri-app.html`): the original Figma Make design bundle (React + Vite), not connected to Firebase. `npm i && npm run dev`.

## Android setup

1. Open the project in Android Studio (it creates `local.properties` for you; that file is git-ignored).
2. `app/google-services.json` (git-ignored) is the Firebase client configuration: download it from **your** Firebase project (`app/google-services.json.example` shows the shape). Restrict its API key to the app's package name / SHA-1 in Google Cloud Console.
3. In the Firebase console enable **Authentication -> Email/Password** and create **Firestore**.
4. Deploy the security rules in `firestore.rules`:
   ```bash
   npm i -g firebase-tools
   firebase deploy --only firestore:rules
   ```
5. **Create the owner account.** Roles are never granted by the app. Register the owner normally in the app, then in the Firebase console open Firestore -> `users` -> that user's document and change `role` from `user` to `owner`. Use a strong password.
6. Sign in as the owner and press **Profile -> Seed Demo Menu** to add sample dishes (safe to press more than once).

Debug builds show "Demo Accounts" buttons on the login screen (`user@mayuri.com` / `owner@mayuri.com`, password `123456`). Create those accounts yourself if you want them, and never in a project with real data. Release builds do not show them.

## Known limitations
- Payment is a simulation (UPI / Card / Cash on Delivery all just place the order). No money moves.
- Order totals are computed on the device; Firestore rules cannot verify prices.
- The "WELCOME20" banner on the home screen is not wired to any discount logic.
