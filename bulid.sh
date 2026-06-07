#!/bin/bash
echo "[1/3] Czyszczenie starej kompilacji..."
rm -rf bin

echo "[2/3] Kompilacja..."
mkdir -p bin
javac -d bin -cp "src" src/com/raycasting/*.java

echo "[2b/3] Kopiowanie zasobów..."
mkdir -p bin/com/raycasting
cp -r src/com/raycasting/textures bin/com/raycasting/
cp -r src/com/raycasting/sprites bin/com/raycasting/

echo "[3/3] Uruchamianie..."
java -Xmx2G -cp "bin" com.raycasting.Main