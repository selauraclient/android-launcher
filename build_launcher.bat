@echo off
call gradlew :minecraft:createFullJarRelease
call d8 "./minecraft/build/intermediates/full_jar/release/createFullJarRelease/full.jar"
move "./classes.dex" "./app/src/main/assets/launcher.dex"