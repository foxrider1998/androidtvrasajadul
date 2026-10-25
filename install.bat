@echo off
echo ========================================================
echo   Memasang TV Rasa Jadul ke Android TV
echo ========================================================
echo.
echo Menghubungkan ke 172.16.5.210:5555...
adb connect 172.16.5.210:5555
echo.
echo Memasang APK...
adb -s 172.16.5.210:5555 install -r app\build\outputs\apk\debug\app-debug.apk
echo.
echo Membuka aplikasi di TV...
adb -s 172.16.5.210:5555 shell monkey -p com.example.androidtvrasajadul -c android.intent.category.LAUNCHER 1
echo.
echo Selesai! TV Rasa Jadul aktif.
pause
