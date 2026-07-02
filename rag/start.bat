@echo off
cd /d "%~dp0"
python -m uvicorn src.main:app --host 0.0.0.0 --port 5001 --reload
pause
