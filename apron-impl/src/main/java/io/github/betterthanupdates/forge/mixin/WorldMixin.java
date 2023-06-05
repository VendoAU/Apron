package io.github.betterthanupdates.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.util.math.AxixAlignedBoundingBox;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.github.betterthanupdates.forge.block.ForgeBlock;
import io.github.betterthanupdates.forge.world.ForgeWorld;

@Mixin(World.class)
public abstract class WorldMixin implements BlockView, ForgeWorld {
	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@ModifyReturnValue(method = "isAir", at = @At("RETURN"))
	public boolean isAir(boolean original, int x, int y, int z) {
		int l = this.getBlockId(x, y, z);
		return original || ((ForgeBlock) Block.BY_ID[l]).isAirBlock((World) (Object) this, x, y, z);
	}

	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@Inject(method = "method_165", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I", shift = At.Shift.BY, by = 2))
	public void method_165(LightType lightType, int x, int y, int z, int l, CallbackInfo ci, @Local(index = 4) LocalIntRef refL, @Local(index = 6) int i1) {
		int bl = i1 == 0 ? 0 : ((ForgeBlock) Block.BY_ID[i1]).getLightValue(this, x, y, z);

		if (bl > l) {
			refL.set(bl);
		}
	}

	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@Redirect(method = "method_165", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;EMITTANCE:[I"))
	public int[] method_165$redirectEmittance() {
		return new int[0];
	}

	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@Inject(method = "method_225", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I", shift = At.Shift.BY, by = 4), cancellable = true)
	public void method_225(AxixAlignedBoundingBox var1, CallbackInfoReturnable<Boolean> cir, @Local(index = 8) int k1, @Local(index = 9) int l1, @Local(index = 10) int i2, @Local(index = 11) int j2) {
		if (j2 > 0 && ((ForgeBlock) Block.BY_ID[j2]).isBlockBurning((World) (Object) this, k1, l1, i2)) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@Inject(method = "canSuffocate", at = @At("HEAD"), cancellable = true)
	public void canSuffocate(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		Block block = Block.BY_ID[this.getBlockId(x, y, z)];
		cir.setReturnValue(block != null && ((ForgeBlock) block).isBlockNormalCube((World) (Object) this, x, y, z));
	}

	@Override
	public boolean isBlockSolidOnSide(int x, int y, int z, int side) {
		Block block = Block.BY_ID[this.getBlockId(x, y, z)];
		return block != null && ((ForgeBlock) block).isBlockSolidOnSide((World) (Object) this, x, y, z, side);
	}

	/**
	 * @author Eloraam
	 * @reason implement Forge hooks
	 */
	@Redirect(method = "canPlaceBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canPlaceAt(Lnet/minecraft/world/World;IIII)Z"))
	public boolean canPlaceBlock(Block block1, World world, int x, int y, int z, int i, @Local(ordinal = 0) Block block) {
		if (block != null && ((ForgeBlock) block).isBlockReplaceable((World) (Object) this, x, y, z)) {
			return block1.canPlaceAt(world, x, y, z, i);
		}
		return false;
	}
}
