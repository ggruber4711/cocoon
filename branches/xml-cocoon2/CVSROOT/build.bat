@echo off
:: -----------------------------------------------------------------------------
:: build.bat - Win32 Build Script for Apache Cocoon
::
:: $Id: build.bat,v 1.10.2.24 2001-04-13 17:39:50 dims Exp $
:: -----------------------------------------------------------------------------

:: ----- Verify and Set Required Environment Variables -------------------------

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto cleanup
:gotJavaHome

if not "%ANT_HOME%" == "" goto gotAntHome
set ANT_HOME=.
:gotAntHome

:: ----- Set Up The Runtime Classpath ------------------------------------------

set CP=%JAVA_HOME%\lib\tools.jar;%ANT_HOME%\lib\ant_1_3.jar;%ANT_HOME%\lib\ant_1_3-optional.jar;.\lib\xerces_1_3_0.jar;.\lib\xalan-2.0.1.jar

:: ----- Execute The Requested Build -------------------------------------------

%JAVA_HOME%\bin\java.exe %ANT_OPTS% -classpath %CP% org.apache.tools.ant.Main -Dant.home=%ANT_HOME% %1 %2 %3 %4 %5 %6 %7 %8 %9

:: ----- Cleanup the environment -----------------------------------------------

:cleanup
set CP=


