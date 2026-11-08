# Offline AI Chat — شات بوت ذكاء اصطناعي مدمج بالكامل داخل التطبيق

تطبيق أندرويد فيه شات بوت نصي متخصص بكتابة أكواد بايثون (Qwen2.5-Coder)، يشتغل بالكامل محليًا
عبر **llama.cpp** — بدون إنترنت، وبدون إذن INTERNET بالتطبيق النهائي.

**كل من الموديل ومصدر llama.cpp بينسحبوا تلقائيًا بواسطة GitHub Actions وقت البناء** —
ما في أي خطوة يدوية مطلوبة منك قبل الرفع.

---

## 1. رفع المشروع على GitHub

```bash
cd OfflineAIChat
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/USERNAME/OfflineAIChat.git
git push -u origin main
```

لا ملف الموديل (`.gguf`) ولا مصدر llama.cpp بيترفعوا مع المستودع (مستثنيين بـ `.gitignore`) —
الـ workflow هو يلي بيجيبهم وقت كل بناء، فالمستودع يضل خفيف وسريع.

---

## 2. البناء عبر GitHub Actions (تلقائي بالكامل)

بمجرد الـ push، `.github/workflows/build.yml` بيشتغل وبيعمل بالترتيب:
1. **يسحب مصدر llama.cpp** بأمر `git clone` (نسخة ثابتة محددة بـ `LLAMA_CPP_TAG`، عشان البناء ما ينكسر
   لو تغيّرت نسخة llama.cpp الأحدث)
2. **ينزل ملف الموديل** من الرابط المحدد بـ `MODEL_URL` ويحطه بـ `app/src/main/assets/qwen-model.gguf`
3. يثبّت JDK 17 + Android SDK + NDK + CMake
4. يبني التطبيق (أول بناء ~15-20 دقيقة لأنه عم يترجم llama.cpp من المصدر، بعدها أسرع بكثير)
5. يرفع `app-debug.apk` كـ artifact جاهز للتنزيل

### الموديل الافتراضي المستخدم:
**Qwen2.5-Coder-1.5B-Instruct** (تقييس Q4_K_M، ~1 جيجا) — من حساب Qwen الرسمي على Hugging Face،
مخصص لكتابة وفهم أكواد بايثون وبرمجة عمومًا، ومناسب الحجم للموبايل.

### بدك تغيّر الموديل؟
افتح `.github/workflows/build.yml` وبدّل قيمة `MODEL_URL` برابط `resolve/main/....gguf` من أي
صفحة موديل على Hugging Face.

### بدك تحدّث نسخة llama.cpp؟
بدّل قيمة `LLAMA_CPP_TAG` بأي تاغ `bXXXX` من صفحة [releases الخاصة بـ llama.cpp](https://github.com/ggml-org/llama.cpp/releases).
لو حدّثتها، تأكد إنه البناء لسا ناجح — أسماء دوال الـ API بملف `llama-bridge.cpp` ممكن تتغيّر بين النسخ.

---

## 3. تنزيل وتثبيت APK

من تبويب **Actions** بصفحة المستودع → آخر عملية ناجحة ✅ → Artifacts → حمّل `app-debug-apk` →
فك الضغط وثبّت `app-debug.apk` على جهازك (فعّل "تثبيت من مصادر غير معروفة" أول مرة من إعدادات أندرويد).

---

## 4. البناء محليًا (اختياري)

لو بدك تجرب بـ Android Studio مباشرة بدل GitHub Actions، لازم تجيب الموديل ومصدر llama.cpp يدويًا أول مرة:

```bash
# 1. جيب مصدر llama.cpp
git clone --depth 1 --branch b4753 https://github.com/ggml-org/llama.cpp.git app/src/main/cpp/llama.cpp

# 2. جيب ملف الموديل
mkdir -p app/src/main/assets
curl -L "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf" \
  -o app/src/main/assets/qwen-model.gguf
```
بعدين افتح المشروع بـ Android Studio، خلّيه يعمل Gradle Sync، ووصل جهازك واضغط Run.

---

## هيكل المشروع

```
OfflineAIChat/
├── .github/workflows/build.yml   ← يسحب llama.cpp وينزل الموديل ويبني الـ APK تلقائيًا
├── app/
│   ├── build.gradle              ← إعدادات NDK/CMake، noCompress للـ gguf
│   └── src/main/
│       ├── AndroidManifest.xml   ← بدون إذن إنترنت (التطبيق أوفلاين بالكامل بعد البناء)
│       ├── assets/                ← هون بينحط الموديل (تلقائيًا من الـ workflow)
│       ├── cpp/
│       │   ├── CMakeLists.txt    ← بناء llama.cpp + مكتبتنا
│       │   ├── llama-bridge.cpp  ← جسر JNI (تحميل/توليد/تحرير)
│       │   └── llama.cpp/        ← مصدر llama.cpp (ينسحب تلقائيًا، مش جزء من المستودع)
│       └── java/.../
│           ├── MainActivity.kt   ← واجهة الشات + استدعاء LlamaBridge
│           ├── LlamaBridge.kt    ← صف Kotlin يستدعي دوال JNI
│           └── ChatAdapter.kt    ← عرض رسائل الشات
└── README.md
```

## ملاحظات مهمة

- **إنترنت لازم وقت البناء فقط** (سحب llama.cpp وتنزيل الموديل داخل GitHub Actions). التطبيق
  النهائي المثبت على جهازك ما بيحتاج إنترنت أبدًا.
- أول تشغيل للتطبيق بياخد كم ثانية لأنه عم ينسخ الموديل من جوا الـ APK لمساحة تخزين داخلية
  (ضروري لأن llama.cpp بحتاج مسار ملف حقيقي على القرص).
- لو جهازك قليل الذاكرة، بدّل لموديل أصغر (0.5B) بتغيير `MODEL_URL`.

## الخطوة الجاية؟

لو حابب، بقدر:
- أضيف streaming حقيقي (الرد يطلع حرف حرف).
- أضيف system prompt خاص بكتابة كود بايثون تحديدًا.
- أحسّن الواجهة (فقاعات شات ملونة، يمين/يسار حسب المرسل).
