@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot

echo [1/3] Czyszczenie starej kompilacji...
if exist bin rmdir /s /q bin

echo [2/2] Kompilacja...
mkdir bin
"%JAVA_HOME%\bin\javac" -d bin -cp "src" src/com/raycasting/*.java

echo [2b/3] Kopiowanie zasobów...
xcopy "src\com\raycasting\textures" "bin\com\raycasting\textures\" /E /I /Y
xcopy "src\com\raycasting\sprites" "bin\com\raycasting\sprites\" /E /I /Y

echo [3/3] Uruchamianie...
"%JAVA_HOME%\bin\java" -Xmx2G -cp "bin" com.raycasting.Main
pause