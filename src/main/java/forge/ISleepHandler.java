/**
 * This software is provided under the terms of the Minecraft Forge Public
 * License v1.1.
 */
package forge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SleepStatus;

public interface ISleepHandler {
	SleepStatus sleepInBedAt(final PlayerEntity player, final int i, final int j, final int k);
}
