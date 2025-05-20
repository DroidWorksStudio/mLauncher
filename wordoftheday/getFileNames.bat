@echo off
REM Echo a blank line
echo.

REM Loop through all .json files in the current directory
for %%f in (*.json) do (
    if exist "%%f" (
        set "filename=%%~nf"
        echo     - [Download %%~nf WOTD](%%f^)
    )
)

REM Echo a blank line
echo.

pause
