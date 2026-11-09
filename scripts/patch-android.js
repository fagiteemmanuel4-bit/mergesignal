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

const resDir = path.join('android', 'app', 'src', 'main', 'res');
const ensureDir = name => fs.mkdirSync(path.join(resDir, name), { recursive: true });
ensureDir('drawable'); ensureDir('values'); ensureDir('mipmap-anydpi-v26');
fs.writeFileSync(path.join(resDir, 'drawable', 'splash_logo.xml'), '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="112dp" android:height="112dp" android:viewportWidth="112" android:viewportHeight="112"><path android:fillColor="#FFFFFFFF" android:pathData="M56,10 L66,43 L99,56 L66,69 L56,102 L46,69 L13,56 L46,43 Z"/><path android:fillColor="#FFFFD7C6" android:pathData="M88,5 L92,17 L104,21 L92,25 L88,37 L84,25 L72,21 L84,17 Z"/></vector>');
fs.writeFileSync(path.join(resDir, 'drawable', 'splash_screen.xml'), '<layer-list xmlns:android="http://schemas.android.com/apk/res/android"><item><shape android:shape="rectangle"><gradient android:angle="135" android:startColor="#171614" android:centerColor="#24201D" android:endColor="#171614" android:type="linear"/></shape></item><item android:drawable="@drawable/splash_logo" android:gravity="center"/></layer-list>');
fs.writeFileSync(path.join(resDir, 'drawable', 'ic_launcher_background.xml'), '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#C96442"/></shape>');
fs.writeFileSync(path.join(resDir, 'drawable', 'ic_launcher_foreground.xml'), '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><group android:translateX="10" android:translateY="10"><path android:fillColor="#FFFFFFFF" android:pathData="M44,8 L54,36 L82,46 L54,56 L44,84 L34,56 L6,46 L34,36 Z"/><path android:fillColor="#FFFFD7C6" android:pathData="M77,3 L81,14 L92,18 L81,22 L77,33 L73,22 L62,18 L73,14 Z"/></group></vector>');
const adaptiveIcon = '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@drawable/ic_launcher_background"/><foreground android:drawable="@drawable/ic_launcher_foreground"/></adaptive-icon>';
fs.writeFileSync(path.join(resDir, 'mipmap-anydpi-v26', 'ic_launcher.xml'), adaptiveIcon);
fs.writeFileSync(path.join(resDir, 'mipmap-anydpi-v26', 'ic_launcher_round.xml'), adaptiveIcon);
const styleFile = path.join(resDir, 'values', 'styles.xml');
if (fs.existsSync(styleFile)) {
 let styles = fs.readFileSync(styleFile, 'utf8');
 const launch = /<style name="AppTheme\.NoActionBarLaunch"[^>]*>[\s\S]*?<\/style>/;
 const base = /<style name="AppTheme\.NoActionBar"[^>]*>[\s\S]*?<\/style>/;
 const addBg = block => block.includes('android:windowBackground') ? block : block.replace('</style>', '  <item name="android:windowBackground">@drawable/splash_screen</item>\n</style>');
 if (launch.test(styles)) styles = styles.replace(launch, addBg);
 else if (base.test(styles)) styles = styles.replace(base, addBg);
 fs.writeFileSync(styleFile, styles);
}
console.log('Installed Prompt Notes adaptive launcher icon and native splash screen.');
