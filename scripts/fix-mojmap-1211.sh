#!/usr/bin/env bash
# Targeted Yarn -> Mojang 1.21.1 renames (avoid overly broad patterns).
set -euo pipefail
DIRS=(src/main/java src/compat/legacy/base/java src/compat/legacy/useitem/typed/java src/compat/legacy/render/old/java)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/\.renderDistance()\.getValue()/.renderDistance().get()/g' \
      -e 's/\.getServerRenderDistance()\.getValue()/.getServerRenderDistance().get()/g' \
      -e 's/\.getFov()\.getValue()/.fov().get()/g' \
      -e 's/client\.options\.getFov()/client.options.fov()/g' \
      -e 's/\.getHungerManager()/.getFoodData()/g' \
      -e 's/\.getInventory()\.count(/.getInventory().countItem(/g' \
      -e 's/\.getMainHandStack()/.getMainHandItem()/g' \
      -e 's/\.getOffHandStack()/.getOffhandItem()/g' \
      -e 's/\.canSee(/.hasLineOfSight(/g' \
      -e 's/\.getFramebufferWidth()/.getWidth()/g' \
      -e 's/\.getFramebufferHeight()/.getHeight()/g' \
      -e 's/\.isSubmergedInWater()/.isUnderWater()/g' \
      -e 's/\.isClimbing()/.onClimbable()/g' \
      -e 's/\.getVelocity()/.getDeltaMovement()/g' \
      -e 's/\.isSneaking()/.isCrouching()/g' \
      -e 's/\.getStackInHand(/.getItemInHand(/g' \
      -e 's/\.swingHand(/.swing(/g' \
      -e 's/\.sendPacket(/.send(/g' \
      -e 's/\.getCursorStack()/.getCarried()/g' \
      -e 's/\.hasStack()/.hasItem()/g' \
      -e 's/\.isAccepted()/.consumesAction()/g' \
      -e 's/\.getStateManager()/.getStateDefinition()/g' \
      -e 's/state\.get(property)/state.getValue(property)/g' \
      -e 's/state\.getEntries()/state.getValues()/g' \
      -e 's/\.getCamera()/.getMainCamera()/g' \
      -e 's/Direction\.getFacing(/Direction.getNearest(/g' \
      -e 's/\.calcBlockBreakingDelta(/.getDestroyProgress(/g' \
      -e 's/Ingredient\.ofItems(/Ingredient.of(/g' \
      -e 's/property\.parse(/property.getValue(/g' \
      -e 's/rawProperty\.name(value)/rawProperty.getName(value)/g' \
      -e 's/blockHit\.blockPosition()/blockHit.getBlockPos()/g' \
      -e 's/blockHit\.getPos()/blockHit.getLocation()/g' \
      -e 's/hit\.blockPosition()/hit.getBlockPos()/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
