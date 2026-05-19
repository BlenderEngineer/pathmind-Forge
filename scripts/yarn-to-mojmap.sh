#!/usr/bin/env bash
# Bulk-replace common Yarn (Fabric) names with Mojang/Parchment names for NeoForge 1.21.1.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIRS=(
  "$ROOT/src/main/java"
  "$ROOT/src/compat/legacy/base/java"
  "$ROOT/src/compat/legacy/useitem/typed/java"
  "$ROOT/src/compat/legacy/render/old/java"
)

replace_all() {
  local from="$1" to="$2"
  find "${DIRS[@]}" -name '*.java' -print0 | xargs -0 sed -i "s/${from}/${to}/g"
}

# Imports / types
replace_all 'net\.minecraft\.client\.MinecraftClient' 'net.minecraft.client.Minecraft'
replace_all 'MinecraftClient' 'Minecraft'
replace_all 'net\.minecraft\.client\.gui\.DrawContext' 'net.minecraft.client.gui.GuiGraphics'
replace_all 'DrawContext' 'GuiGraphics'
replace_all 'net\.minecraft\.util\.Identifier' 'net.minecraft.resources.ResourceLocation'
replace_all 'Identifier' 'ResourceLocation'
replace_all 'net\.minecraft\.client\.option\.KeyBinding' 'net.minecraft.client.KeyMapping'
replace_all 'KeyBinding' 'KeyMapping'
replace_all 'net\.minecraft\.client\.font\.TextRenderer' 'net.minecraft.client.gui.Font'
replace_all 'TextRenderer' 'Font'
replace_all 'net\.minecraft\.util\.ActionResult' 'net.minecraft.world.InteractionResult'
replace_all 'ActionResult' 'InteractionResult'
replace_all 'net\.minecraft\.util\.TypedActionResult' 'net.minecraft.world.InteractionResultHolder'
replace_all 'TypedActionResult' 'InteractionResultHolder'
replace_all 'net\.minecraft\.client\.world\.ClientWorld' 'net.minecraft.client.multiplayer.ClientLevel'
replace_all 'ClientWorld' 'ClientLevel'
replace_all 'net\.minecraft\.world\.World' 'net.minecraft.world.level.Level'
replace_all 'net\.minecraft\.entity\.player\.PlayerEntity' 'net.minecraft.world.entity.player.Player'
replace_all 'PlayerEntity' 'Player'
replace_all 'net\.minecraft\.client\.network\.ClientPlayerEntity' 'net.minecraft.client.player.LocalPlayer'
replace_all 'ClientPlayerEntity' 'LocalPlayer'
replace_all 'net\.minecraft\.registry\.Registries' 'net.minecraft.core.registries.BuiltInRegistries'
replace_all 'net\.minecraft\.text\.Text' 'net.minecraft.network.chat.Component'
replace_all 'net\.minecraft\.client\.util\.InputUtil' 'com.mojang.blaze3d.platform.InputConstants'
replace_all 'InputUtil' 'InputConstants'
replace_all 'net\.minecraft\.util\.math\.BlockPos' 'net.minecraft.core.BlockPos'
replace_all 'net\.minecraft\.util\.math\.Box' 'net.minecraft.world.phys.AABB'
replace_all '\bBox\b' 'AABB'
replace_all 'net\.minecraft\.block\.BlockState' 'net.minecraft.world.level.block.state.BlockState'
replace_all 'net\.minecraft\.item\.Item' 'net.minecraft.world.item.Item'
replace_all 'net\.minecraft\.entity\.ItemEntity' 'net.minecraft.world.entity.item.ItemEntity'
replace_all 'net\.minecraft\.client\.gui\.screen\.Screen' 'net.minecraft.client.gui.screens.Screen'
replace_all 'net\.minecraft\.client\.gui\.screen\.TitleScreen' 'net.minecraft.client.gui.screens.TitleScreen'
replace_all 'net\.minecraft\.client\.gui\.screen\.ChatScreen' 'net.minecraft.client.gui.screens.ChatScreen'
replace_all 'net\.minecraft\.client\.gui\.screen\.ingame\.MerchantScreen' 'net.minecraft.client.gui.screens.inventory.MerchantScreen'
replace_all 'net\.minecraft\.client\.gui\.hud\.InGameHud' 'net.minecraft.client.gui.Gui'
replace_all 'InGameHud' 'Gui'
replace_all 'net\.minecraft\.client\.render\.RenderTickCounter' 'net.minecraft.client.DeltaTracker'
replace_all 'RenderTickCounter' 'DeltaTracker'
replace_all 'ResourceLocation\.of' 'ResourceLocation.fromNamespaceAndPath'
replace_all '\.wasPressed()' '.consumeClick()'
replace_all '\.isPressed()' '.isDown()'
replace_all 'InteractionResult\.PASS' 'InteractionResult.PASS'
replace_all 'InteractionResultHolder\.pass' 'InteractionResultHolder.pass'
replace_all 'getString()' 'getString()'

echo "Remap pass complete."
