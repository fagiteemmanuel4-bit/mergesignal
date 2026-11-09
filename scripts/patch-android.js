const fs = require('fs');
const path = require('path');

const manifestPath = path.join('android', 'app', 'src', 'main', 'AndroidManifest.xml');
const activityPath = path.join('android', 'app', 'src', 'main', 'java', 'com', 'axiom', 'promptnotes', 'MainActivity.java');

if (!fs.existsSync(manifestPath)) throw new Error('Generated AndroidManifest.xml was not found');
fs.mkdirSync(path.dirname(activityPath), { recursive: true });

let manifest = fs.readFileSync(manifestPath, 'utf8');
if (!manifest.includes('android.permission.RECORD_AUDIO')) {
  manifest = manifest.replace('<application', '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n    <uses-permission android:name="android.permission.VIBRATE" />\n    <uses-feature android:name="android.hardware.microphone" android:required="false" />\n    <queries>\n        <intent><action android:name="android.speech.RecognitionService" /></intent>\n    </queries>\n\n    <application');
}
fs.writeFileSync(manifestPath, manifest);
fs.copyFileSync(path.join('android-native', 'MainActivity.java'), activityPath);
console.log('Installed native Android microphone permission, speech recognition bridge, and system haptics bridge.');
