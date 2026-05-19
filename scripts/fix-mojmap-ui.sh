#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/\.drawCenteredTextWithShadow(/.drawCenteredString(/g' \
      -e 's/\.drawTextWithShadow(/.drawString(/g' \
      -e 's/\.drawHorizontalLine(/.hLine(/g' \
      -e 's/\.drawVerticalLine(/.vLine(/g' \
      -e 's/\.fontHeight/.lineHeight/g' \
      -e 's/\.getWidth(/.width(/g' \
      -e 's/\.trimToWidth(/.plainSubstrByWidth(/g' \
      -e 's/Util\.getMeasuringTimeMs()/Util.getMillis()/g' \
      -e 's/getWindow()\.getHandle()/getWindow().handle()/g' \
      -e 's/\.getMatrices()/.pose()/g' \
      -e 's/AbstractButton\.builder(/Button.builder(/g' \
      -e 's/addDrawableChild(/addRenderableWidget(/g' \
      -e 's/import net\.minecraft\.client\.gui\.components\.AbstractButton;/import net.minecraft.client.gui.components.Button;\nimport net.minecraft.client.gui.components.AbstractButton;/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done

# Screen.minecraft field (not every this.client)
find src/main/java/com/pathmind/screen -name '*.java' -print0 | xargs -0 sed -i 's/this\.client/this.minecraft/g'
find src/main/java/com/pathmind/screen -name '*.java' -print0 | xargs -0 sed -i 's/Minecraft client = this\.minecraft/Minecraft client = this.minecraft/g'
