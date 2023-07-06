package io.github.betterthanupdates.apron.fixes.vanilla.compat.mixin.client.plasticcraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shockahpi.AchievementPage;

import net.minecraft.block.Block;
import net.minecraft.stat.achievement.Achievement;

@Pseudo
@Mixin(targets = {"net/minecraft/mod_PlasticCraft"})
public class PlasticCraftMixin {

	@Shadow
	private static Block blockPLASTIC;

	@Shadow
	private static Achievement achPlastic;
	@Shadow
	private static Achievement achClrPlastic;
	@Shadow
	private static Achievement achPlexiglass;
	@Shadow
	private static Achievement achPlexDoor;
	@Shadow
	private static Achievement achPlasticBoat;
	@Shadow
	private static Achievement achBucket;
	@Shadow
	private static Achievement achBucketLava;
	@Shadow
	private static Achievement achBucketLatex;
	@Shadow
	private static Achievement achPlasString;
	@Shadow
	private static Achievement achSynthFiber;
	@Shadow
	private static Achievement achSynthFeather;
	@Shadow
	private static Achievement achPlasWrap;
	@Shadow
	private static Achievement achJello;
	@Shadow
	private static Achievement achGoo;
	@Shadow
	private static Achievement achTape;
	@Shadow
	private static Achievement achGooblock;
	@Shadow
	private static Achievement achNeedle;
	@Shadow
	private static Achievement achNeedleHealth;
	@Shadow
	private static Achievement achRedstone;
	@Shadow
	private static Achievement achChip;
	@Shadow
	private static Achievement achGoggles;
	@Shadow
	private static Achievement achMicrowave;
	@Shadow
	private static Achievement achC4;
	@Shadow
	private static Achievement achC4Blown;
	@Shadow
	private static Achievement achExpDispense;
	@Shadow
	private static Achievement achBattery;
	@Shadow
	private static Achievement achPlasRecord;
	@Shadow
	private static Achievement achBottle;
	@Shadow
	private static Achievement achRubber;
	@Shadow
	private static Achievement achTrampoline;
	@Shadow
	private static Achievement achAccelerator;
	@Shadow
	private static Achievement achPlasBoots;

	@ModifyVariable(method = "AddAchievements", index = 0, at = @At(value = "STORE"), remap = false)
	private static int AddAchievements(int xOff) {
		return 0;
	}

	@Inject(method = "AddAchievements", at = @At("TAIL"), remap = false)
	private static void AddAchievements(CallbackInfo ci) {
		final AchievementPage page = new AchievementPage("PlasticCraft") {
			@Override
			public int bgGetSprite(Random random, int x, int y) {
				return blockPLASTIC.texture;
			}
		};
		page.addAchievements(achPlastic, achClrPlastic, achPlexiglass, achPlexDoor, achPlasticBoat, achBucket, achBucketLava, achBucketLatex, achPlasString, achSynthFiber, achSynthFeather, achPlasWrap, achJello, achGoo, achTape, achGooblock, achNeedle, achNeedleHealth, achRedstone, achChip, achGoggles, achMicrowave, achC4, achC4Blown, achExpDispense, achBattery, achPlasRecord, achBottle, achRubber, achTrampoline, achAccelerator, achPlasBoots);
	}
}
