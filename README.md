# Offline AI Chat — شات بوت ذكاء اصطناعي مدمج بالكامل داخل التطبيق

تطبيق أندرويد فيه شات بوت نصي متخصص بكتابة أكواد بايثون (Qwen2.5-Coder)، يشتغل بالكامل محليًا
عبر **llama.cpp** — بدون إنترنت، وبدون إذن INTERNET بالتطبيق النهائي.

**الموديل منزّله تلقائيًا GitHub Actions وقت البناء** ومنضمّه جوا الـ APK — ما في داعي تحطه
يدويًا ولا تستخدم Git LFS.

---

## 1. جيب مصدر llama.cpp (submodule) — مرة وحدة بس

```bash
git submodule add https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp
git submodule update --init --recursive
```

(ملف `.gitmodules` موجود مسبقًا بالمشروع ومُعد لنفس هالمسار.)

---

## 2. رفع المشروع على GitHub

```bash
cd OfflineAIChat
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/USERNAME/OfflineAIChat.git
git push -u origin main
```

ملف الموديل (`.gguf`) **ما بيترفع مع المستودع** (مستثنى بـ `.gitignore`) — الـ workflow هو يلي بينزله
وقت البناء، فالمستودع يضل خفيف وسريع.

---

## 3. البناء عبر GitHub Actions (تلقائي)

بمجرد الـ push، `.github/workflows/build.yml` بيشتغل وبيعمل بالترتيب:
1. يسحب المشروع + llama.cpp submodule
2. **ينزل ملف الموديل** من الرابط المحدد بـ `MODEL_URL` ويحطه بـ `app/src/main/assets/qwen-model.gguf`
3. يثبّت JDK 17 + Android SDK + NDK + CMake
4. يبني التطبيق (أول بناء ~15-20 دقيقة لأنه عم يترجم llama.cpp من المصدر، بعدها أسرع بكثير)
5. يرفع `app-debug.apk` كـ artifact جاهز للتنزيل

### الموديل الافتراضي المستخدم:
**Qwen2.5-Coder-1.5B-Instruct** (تقييس Q4_K_M، ~1 جيجا) — من حساب Qwen الرسمي على Hugging Face،
مخصص لكتابة وفهم أكواد بايثون وبرمجة عمومًا، ومناسب الحجم للموبايل.

### بدك تغيّر الموديل؟
افتح `.github/workflows/build.yml` وبدّل قيمة `MODEL_URL` برابط `resolve/main/....gguf` من أي
صفحة موديل على Hugging Face (مثلاً نسخة أكبر زي 3B لو جهازك قوي، أو أصغر زي 0.5B لو بدك أسرع وأخف).

---

## 4. تنزيل وتثبيت APK

من تبويب **Actions** بصفحة المستودع → آخر عملية ناجحة ✅ → Artifacts → حمّل `app-debug-apk` →
فك الضغط وثبّت `app-debug.apk` على جهازك (فعّل "تثبيت من مصادر غير معروفة" أول مرة من إعدادات أندرويد).

---

## 5. البناء محليًا (اختياري)

لو بدك تجرب بـ Android Studio مباشرة بدل GitHub Actions:
```bash
mkdir -p app/src/main/assets
curl -L "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf" \
  -o app/src/main/assets/qwen-model.gguf
```
بعدين افتح المشروع بـ Android Studio، خلّيه يعمل Gradle Sync، ووصل جهازك واضغط Run.

---

## هيكل المشروع

```
OfflineAIChat/
├── .github/workflows/build.yml   ← ينزل الموديل + يبني الـ APK تلقائيًا
├── .gitmodules                   ← مرجع submodule لـ llama.cpp
├── app/
│   ├── build.gradle              ← إعدادات NDK/CMake، noCompress للـ gguf
│   └── src/main/
│       ├── AndroidManifest.xml   ← بدون إذن إنترنت (التطبيق أوفلاين بالكامل بعد البناء)
│       ├── assets/                ← هون بينحط الموديل (تلقائيًا من الـ workflow)
│       ├── cpp/
│       │   ├── CMakeLists.txt    ← بناء llama.cpp + مكتبتنا
│       │   ├── llama-bridge.cpp  ← جسر JNI (تحميل/توليد/تحرير)
│       │   └── llama.cpp/        ← مصدر llama.cpp (git submodule)
│       └── java/.../
│           ├── MainActivity.kt   ← واجهة الشات + استدعاء LlamaBridge
│           ├── LlamaBridge.kt    ← صف Kotlin يستدعي دوال JNI
│           └── ChatAdapter.kt    ← عرض رسائل الشات
└── README.md
```

## ملاحظات مهمة

- **إنترنت لازم وقت البناء فقط** (تنزيل الموديل داخل GitHub Actions). التطبيق النهائي المثبت
  على جهازك ما بيحتاج إنترنت أبدًا.
- أول تشغيل للتطبيق بياخد كم ثانية لأنه عم ينسخ الموديل من جوا الـ APK لمساحة تخزين داخلية
  (ضروري لأن llama.cpp بحتاج مسار ملف حقيقي على القرص).
- لو جهازك قليل الذاكرة، بدّل لموديل أصغر (0.5B) بتغيير `MODEL_URL`.
- أسماء دوال llama.cpp بملف `llama-bridge.cpp` تتماشى مع نسخة حديثة من المكتبة. لو صار خطأ ترجمة
  بعد تحديث الـ submodule، افتح `llama.cpp/include/llama.h` وقارن أسماء الدوال.

## الخطوة الجاية؟

لو حابب، بقدر:
- أضيف streaming حقيقي (الرد يطلع حرف حرف).
- أضيف system prompt خاص بكتابة كود بايثون تحديدًا.
- أحسّن الواجهة (فقاعات شات ملونة، يمين/يسار حسب المرسل).
