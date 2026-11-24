# Running Ariadne on Windows (PowerShell)

Everything below is copy-pasteable. Run each block from the repo root
(`C:\Users\NAEHA\ariadne`).

---

## 1. Get the updated code

You have a local clone, so pull rather than re-clone - that keeps the Gradle
caches you already warmed.

```powershell
cd C:\Users\NAEHA\ariadne
git pull
```

If `git pull` complains about local changes, you have edited files locally:

```powershell
git status
git stash          # park your edits
git pull
# git stash pop    # only if you actually want them back
```

Confirm you are on the commit with the fix (`4748ff2` or later):

```powershell
git log --oneline -1
```

---

## 2. Tell Gradle which JDK to use

Your first error was `Gradle requires JVM 17 or later ... currently JVM 11`.
WebStorm's bundled JBR is fine, but point at it explicitly:

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\WebStorm 2026.1.4\jbr"
java -version
```

`java -version` must print 17 or higher. If it does not, check the path exists:

```powershell
Test-Path $env:JAVA_HOME
```

Persist it for future sessions (only affects **new** terminals):

```powershell
setx JAVA_HOME "C:\Program Files\JetBrains\WebStorm 2026.1.4\jbr"
```

---

## 3. The schema changed - regenerate before running

The migrations were restructured in `4748ff2`, so any generated code left from
the broken build must go. This is the step that makes the difference:

```powershell
.\gradlew.bat --stop
Remove-Item -Recurse -Force build\generated, core\store\build\generated -ErrorAction SilentlyContinue
.\gradlew.bat :core:store:generateCommonMainAriadneDbInterface
```

That last command is the exact task that failed for you. It must end with
`BUILD SUCCESSFUL`. **If it does not, stop there and send me the output** - the
rest will not work.

To prove the migrations themselves are sound:

```powershell
.\gradlew.bat :core:store:verifyCommonMainAriadneDbMigration
```

---

## 4. Run it

```powershell
.\gradlew.bat :app:desktop:run
```

First run compiles the Compose UI and takes a few minutes. A window opens on the
trip form.

---

## 5. If the window opens but something is still wrong

The database lives at:

```powershell
explorer $env:USERPROFILE\.ariadne
```

You should now see `ariadne.db`. Two things worth knowing:

- **The old broken file.** If a previous build left a database whose shape could
  not be migrated, the app renames it to `ariadne-unusable-<timestamp>.db`, keeps
  it, and opens on a blank form. Nothing is deleted. If you see one of those, that
  is the app telling you it found the old file and set it aside - tell me and I
  will write the salvage.
- **To start completely clean** (loses the trips in that file):

  ```powershell
  Remove-Item $env:USERPROFILE\.ariadne\ariadne.db
  ```

---

## 6. The checks that need no Gradle

These are what I run here, and they are much faster than a full build:

```powershell
bash tools/test-domain.sh      # 70 domain tests
bash tools/flow-check.sh       # 54 end-to-end state checks
bash tools/store-check.sh      # 59 checks over three real database shapes
bash tools/entry-check.sh      # all four entry points compile
bash tools/run-preview.sh --shots docs/shots
```

They need `bash` on PATH - Git for Windows provides it, or use WSL.

---

## What I could not verify

I cannot run the Gradle build end to end here the way you do: no device, no
emulator, and the Android camera has still never opened a sensor in this sandbox.
`generateCommonMainAriadneDbInterface` I *did* run through real Gradle before
pushing, and it passes. The rest is your machine's word against mine - so if step
3 or 4 fails, the output is the useful thing.
