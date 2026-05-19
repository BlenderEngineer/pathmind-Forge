#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy/base/java src/compat/legacy/useitem/typed/java src/compat/legacy/render/old/java)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/options\.useKey/options.keyUse/g' \
      -e 's/options\.keySneak/options.keyShift/g' \
      -e 's/options\.forwardKey/options.keyUp/g' \
      -e 's/options\.backKey/options.keyDown/g' \
      -e 's/options\.leftKey/options.keyLeft/g' \
      -e 's/options\.rightKey/options.keyRight/g' \
      -e 's/options\.jumpKey/options.keyJump/g' \
      -e 's/options\.attackKey/options.keyAttack/g' \
      -e 's/slot\.inventory/slot.container/g' \
      -e 's/\.getIndex()/.getContainerSlot()/g' \
      -e 's/\.clickSlot(/.handleInventoryMouseClick(/g' \
      -e 's/\.attackBlock(/.startDestroyBlock(/g' \
      -e 's/\.updateBlockBreakingProgress(/.continueDestroyBlock(/g' \
      -e 's/\.interactEntity(/.interact(/g' \
      -e 's/\.interactBlock(/.useItemOn(/g' \
      -e 's/\.interactItem(/.useItem(/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
