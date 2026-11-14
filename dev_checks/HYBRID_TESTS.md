# Advanced Hybrid Breeding developer checks

Pure Java smoke test (works without Minecraft dependencies from the repository root):

```
mkdir -p build/hybrid-smoke
javac -d build/hybrid-smoke \
  src/main/java/com/agrievolution/genetics/CropGenome.java \
  src/main/java/com/agrievolution/genetics/CropSpecies.java \
  src/main/java/com/agrievolution/genetics/GenomeBreeder.java \
  src/main/java/com/agrievolution/genetics/AdvancedHybridBreeder.java \
  dev_checks/HybridSmoke.java
java -cp build/hybrid-smoke HybridSmoke
```

JSON and registration reference sanity check:

```
python dev_checks/hybrid_static_check.py
```

Use the Gradle wrapper to build against NeoForge for Minecraft 1.21.1. To run the gameplay tests on Windows, use `.\gradlew.bat -PgameplayTests runGameplayTests`.
