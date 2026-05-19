#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/\.setYaw(/.setYRot(/g' \
      -e 's/\.setPitch(/.setXRot(/g' \
      -e 's/\.setHeadYaw(/.setYHeadRot(/g' \
      -e 's/BlockPos\.ofFloored(/BlockPos.containing(/g' \
      -e 's/\.jump()/.jumpFromGround()/g' \
      -e 's/\.getPlacementState(/.getStateForPlacement(/g' \
      -e 's/\.canPlaceAt(/.canSurvive(/g' \
      -e 's/placementContext\.blockPosition()/placementContext.getClickedPos()/g' \
      -e 's/targetPos\.offset(/targetPos.relative(/g' \
      -e 's/Vec3\.of(/new Vec3(/g' \
      -e 's/\.multiply(\([^V][^,)]*\))/.scale(\1)/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
