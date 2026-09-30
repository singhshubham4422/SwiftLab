# SwiftLab Windows Desktop Application Build & Packaging Guide

## 1. Prerequisites

To build the native Windows `.exe` and Windows installer from source:
1. **JDK 21+:** With `jpackage` included in PATH (e.g. Microsoft Build of OpenJDK 21 or Eclipse Temurin 21).
2. **WiX Toolset v3.11+:** Included in repository under `./wix-bin` or installed system-wide.
3. **Apache Maven 3.9+:** Bundled under `./apache-maven-3.9.6` or system PATH.

---

## 2. Windows Script Reference

- `run.bat`: Launches the compiled Spring Boot application using `javaw` (no console window) and opens the default web browser to `http://localhost:8080/`.
- `package.bat`: Builds the production JAR with Maven, links the WiX toolset, and executes `jpackage` to produce both a portable app folder and a single-file Windows installer.
- `clean.bat`: Cleans up `target/` and `dist/` directories.

---

## 3. Building the Windows Binaries

Run the automated packaging batch script from PowerShell or Command Prompt:
```cmd
.\package.bat
```

### Packaging Outputs:
1. **Portable Application Directory:**
   ```
   dist\InvoiceApp\
     ├── InvoiceApp.exe   <-- Standalone native Windows executable
     ├── runtime\         <-- Bundled minimal Java 21 runtime
     └── app\             <-- Spring Boot application JARs
   ```
2. **Windows Single-File Installer:**
   ```
   InvoiceApp-Installer.exe
   ```

---

## 4. Desktop Client Architecture & H2 Persistence

- When launched via `InvoiceApp.exe`, the application starts the embedded Spring Boot engine on an internal port and initializes the local H2 file database at `./data/invoicedb.mv.db`.
- The database persists all records permanently across system reboots, user logouts, and application restarts.
- The desktop application operates completely offline with zero dependency on internet connectivity.
