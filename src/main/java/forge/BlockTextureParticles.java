/**
 * This software is provided under the terms of the Minecraft Forge Public
 * License v1.1.
 */
package forge;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.entity.particle.ParticleEntity;

public class BlockTextureParticles {
	public String texture;
	public List<ParticleEntity> effects;

	public BlockTextureParticles() {
		this.effects = new ArrayList<>();
	}
}
