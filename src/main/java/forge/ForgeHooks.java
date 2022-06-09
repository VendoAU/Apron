/**
 * Minecraft Forge Public Licence
 *
 * ==============================
 *
 *
 * Version 1.1
 *
 *
 * 0. Definitions
 *
 * --------------
 *
 *
 * Minecraft: Denotes a copy of the Minecraft game licensed by Mojang AB
 *
 *
 * User: Anybody that interact with the software in one of the following ways:
 *
 *    - play
 *
 *    - decompile
 *
 *    - recompile or compile
 *
 *    - modify
 *
 *
 * Minecraft Forge: The Minecraft Forge code, in source form, class file form, as
 *
 * obtained in a standalone fashion or as part of a wider distribution.
 *
 *
 * Dependency: Code required to have Minecraft Forge working properly. That can
 *
 * include dependencies required to compile the code as well as modifications in
 *
 * the Minecraft sources that are required to have Minecraft Forge working.
 *
 *
 * 1. Scope
 *
 * --------
 *
 *
 * The present license is granted to any user of Minecraft Forge. As a
 *
 * prerequisite, a user of Minecraft Forge must own a legally aquired copy of
 *
 * Minecraft
 *
 *
 * 2. Play rights
 *
 * --------------
 *
 *
 * The user of Minecraft Forge is allowed to install the software on a client or
 *
 * a server and to play it without restriction.
 *
 *
 * 3. Modification rights
 *
 * ----------------------
 *
 *
 * The user has the right to decompile the source code, look at either the
 *
 * decompiled version or the original source code, and to modify it.
 *
 *
 * 4. Derivation rights
 *
 * --------------------
 *
 *
 * The user has the rights to derive code from Minecraft Forge, that is to say to
 *
 * write code that either extends Minecraft Forge class and interfaces,
 *
 * instantiate the objects declared or calls the functions. This code is known as
 *
 * "derived" code, and can be licensed with conditions different from Minecraft
 *
 * Forge.
 *
 *
 *
 * 5. Distribution rights
 *
 * ----------------------
 *
 *
 * The user of Minecraft Forge is allowed to redistribute Minecraft Forge in
 *
 * partially, in totality, or included in a distribution. When distributing
 *
 * binaries or class files, the user must provide means to obtain the sources of
 *
 * the distributed version of Minecraft Forge at no costs. This includes the
 *
 * files as well as any dependency that the code may rely on, including patches to
 *
 * minecraft original sources.
 *
 *
 * Modification of Minecraft Forge as well as dependencies, including patches to
 *
 * minecraft original sources, has to remain under the terms of the present
 *
 * license.
 */
package forge;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SleepStatus;

import java.util.*;

import io.github.betterthanupdates.forge.block.ForgeBlock;
import io.github.betterthanupdates.forge.entity.player.ForgePlayerEntity;

import static forge.MinecraftForge.LOGGER;

public class ForgeHooks {
    static LinkedList<ICraftingHandler> craftingHandlers;
    static LinkedList<IDestroyToolHandler> destroyToolHandlers;
    static LinkedList<ISleepHandler> sleepHandlers;
    public static final int majorVersion = 1;
    public static final int minorVersion = 0;
    public static final int revisionVersion = 6;
    static boolean toolInit;
    static HashMap<Integer, List<Object>> toolClasses;
    static HashMap<List<Object>, Integer> toolHarvestLevels;
    static HashSet<List<Object>> toolEffectiveness;
    
    public static void onTakenFromCrafting(final PlayerEntity player, final ItemStack stack, final Inventory craftingGrid) {
        for (final ICraftingHandler handler : ForgeHooks.craftingHandlers) {
            handler.onTakenFromCrafting(player, craftingGrid, stack);
        }
    }
    
    public static void onDestroyCurrentItem(final PlayerEntity player, final ItemStack destroyedItem) {
        for (final IDestroyToolHandler handler : ForgeHooks.destroyToolHandlers) {
            handler.onDestroyCurrentItem(player, destroyedItem);
        }
    }
    
    public static SleepStatus sleepInBedAt(final PlayerEntity player, final int i, final int j, final int k) {
        for (final ISleepHandler handler : ForgeHooks.sleepHandlers) {
            final SleepStatus status = handler.sleepInBedAt(player, i, j, k);
            if (status != null) {
                return status;
            }
        }
        return null;
    }
    
    public static boolean canHarvestBlock(final Block block, final PlayerEntity player, final int meta) {
        if (block.material.doesRequireTool()) {
            return true;
        }
        final ItemStack itemstack = player.inventory.getHeldItem();
        if (itemstack == null) {
            return false;
        }
        final List<Object> tc = ForgeHooks.toolClasses.get(itemstack.itemId);
        if (tc == null) {
            return itemstack.isEffectiveOn(block);
        }
        final Object[] ta = tc.toArray();
        final String cls = (String)ta[0];
        final int hvl = (int)ta[1];
        final Integer bhl = ForgeHooks.toolHarvestLevels.get(Arrays.asList(block.id, meta, cls));
        if (bhl == null) {
            return itemstack.isEffectiveOn(block);
        }
        return bhl <= hvl && itemstack.isEffectiveOn(block);
    }
    
