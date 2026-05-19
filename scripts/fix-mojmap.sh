#!/usr/bin/env bash
set -euo pipefail
DIRS=(src/main/java src/compat/legacy)
for d in "${DIRS[@]}"; do
  [ -d "$d" ] || continue
  while IFS= read -r -d '' f; do
    sed -i \
      -e 's/net\.minecraft\.client\.render/net.minecraft.client.renderer/g' \
      -e 's/net\.minecraft\.client\.util\.Window/com.mojang.blaze3d.platform.Window/g' \
      -e 's/net\.minecraft\.client\.Mouse/net.minecraft.client.MouseHandler/g' \
      -e 's/net\.minecraft\.client\.resource\.language/net.minecraft.client.resources.language/g' \
      -e 's/net\.minecraft\.client\.gui\.tooltip/net.minecraft.client.gui.components/g' \
      -e 's/net\.minecraft\.core\.ResourceKey/net.minecraft.resources.ResourceKey/g' \
      -e 's/net\.minecraft\.core\.ResourceKeys/net.minecraft.resources.ResourceKey/g' \
      -e 's/net\.minecraft\.core\.random\.Random/net.minecraft.util.RandomSource/g' \
      -e 's/net\.minecraft\.world\.entity\.passive\.Villager/net.minecraft.world.entity.npc.Villager/g' \
      -e 's/net\.minecraft\.world\.entity\.SpawnReason/net.minecraft.world.entity.MobSpawnType/g' \
      -e 's/net\.minecraft\.util\.DyeColor/net.minecraft.world.item.DyeColor/g' \
      -e 's/net\.minecraft\.network\.ClientConnection/net.minecraft.network.Connection/g' \
      -e 's/net\.minecraft\.client\.player\.PlayerListEntry/net.minecraft.client.multiplayer.PlayerInfo/g' \
      -e 's/com\.mojang\.blaze3d\.platform\.DynamicTexture/net.minecraft.client.renderer.texture.DynamicTexture/g' \
      -e 's/com\.mojang\.blaze3d\.vertex\.VertexConsumerProvider/net.minecraft.client.renderer.MultiBufferSource/g' \
      -e 's/VertexConsumerProvider/MultiBufferSource/g' \
      -e 's/net\.minecraft\.network\.chat\.OrderedText/net.minecraft.util.FormattedCharSequence/g' \
      -e 's/OrderedText/FormattedCharSequence/g' \
      -e 's/net\.minecraft\.client\.gui\.Element/net.minecraft.client.gui.components.events.GuiEventListener/g' \
      -e 's/net\.minecraft\.gui\.Drawable/net.minecraft.client.gui.components.Renderable/g' \
      -e 's/net\.minecraft\.client\.gui\.Drawable/net.minecraft.client.gui.components.Renderable/g' \
      -e 's/net\.minecraft\.client\.gui\.Selectable/net.minecraft.client.gui.narration.NarratableEntry/g' \
      -e 's/<Element & Drawable & Selectable>/<GuiEventListener \& Renderable \& NarratableEntry/g' \
      -e 's/extends Element & Drawable & Selectable/extends GuiEventListener, Renderable, NarratableEntry/g' \
      -e 's/RegistryWrapper\.Impl/RegistryLookup.RegistryLookup/g' \
      -e 's/RegistryWrapper/HolderLookup/g' \
      -e 's/MerchantOffers\.Factory/VillagerTrades.ItemListing/g' \
      -e 's/\bRegistries\./BuiltInRegistries./g' \
      -e 's/import net\.minecraft\.registry\.Registries/import net.minecraft.core.registries.BuiltInRegistries/g' \
      -e 's/\.getYaw()/.getYRot()/g' \
      -e 's/\.getPitch()/.getXRot()/g' \
      -e 's/\.getEyePos()/.getEyePosition(1.0f)/g' \
      -e 's/\.lengthSquared()/.lengthSqr()/g' \
      -e 's/\.squaredDistanceTo(/.distanceToSqr(/g' \
      -e 's/\.expand(/.inflate(/g' \
      -e 's/getOtherEntities(/getEntities(/g' \
      -e 's/\.getOffsetX()/.getStepX()/g' \
      -e 's/\.getOffsetY()/.getStepY()/g' \
      -e 's/\.getOffsetZ()/.getStepZ()/g' \
      -e 's/Inventory\.MAIN_SIZE/Inventory.INVENTORY_SIZE/g' \
      -e 's/ServerboundPlayerCommandPacket\.Mode/ServerboundPlayerCommandPacket.Action/g' \
      -e 's/net\.minecraft\.world\.level\.block\.state\.properties\.Properties;/net.minecraft.world.level.block.state.properties.BlockStateProperties;/g' \
      -e 's/state\.contains(Properties\./state.hasProperty(BlockStateProperties./g' \
      -e 's/state\.get(Properties\./state.getValue(BlockStateProperties./g' \
      -e 's/AbstractButton\.PressAction/Button.OnPress/g' \
      -e 's/PressAction pressAction/Button.OnPress onPress/g' \
      -e 's/Tooltip\.of(/Tooltip.create(/g' \
      -e 's/LocalPlayerNetworkEvent/ClientPlayerNetworkEvent/g' \
      -e 's/FMLClientStoppingEvent/ClientStoppedRunningEvent/g' \
      -e 's/net\.neoforged\.fml\.event\.lifecycle\.ClientStoppedRunningEvent/net.neoforged.neoforge.client.event.ClientStoppedRunningEvent/g' \
      -e 's/\.sendMessage(\([^,]*\), false)/.displayClientMessage(\1, false)/g' \
      "$f"
  done < <(find "$d" -name '*.java' -print0)
done
