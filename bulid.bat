@echo off
echo [1/3] Czyszczenie starej kompilacji...
if exist bin rmdir /s /q bin

echo [2/3] Kompilacja...
mkdir bin
javac -d bin -cp "src" src/com/raycasting/*.java

echo [3/3] Uruchamianie...
java -Xmx2G -cp "bin" com.raycasting.Main
pause