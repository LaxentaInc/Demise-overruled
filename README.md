# Demise OverRuled.
- A Modern Minecraft utility client based around bypassing annoying anticheats. 
- The code is opensource under AGPL-3, and releases are Automated (if any present).

I am not going to host cheat clients on github and risk my account.
Get it on: https://colorwall.xyz/api/download/demise
### How to install?
[demise.json](https://github.com/LaxentaInc/Demise-overruled/releases/download/v2.1.1/demise.json) 
Let me paste screenshots, so its IMMIDIATELY CLEAR HOW TO USE THE JAR + JSON FILE AND WHERE TO PUT THEM:


## DO " `CTRL + R` "  and PASTE " `%appdata%/.minecraft/versions` "
<img width="436" height="229" alt="image" src="https://github.com/user-attachments/assets/e77e30ca-1e4a-4623-8b97-bb7f5858eba4" />

## THEN CREATE A FOLDER NAMED DEMISE
<img width="555" height="470" alt="image" src="https://github.com/user-attachments/assets/eb3ae3d6-2468-4374-9421-3bb790d344b8" />


<img width="1446" height="896" alt="image" src="https://github.com/user-attachments/assets/d74c60ad-5222-4ece-a407-e021b43d3763" />

- AND THATS IT, PUT `demise.json` + `demise.jar` INSIDE THAT FOLDER AS SHOWN IN THE ABOVE SCREENSHOT.

- Done. Enjoy! Make sure to report ANY bugs, in issues please. Or anything you want.

### How to build?
1) Make sure you have **Java 17 / JDK 17** installed.
2) Clone the repository and navigate to the project directory.
3) Run the Gradle `shadowJar` task to compile the client:

```powershell
.\gradlew shadowJar '-Dorg.gradle.java.home=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr'
```

*(Note: Adjust `-Dorg.gradle.java.home` to point to your local JDK 17 directory if needed).*

4) The output `.jar` will be created at:
   `build/libs/demise-1.8.9-all.jar`

5) **Automated Build & Deploy Command (PowerShell)**:
   You can run this PowerShell script to automatically build and copy the compiled `.jar` straight into your `.minecraft` versions folder:

```powershell
.\gradlew shadowJar '-Dorg.gradle.java.home=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr'
if ($LASTEXITCODE -eq 0) {
    $versionDir = "$env:APPDATA\.minecraft\versions\demise"
    Copy-Item -Path "build\libs\demise-1.8.9-all.jar" -Destination "$versionDir\demise.jar" -Force
    Write-Output "Successfully compiled and copied to .minecraft"
} else {
    Write-Output "Build failed."
}
```

### Quick launch & developer run (instant execution)
Instead of building the full JAR and copying it to `.minecraft`, you can launch Demise **directly from the terminal in 2 seconds**:

```powershell
.\gradlew runClient '-Dorg.gradle.java.home=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr'
.\gradlew runClient '-Dorg.gradle.java.home=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr'
```
*When you make code changes, Gradle will only recompile the modified file incrementally and boot the client immediately!*

### The client is in early development, feel free to report any bugs in the issues or on the discord server or on https://colorwall.xyz/feedback



We created 

.settings/org.eclipse.jdt.core.prefs:

```properties
eclipse.preferences.version=1
org.eclipse.jdt.core.compiler.codegen.targetPlatform=17
org.eclipse.jdt.core.compiler.compliance=17
org.eclipse.jdt.core.compiler.source=17```
```
This forces the IDE's Language Server to check your code against Java 17 compliance, clearing those 3,000+ false error highlights. (If any files still show red in the editor, pressing Ctrl+Shift+P -> Java: Clean Java Language Server Workspace or restarting the IDE will force a clean re-index).