    public static float blockStrength(final Block block, final PlayerEntity player, final int meta) {
        final float bh = ((ForgeBlock) block).getHardness(meta);
        if (bh < 0.0f) return 0.0f;
        if (!canHarvestBlock(block, player, meta)) {
            return 1.0f / bh / 100.0f;
        }
        return ((ForgePlayerEntity) player).getCurrentPlayerStrVsBlock(block, meta) / bh / 30.0f;
    }
    
    public static boolean isToolEffective(final ItemStack item, final Block block, final int meta) {
        final List<Object> tc = ForgeHooks.toolClasses.get(item.itemId);
        if (tc == null) {
            return false;
        }
        final Object[] ta = tc.toArray();
        final String cls = (String) ta[0];
        return ForgeHooks.toolEffectiveness.contains(Arrays.asList(block.id, meta, cls));
    }
    
    static void initTools() {
        if (ForgeHooks.toolInit) {
            return;
        }
        ForgeHooks.toolInit = true;
        MinecraftForge.setToolClass(Item.WOOD_PICKAXE, "pickaxe", 0);
        MinecraftForge.setToolClass(Item.STONE_PICKAXE, "pickaxe", 1);
        MinecraftForge.setToolClass(Item.IRON_PICKAXE, "pickaxe", 2);
        MinecraftForge.setToolClass(Item.GOLD_PICKAXE, "pickaxe", 0);
        MinecraftForge.setToolClass(Item.DIAMOND_PICKAXE, "pickaxe", 3);
        MinecraftForge.setToolClass(Item.WOOD_AXE, "axe", 0);
        MinecraftForge.setToolClass(Item.STONE_AXE, "axe", 1);
        MinecraftForge.setToolClass(Item.IRON_AXE, "axe", 2);
        MinecraftForge.setToolClass(Item.GOLD_AXE, "axe", 0);
        MinecraftForge.setToolClass(Item.DIAMOND_AXE, "axe", 3);
        MinecraftForge.setToolClass(Item.WOOD_SHOVEL, "shovel", 0);
        MinecraftForge.setToolClass(Item.STONE_SHOVEL, "shovel", 1);
        MinecraftForge.setToolClass(Item.IRON_SHOVEL, "shovel", 2);
        MinecraftForge.setToolClass(Item.GOLD_SHOVEL, "shovel", 0);
        MinecraftForge.setToolClass(Item.DIAMOND_SHOVEL, "shovel", 3);
        MinecraftForge.setBlockHarvestLevel(Block.OBSIDIAN, "pickaxe", 3);
        MinecraftForge.setBlockHarvestLevel(Block.DIAMOND_ORE, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(Block.DIAMOND_BLOCK, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(Block.GOLD_ORE, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(Block.GOLD_BLOCK, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(Block.IRON_ORE, "pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(Block.IRON_BLOCK, "pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(Block.LAPIS_LAZULI_ORE, "pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(Block.LAPIS_LAZULI_BLOCK, "pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(Block.REDSTONE_ORE, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(Block.REDSTONE_ORE_LIT, "pickaxe", 2);
        MinecraftForge.removeBlockEffectiveness(Block.REDSTONE_ORE, "pickaxe");
        MinecraftForge.removeBlockEffectiveness(Block.REDSTONE_ORE_LIT, "pickaxe");
        final Block[] pickEffective = new Block[] { Block.COBBLESTONE, Block.DOUBLE_STONE_SLAB, Block.STONE_SLAB, Block.STONE, Block.SANDSTONE, Block.MOSSY_COBBLESTONE, Block.IRON_ORE, Block.IRON_BLOCK, Block.COAL_ORE, Block.GOLD_BLOCK, Block.GOLD_ORE, Block.DIAMOND_ORE, Block.DIAMOND_BLOCK, Block.ICE, Block.NETHERRACK, Block.LAPIS_LAZULI_ORE, Block.LAPIS_LAZULI_BLOCK };
        for (final Block bl : pickEffective) {
            MinecraftForge.setBlockHarvestLevel(bl, "pickaxe", 0);
        }
    }
    
    static {
        ForgeHooks.craftingHandlers = new LinkedList<>();
        ForgeHooks.destroyToolHandlers = new LinkedList<>();
        ForgeHooks.sleepHandlers = new LinkedList<>();
        LOGGER.info("MinecraftForge V%d.%d.%d Initialized\n", majorVersion, minorVersion, revisionVersion);
        ForgeHooks.toolInit = false;
        ForgeHooks.toolClasses = new HashMap<>();
        ForgeHooks.toolHarvestLevels = new HashMap<>();
        ForgeHooks.toolEffectiveness = new HashSet<>();
    }
}
