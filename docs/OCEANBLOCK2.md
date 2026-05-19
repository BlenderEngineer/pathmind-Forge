# FTB OceanBlock 2 compatibility

## Modpack loader (important)

**FTB OceanBlock 2 does not use legacy Forge.** It uses:

| Component | Version |
|-----------|---------|
| Minecraft | **1.21.1** |
| Loader | **NeoForge 21.1.228** |

Source: `mmc-pack.json` in the Shattered Prism instance  
`~/.var/app/org.Noctilune.ShatteredPrism/data/ShatteredPrism/instances/FTB OceanBlock 2/`

CurseForge/FTB often label 1.21 modpacks as “Forge” in the UI; on 1.21.1 the runtime is **NeoForge** (`neoforge-21.1.228` in `minecraft/mods` / `logs/modlist.txt`).

Pathmind’s converted build targets the same stack: **MC 1.21.1 + NeoForge 21.1.228** (`gradle.properties` → `neo_version=21.1.228`).

A **legacy Forge** (pre–NeoForge split) build would **not** load in this instance.

## Installing into the modpack

1. Finish `./gradlew build` once the port compiles cleanly.
2. Copy the release JAR from `build/libs/` (name like `pathmind-1.1.4+mc1.21.1.jar`) into:

   ```
   ~/.var/app/org.Noctilune.ShatteredPrism/data/ShatteredPrism/instances/FTB OceanBlock 2/minecraft/mods/
   ```

3. Do **not** install the Fabric JAR in this pack.

## Java for development

| JDK | Location | Use |
|-----|----------|-----|
| **OpenJDK 26** | `.jdk/` (from `openjdk-26_linux-x64_bin.tar.gz`) | Extracted; **Gradle 9.2 + ModDevGradle currently fail** with JDK 26 (`JvmVendorSpec IBM_SEMERU`). |
| **Java 21** | Shattered Prism runtime or system `jdk-openjdk` | **Recommended for `./gradlew`** — matches MC 1.21.1. |

Build example:

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew build
```

The OceanBlock 2 instance uses its **own** bundled Java 21 at launch; you do not need to point the modpack at `.jdk/`.

## Remaining port work

The NeoForge port is in progress; `./gradlew compileJava` may still report mapping/API errors.  
After `./gradlew build`, copy `build/libs/pathmind-<version>+mc1.21.1.jar` into the instance `minecraft/mods/` folder. Remove any older `pathmind-*.jar` first so only one version is present.
