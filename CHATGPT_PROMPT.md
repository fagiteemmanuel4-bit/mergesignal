Hi! I'm attaching a zip with a complete Android-ready project called "Prompt Notes". Please do this for me end to end:

1. My GitHub repo is: [PASTE REPO URL HERE]. It already contains an older, similar project. I want it fully replaced by the contents of the zip.
   - First create a backup branch called `backup-before-replace` from the current main branch and push it.
   - Then on main: delete every existing file, extract the zip so its files sit at the repo ROOT (not inside a subfolder), and include the hidden `.github/workflows/android.yml`.
   - Commit as "Replace project with Prompt Notes Android build" and push to main.
2. Make sure GitHub Actions is enabled. The workflow "Build Android APK" should run and create a Release with `PromptNotes.apk`. If the build fails, read the logs, fix only what is needed to make it build, push again, and repeat until it is green.
3. Send me the direct download link to the APK: https://github.com/OWNER/REPO/releases/latest/download/PromptNotes.apk (fill in the real owner/repo), plus the link to the release page. If the repo is private, tell me, because the link will then need a GitHub login. Do not change the repo's visibility without asking me.
4. Do NOT put any secret keys in the repo. Do not change app features unless a build fix requires it. The AI works in the build by letting me type my own endpoint and key in the app's Settings, and that is fine for now.
5. Be honest: if you cannot push to GitHub or run builds with the tools you have, say so clearly and give me exact step-by-step commands instead of pretending it worked. Finish with a short report of what you did and anything you could not do.

Only after the APK is working, and only if I ask, continue with the improvements listed in CHATGPT_TASK.md.
