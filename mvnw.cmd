@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "WRAPPER_JAR=%PROJECT_DIR%.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
  echo ERROR: Maven Wrapper JAR not found: "%WRAPPER_JAR%"
  exit /b 1
)

java "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%." -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
