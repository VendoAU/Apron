package io.github.betterthanupdates.apron.fixes.vanilla.compat.mixin.client.netherores;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = {"net/minecraft/mod_NetherOres"})
public class NetherOresMixin {

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", desc = @Desc(owner = Class.class, value = "getDeclaredMethod", args = {String.class, Class[].class}, ret = java.lang.reflect.Method.class)), remap = false)
	public String init(String s) {
		return "method_1792";
	}
}
