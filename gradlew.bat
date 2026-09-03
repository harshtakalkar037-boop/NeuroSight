@rem Standard Gradle wrapper launcher script (Windows).
@rem If Android Studio prompts to regenerate/repair the wrapper on first sync,
@rem accept -- this project intentionally ships without the wrapper jar binary.

@echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

"%JAVA_HOME%\bin\java.exe" -Xmx64m -Xms64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
