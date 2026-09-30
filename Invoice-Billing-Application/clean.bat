@echo off
echo Cleaning build output directories...
if exist target rd /s /q target
if exist dist rd /s /q dist
echo Cleanup complete.
