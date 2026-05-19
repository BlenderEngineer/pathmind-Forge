#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy/base/java src/compat/legacy/useitem/typed/java src/compat/legacy/render/old/java)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/ButtonClickC2SPacket/ServerboundContainerButtonClickPacket/g' \
      -e 's/CloseHandledScreenC2SPacket/ServerboundContainerClosePacket/g' \
      -e 's/SelectMerchantTradeC2SPacket/ServerboundSelectTradePacket/g' \
      -e 's/ItemStack\.areItemsAndComponentsEqual(/ItemStack.isSameItemSameComponents(/g' \
      -e 's/ItemStack\.areItemsEqual(/ItemStack.isSameItem(/g' \
      -e 's/\.attackEntity(/.attack(/g' \
      -e 's/\.closeHandledScreen()/.closeContainer()/g' \
      -e 's/\.canInsert(/.mayPlace(/g' \
      -e 's/\.dropSelectedItem(/.drop(/g' \
      -e 's/\.equipStack(/.setItemSlot(/g' \
      -e 's/\.getEquippedStack(/.getItemBySlot(/g' \
      -e 's/getDisplayedFirstBuyItem()/getCostA()/g' \
      -e 's/getDisplayedSecondBuyItem()/getCostB()/g' \
      -e 's/getFirstBuyItem()/getCostA()/g' \
      -e 's/getSecondBuyItem()/getCostB()/g' \
      -e 's/getSellItem()/getResult()/g' \
      -e 's/\.getMaxCount()/.getMaxStackSize()/g' \
      -e 's/getMaxItemCount(/getMaxStackSize(/g' \
      -e 's/\.down()/.below()/g' \
      -e 's/\.distanceTo(/.distManhattan(/g' \
      -e 's/\.distSqr(/.distanceToSqr(/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
