package io.github.betterthanupdates.apron.fixes.vanilla.compat.mixin.client.aether;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.AetherItems;

@Mixin(AetherItems.class)
public class AetherItemsMixin {
	@ModifyConstant(method = "<init>", constant = @Constant(stringValue = "mod_TooManyItems"))
	private static String fixTMICompat(String constant) {
		return "net.minecraft.mod_TooManyItems";
	}
}
