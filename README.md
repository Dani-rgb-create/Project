# WebAI Copilot

WebAI Copilot is an expert, state-of-the-art developer companion built for Android with Kotlin and Jetpack Compose. It is designed to safely audit, correct, and optimize small local-business websites, static pages, schema graphs, and SEO indices.

## 🚀 Key Features

*   **Offline Web Analytics**: Scans local static projects (includes HTML files, robots.txt, sitemaps, templates) and highlights critical layout issues or SEO optimizations.
*   **One-Click SEO Generators**: Easily generate robust robots.txt, XML sitemaps, semantic Dublin Core & LocalBusiness schema graphs, and landing page wireframes directly from the dashboard.
*   **AI-Powered Chat Assistant**: Integrated with Gemini 1.5/2.0 Flash to suggest custom HTML refactoring, schema injections, and interactive FAQ generation.
*   **Secure Sandboxed Backups**: Live-tracking repository modifications. WebAI Copilot automatically stores restoration backup points before updating files, allowing 100% risk-free undoing and rolling back.
*   **Zip Import & Export**: Directly upload full zip templates, work on them, and export them as clean structure-ready zip archives.

---

## 🛠️ GitHub Build & CI/CD Debug APKs

This project comes equipped with an automated **GitHub Actions CI/CD workflow** located at `.github/workflows/android.yml`.

### How to get your Debug APK directly from GitHub:
1.  **Push the repository** to your GitHub account.
2.  Navigate to the **Actions** tab on your GitHub repository page.
3.  Click on the latest run of the **Android CI/CD** workflow.
4.  Scroll down to the **Artifacts** section at the bottom of the page.
5.  Download the **`WebAICopilot-Debug-APK`** zip containing your standalone runnable debug app package (`app-debug.apk`)!

The workflow automatically decodes the `debug.keystore.base64` checked into the project root to guarantee that the debug compilation compiles perfectly and matches existing signatures.

---

## 🔑 AI Assistant API Configuration

To enable the responsive AI-powered refactoring assistant:
1.  Obtain an API Key from [Google AI Studio](https://aistudio.google.com/).
2.  Add your API Key to the **Secrets panel in Google AI Studio** as `GEMINI_API_KEY`.
3.  Once declared, WebAI Copilot automatically injects this secret dynamically at compile time—guaranteeing that your API key is never hardcoded or exposed in your GitHub repositories.

---

## 🏗️ Local Compilation & Development

If you prefer compiling development builds locally, ensure you have **Java JDK 17** active:

### Running Unit & Robolectric Tests:
```bash
gradle test
```

### Compiling and generating a Debug APK:
```bash
gradle assembleDebug
```
The resulting APK will be saved at:
`app/build/outputs/apk/debug/app-debug.apk`
