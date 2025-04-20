@echo on
echo Building jar
call gradlew :minecraft:createFullJarRelease
echo Sanitize jar
tar --extract --file=./minecraft/build/intermediates/full_jar/release/createFullJarRelease/full.jar --directory=temp_jar && find temp_jar -type f -name "*.class" ! -name "NotificationListenerService.class" ! -name "Launcher.class" ! -name "GooglePlayStore.class" ! -name "AmazonAppStore.class" -delete && tar --create --file=./modified_full.tar -C temp_jar . && rm -rf temp_jar
REM Ensure temp_jar directory is created
if not exist temp_jar (
    mkdir temp_jar
)
echo Extract the JAR file 
tar -xvf .\minecraft\build\intermediates\full_jar\release\createFullJarRelease\full.jar -C temp_jar
REM Delete unwanted .class files, preserving specific files
for /R temp_jar %%F in (*.class) do (
    if /I not "%%~nxF"=="NotificationListenerService.class" (
        if /I not "%%~nxF"=="Launcher.class" (
            if /I not "%%~nxF"=="GooglePlayStore.class" (
                if /I not "%%~nxF"=="AmazonAppStore.class" (
                    del "%%F"
                )
            )
        )
    )
)
echo Recreate the JAR archive
cd temp_jar
jar -cfM ..\classes.jar *
cd ..
echo Clean up the temp_jar folder
rmdir /S /Q temp_jar
echo Converting to dex
call d8 "classes.jar"
echo Copying
copy "./classes.dex" "./app/src/main/assets/launcher.dex"
echo Cleanup
del modified_full.jar classes.jar classes.dex
