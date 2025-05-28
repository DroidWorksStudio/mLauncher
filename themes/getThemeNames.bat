@echo off
REM Echo a blank line
echo.

REM Loop through all .mtheme files in the current directory
for %%f in (*.mtheme) do (
    REM Output the file in the desired format
    echo     - [Download %%~nf Theme](%%f^)
)

REM Echo a blank line
echo.

pause
