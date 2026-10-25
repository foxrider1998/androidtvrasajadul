@rem Gradle Wrapper untuk project androidtvrasajadul
@rem Menggunakan Gradle 8.13 dari cache lokal

@echo off
setlocal

set GRADLE_HOME=C:\Users\arisr\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13
set PATH=%GRADLE_HOME%\bin;%PATH%

gradle %*

endlocal
