# Demise OverRuled By @Laxenta LLC. All rights reserved.
A Minecraft client based around bypassing strict anticheats.
I am scared if github removed accounts if i distribute the binary, i will provide it on the site. https://colorwall.xyz
### how to install?
1) So first of all, download the .jar from releases (i will upload it soon, if want it instantly js dm me on discord @laxenta.me), and rename it to `demise` whilst keeping the file extension same
2) then go and download the .json from `/json/demise.json` or from Releases.
3) create a new folder in minecraft version dir named `demise`, and put both of the files in there
4) go on the your mc launcher (sk, tl idk whatever tf you use) and create a new installation using the `demise` version

### how to build?
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

### quick launch & developer run (instant execution)
Instead of building the full JAR and copying it to `.minecraft`, you can launch Demise **directly from the terminal in 2 seconds**:

```powershell
.\gradlew runClient '-Dorg.gradle.java.home=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr'
```
*When you make code changes, Gradle will only recompile the modified file incrementally and boot the client immediately!*

### the client is in early development, feel free to report any bugs in the issues or on the discord server



We created 

.settings/org.eclipse.jdt.core.prefs:

```properties
eclipse.preferences.version=1
org.eclipse.jdt.core.compiler.codegen.targetPlatform=17
org.eclipse.jdt.core.compiler.compliance=17
org.eclipse.jdt.core.compiler.source=17```

This forces the IDE's Language Server to check your code against Java 17 compliance, clearing those 3,000+ false error highlights. (If any files still show red in the editor, pressing Ctrl+Shift+P -> Java: Clean Java Language Server Workspace or restarting the IDE will force a clean re-index).