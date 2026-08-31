# demise
A Minecraft client based around bypassing strict anticheats.
I am scared if github removed accounts if i distribute the binary, i will provide it on the site. https://colorwall.xyz
### how to install?
1) download the .jar from releases that match your os, and rename it to `demise` (keep the file extension the same plz)
2) download the .json from `/json/demise.json`
3) create a new folder in minecraft's version dir named `demise`, and put both of the files in there
4) go on the vanilla mc launcher (sk, tl and others work too) and create a new installation using the `demise` version
   <br> <b> make sure to use java 17 </b>

### how to build?
1) Make sure you have **Java 17 / JDK 17** installed (or point Gradle to your local JDK runtime).
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
