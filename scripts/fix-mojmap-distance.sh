#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy/base/java src/compat/legacy/useitem/typed/java src/compat/legacy/render/old/java)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/mutable\.distanceToSqr(/mutable.distSqr(/g' \
      -e 's/pos\.distanceToSqr(/pos.distSqr(/g' \
      -e 's/\.distManhattan(client\.player)/.distanceToSqr(client.player)/g' \
      -e 's/\.distManhattan(playerPos)/.distanceToSqr(playerPos)/g' \
      -e 's/\.distManhattan(target)/.distanceToSqr(target)/g' \
      -e 's/\.distManhattan(player)/.distanceToSqr(player)/g' \
      -e 's/\.getStack()/.getItem()/g' \
      -e 's/\.isOnGround()/.onGround()/g' \
      -e 's/\.isDisabled()/.isOutOfStock()/g' \
      -e 's/\.getUuidAsString()/.getUUID().toString()/g' \
      -e 's/\.markDirty()/.setChanged()/g' \
      -e 's/\.setStack(/.setItem(/g' \
      -e 's/\.setStackInHand(/.setItemInHand(/g' \
      -e 's/\.sendContentUpdates()/.broadcastChanges()/g' \
      -e 's/\.getRevision()/.getStateId()/g' \
      -e 's/\.getScaledWidth()/.getGuiScaledWidth()/g' \
      -e 's/\.getScaledHeight()/.getGuiScaledHeight()/g' \
      -e 's/\.setText(/.setValue(/g' \
      -e 's/\.toImmutable()/.immutable()/g' \
      -e 's/\.up()/.above()/g' \
      -e 's/current\.add(/current.offset(/g' \
      -e 's/mutable\.add(/mutable.offset(/g' \
      -e 's/targetPos\.add(/targetPos.offset(/g' \
      -e 's/abilities\.allowFlying/abilities.mayfly/g' \
      -e 's/WritableBookContent\.DEFAULT/WritableBookContent.EMPTY/g' \
      -e 's/client\.keyboard/client.keyboardHandler/g' \
      -e 's/getUnitVec3()/getUnitVec3d()/g' \
      -e 's/getServerRenderDistance()/serverRenderDistance()/g' \
      -e 's/\.getRecipes()/.getOffers()/g' \
      -e 's/\.setRecipeIndex(/.setSelectionHint(/g' \
      -e 's/\.switchTo(/.setRecipeIndex(/g' \
      -e 's/\.sendAbilitiesUpdate()/.onUpdateAbilities()/g' \
      -e 's/KeyMapping\.onKeyPressed(/KeyMapping.click(/g' \
      -e 's/KeyMapping\.setKeyPressed(/KeyMapping.set(/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
