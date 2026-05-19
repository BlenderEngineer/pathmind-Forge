#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy/base/java src/compat/legacy/useitem/typed/java src/compat/legacy/render/old/java)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/\.isInBuildLimit(/.isOutsideBuildHeight(/g' \
      -e 's/!world\.isOutsideBuildHeight(/!world.isOutsideBuildHeight(/g' \
      -e 's/state\.isReplaceable()/state.canBeReplaced()/g' \
      -e 's/\.isReplaceable()/.canBeReplaced()/g' \
      -e 's/\.isIn(\([^)]*TagKey<Block>\)/.is(\1/g' \
      -e 's/world\.isChunkLoaded(/world.hasChunk(/g' \
      -e 's/\.isStill()/.isSource()/g' \
      -e 's/\.getHardness(/.getDestroySpeed(/g' \
      -e 's/\.setVelocity(/.setDeltaMovement(/g' \
      -e 's/\.setBodyYaw(/.setYBodyRot(/g' \
      -e 's/options\.sprintKey/options.keySprint/g' \
      -e 's/\.isTouchingWater()/.isInWater()/g' \
      -e 's/\.getMiningSpeedMultiplier(/.getDestroySpeed(/g' \
      -e 's/\.isSuitableFor(/.isCorrectToolForDrops(/g' \
      -e 's/Component\.Text\.of(/Component.literal(/g' \
      -e 's/Text\.of(/Component.literal(/g' \
      -e 's/abilities\.allowFlying/abilities.mayfly/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
