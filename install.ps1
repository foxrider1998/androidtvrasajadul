Write-Host "Menghubungkan ke Android TV..." -ForegroundColor Cyan
adb connect 172.16.5.210:5555

Write-Host "Memasang APK ke TV..." -ForegroundColor Cyan
adb -s 172.16.5.210:5555 install -r app\build\outputs\apk\debug\app-debug.apk

Write-Host "Membuka TV Rasa Jadul..." -ForegroundColor Green
adb -s 172.16.5.210:5555 shell monkey -p com.example.androidtvrasajadul -c android.intent.category.LAUNCHER 1
