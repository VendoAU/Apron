package io.github.betterthanupdates.apron.fixes.vanilla.compat.mixin.client.spawneggs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Map;

@Pseudo
@Mixin(targets = {"net/minecraft/mod_spawnEggs"})
public class SpawnEggsMixin {

	@ModifyConstant(target = {@Desc(value = "AddRenderer", args = {Map.class})}, constant = @Constant(stringValue = "c"))
	private static String fixReflection(String constant) {
		return "field_759";
	}

	@ModifyConstant(target = {@Desc(value = "AddRenderer", args = {Map.class})}, constant = @Constant(stringValue = "b"))
	private static String fixReflection2(String constant) {
		return "field_758";
	}
}
