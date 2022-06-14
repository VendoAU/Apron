package modloader;

import io.github.betterthanupdates.babricated.BabricatedForge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.block.Block;
import net.minecraft.client.GameStartupError;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StatEntity;
import net.minecraft.client.TexturePackManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.TextureBinder;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.client.render.entity.block.BlockEntityRenderer;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Session;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeRegistry;
import net.minecraft.recipe.SmeltingRecipeRegistry;
import net.minecraft.stat.Stats;
import net.minecraft.stat.achievement.Achievement;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.HellBiome;
import net.minecraft.world.biome.SkyBiome;
import net.minecraft.world.source.WorldSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("unused")
public class ModLoader {
	private static final List<TextureBinder> ANIM_LIST = new LinkedList<>();
	private static final Map<Integer, BaseMod> BLOCK_MODELS = new HashMap<>();
	private static final Map<Integer, Boolean> BLOCK_SPECIAL_INV = new HashMap<>();
	private static final File CONFIG_DIR = new File(Minecraft.getGameDirectory(), "/config/");
	private static final File CONFIG_FILE = new File(CONFIG_DIR, "ModLoader.cfg");
	public static Level cfgLoggingLevel = Level.FINER;
	private static long clock = 0L;
	private static Field field_modifiers = null;
	private static boolean hasInit = false;
	private static int highestEntityId = 3000;
	private static final Map<BaseMod, Boolean> inGameHooks = new HashMap<>();
	private static final Map<BaseMod, Boolean> inGUIHooks = new HashMap<>();
	private static Minecraft instance = null;
	private static int itemSpriteIndex = 0;
	private static int itemSpritesLeft = 0;
	private static final Map<BaseMod, Map<KeyBinding, boolean[]>> keyList = new HashMap<>();
	private static final File LOG_FILE = new File(Minecraft.getGameDirectory(), "ModLoader.txt");
	private static final Logger LOGGER = Logger.getLogger("ModLoader");
	private static FileHandler logHandler = null;
	private static final File MOD_DIR = new File(Minecraft.getGameDirectory(), "/mods/");
	private static final LinkedList<BaseMod> MOD_LIST = new LinkedList<>();
	private static int nextBlockModelID = 1000;
	private static final Map<Integer, Map<String, Integer>> overrides = new HashMap<>();
	public static final Properties props = new Properties();
	private static Biome[] standardBiomes;
	private static int terrainSpriteIndex = 0;
	private static int terrainSpritesLeft = 0;
	private static String texPack = null;
	private static boolean texturesAdded = false;
	private static final boolean[] USED_ITEM_SPRITES = new boolean[256];
	private static final boolean[] USED_TERRAIN_SPRITES = new boolean[256];
	public static final String VERSION = "ModLoader Beta 1.7.3";
	
	/**
	 * Used to give your achievement a readable name and description.
	 * @param achievement the entry to be described
	 * @param name the name of the entry
	 * @param description the description of the entry
	 */
	@SuppressWarnings("unused")
	public static void AddAchievementDesc(Achievement achievement, String name, String description) {
		try {
			if (achievement.name.contains(".")) {
				String[] split = achievement.name.split("\\.");
				if (split.length == 2) {
					String key = split[1];
					AddLocalization("achievement." + key, name);
					AddLocalization("achievement." + key + ".desc", description);
					achievement.name = TranslationStorage.getInstance().translate("achievement." + key);
					achievement.description = TranslationStorage.getInstance().translate("achievement." + key + ".desc");
				} else {
					achievement.name = name;
					achievement.description = description;
				}
			} else {
				achievement.name = name;
				achievement.description = description;
			}
		} catch (IllegalArgumentException | SecurityException var5) {
			LOGGER.throwing("ModLoader", "AddAchievementDesc", var5);
			ThrowException(var5);
		}
	}
	
	/**
	 * Used for adding new sources of fuel to the furnace.
	 * @param id the item to be used as fuel.
	 * @return the fuel ID assigned to the item.
	 */
	public static int AddAllFuel(int id) {
		LOGGER.finest("Finding fuel for " + id);
		int result = 0;
		Iterator<BaseMod> iter = MOD_LIST.iterator();

		while(iter.hasNext() && result == 0) {
			result = iter.next().AddFuel(id);
		}

		if (result != 0) {
			LOGGER.finest("Returned " + result);
		}

		return result;
	}
	
	/**
	 * Used to add all mod entity renderers.
	 * @param rendererMap renderers to add
	 */
	public static void AddAllRenderers(Map<Class<? extends Entity>, EntityRenderer> rendererMap) {
		if (!hasInit) {
			init();
			LOGGER.fine("Initialized");
		}

		for(BaseMod mod : MOD_LIST) {
			mod.AddRenderer(rendererMap);
		}
	}
	
	/**
	 * Registers one animation instance.
	 * @param textureBinder animation instance to register
	 */
	public static void addAnimation(TextureBinder textureBinder) {
		LOGGER.finest("Adding animation " + textureBinder.toString());

		for(TextureBinder oldAnim : ANIM_LIST) {
			if (oldAnim.renderMode == textureBinder.renderMode && oldAnim.index == textureBinder.index) {
				ANIM_LIST.remove(textureBinder);
				break;
			}
		}

		ANIM_LIST.add(textureBinder);
	}
	
	/**
	 * Use this when you need the player to have new armor skin.
	 * @param armor Name of the armor skin
	 * @return index assign for the armor skin
	 */
	@SuppressWarnings("unused")
	public static int AddArmor(String armor) {
		try {
			String[] existingArmor = PlayerRenderer.armorTypes;
			List<String> existingArmorList = Arrays.asList(existingArmor);
			List<String> combinedList = new ArrayList<>(existingArmorList);
			if (!combinedList.contains(armor)) {
				combinedList.add(armor);
			}

			int index = combinedList.indexOf(armor);
			PlayerRenderer.armorTypes = combinedList.toArray(new String[0]);
			return index;
		} catch (IllegalArgumentException var5) {
			LOGGER.throwing("ModLoader", "AddArmor", var5);
			ThrowException("An impossible error has occured!", var5);
		}

		return -1;
	}
	
	/**
	 * Method for adding raw strings to the translation table.
	 * @param key tag for string
	 * @param value string to add
	 */
	public static void AddLocalization(String key, String value) {
		Properties props = TranslationStorage.getInstance().translations;

		if (props != null) {
			props.put(key, value);
		}
	}

	private static void addMod(ClassLoader loader, String filename) {
		try {
			String name = filename.split("\\.")[0];
			if (name.contains("$")) {
				return;
			}

			if (props.containsKey(name) && (props.getProperty(name).equalsIgnoreCase("no") || props.getProperty(name).equalsIgnoreCase("off"))) {
				return;
			}

			// TODO
//			Package pack = ModLoader.class.getPackage();
//			if (pack != null) {
//				name = pack.getName() + "." + name;
//			}

			Class<?> modClass = loader.loadClass(name);
			if (!BaseMod.class.isAssignableFrom(modClass)) return;

			//noinspection unchecked
			setupProperties((Class<? extends BaseMod>) modClass);
			BaseMod mod = (BaseMod)modClass.getDeclaredConstructor().newInstance();
			MOD_LIST.add(mod);
			LOGGER.fine("Mod Loaded: \"" + mod + "\" from " + filename);
		} catch (Throwable var6) {
			LOGGER.fine("Failed to load mod from \"" + filename + "\"");
			LOGGER.throwing("ModLoader", "addMod", var6);
			ThrowException(var6);
		}

	}

	/**
	 * This method will allow adding name to item in inventory.
	 * @param instance A block, item, or item stack reference to name
	 * @param name The name to give
	 */
	@SuppressWarnings("unused")
	public static void AddName(Object instance, String name) {
		String tag = null;
		if (instance instanceof Item) {
			Item item = (Item)instance;
			if (item.getTranslationKey() != null) {
				tag = item.getTranslationKey() + ".name";
			}
		} else if (instance instanceof Block) {
			Block block = (Block)instance;
			if (block.getTranslationKey() != null) {
				tag = block.getTranslationKey() + ".name";
			}
		} else if (instance instanceof ItemStack) {
			ItemStack stack = (ItemStack)instance;
			if (stack.getTranslationKey() != null) {
				tag = stack.getTranslationKey() + ".name";
			}
		} else {
			Exception e = new Exception(instance.getClass().getName() + " cannot have name attached to it!");
			LOGGER.throwing("ModLoader", "AddName", e);
			ThrowException(e);
		}

		if (tag != null) {
			AddLocalization(tag, name);
		} else {
			Exception e = new Exception(instance + " is missing name tag!");
			LOGGER.throwing("ModLoader", "AddName", e);
			ThrowException(e);
		}
	}

	/**
	 * Use this to add custom images for your items and blocks.
	 * @param fileToOverride file to override ("/terrain.png" or "/gui/items.png")
	 * @param fileToAdd path to the image you want to add
	 * @return unique sprite index
	 */
	@SuppressWarnings("unused")
	public static int addOverride(String fileToOverride, String fileToAdd) {
		try {
			int i = getUniqueSpriteIndex(fileToOverride);
			addOverride(fileToOverride, fileToAdd, i);
			return i;
		} catch (Throwable var3) {
			LOGGER.throwing("ModLoader", "addOverride", var3);
			ThrowException(var3);
			throw new RuntimeException(var3);
		}
	}

	/**
	 * Registers one texture override to be done.
	 * @param path Path to the texture file to modify ("/terrain.png" or "/gui/items.png")
	 * @param overlayPath Path to the texture file which is to be overlaid
	 * @param index Sprite index into the texture to be modified
	 */
	public static void addOverride(String path, String overlayPath, int index) {
		int dst;
		int left;
		if (path.equals("/terrain.png")) {
			dst = 0;
			left = terrainSpritesLeft;
		} else {
			if (!path.equals("/gui/items.png")) {
				return;
			}

			dst = 1;
			left = itemSpritesLeft;
		}

		LOGGER.finer("addOverride(" + path + "," + overlayPath + "," + index + "). " + left + " left.");
		Map<String, Integer> overlays = overrides.computeIfAbsent(dst, k -> new HashMap<>());

		overlays.put(overlayPath, index);
	}

	/**
	 * Add a shaped recipe to crafting list.
	 * @param output the result of the crafting recipe
	 * @param ingredients the ingredients for the crafting recipe from top left to bottom right.
	 */
	@SuppressWarnings("unused")
	public static void AddRecipe(ItemStack output, Object... ingredients) {
		RecipeRegistry.getInstance().addShapedRecipe(output, ingredients);
	}

	/**
	 * Add recipe to crafting list.
	 * @param output the result of the crafting recipe
	 * @param ingredients ingredients for the recipe in any order
	 */
	public static void AddShapelessRecipe(ItemStack output, Object... ingredients) {
		RecipeRegistry.getInstance().addShapelessRecipe(output, ingredients);
	}

	/**
	 * Used to add smelting recipes to the furnace.
	 * @param input ingredient for the recipe
	 * @param output the result of the furnace recipe
	 */
	public static void AddSmelting(int input, ItemStack output) {
		SmeltingRecipeRegistry.getInstance().addSmeltingRecipe(input, output);
	}

	/**
	 * Add entity to spawn list for all biomes except Hell.
	 * @param entityClass entity to spawn
	 * @param weightedProb chance of spawning for every try
	 * @param spawnGroup group to spawn the entity in
	 */
	public static void AddSpawn(Class<? extends LivingEntity> entityClass, int weightedProb, SpawnGroup spawnGroup) {
		AddSpawn(entityClass, weightedProb, spawnGroup, (Biome) null);
	}

	/**
	 * Add entity to spawn list for selected biomes.
	 * @param entityClass entity to spawn
	 * @param weightedProb chance of spawning for every try
	 * @param spawnGroup group to spawn the entity in
	 * @param biomes biomes to spawn the entity in
	 */
	@SuppressWarnings("unchecked")
	public static void AddSpawn(Class<? extends LivingEntity> entityClass, int weightedProb, SpawnGroup spawnGroup, Biome... biomes) {
		if (entityClass == null) {
			throw new IllegalArgumentException("entityClass cannot be null");
		} else if (spawnGroup == null) {
			throw new IllegalArgumentException("spawnList cannot be null");
		} else {
			if (biomes == null) {
				biomes = standardBiomes;
			}

			for (Biome biome : biomes) {
				List<EntityEntry> list = biome.getSpawnList(spawnGroup);
				if (list != null) {
					boolean exists = false;

					for (EntityEntry entry : list) {
						if (entry.entryClass == entityClass) {
							entry.rarity = weightedProb;
							exists = true;
							break;
						}
					}

					if (!exists) {
						list.add(new EntityEntry(entityClass, weightedProb));
					}
				}
			}
		}
	}
	
	/**
	 * Add entity to spawn list for all biomes except Hell.
	 * @param entityName Name of entity to spawn
	 * @param chance Higher number means more likely to spawn
	 * @param spawnGroup The spawn group to add entity to (Monster, Creature, or Water)
	 */
	public static void AddSpawn(String entityName, int chance, SpawnGroup spawnGroup) {
		AddSpawn(entityName, chance, spawnGroup, (Biome) null);
	}
	
	/**
	 * Add entity to spawn list for selected biomes.
	 * @param entityName Name of entity to spawn
	 * @param weightedProb Higher number means more likely to spawn
	 * @param spawnGroup The spawn group to add entity to (Monster, Creature, or Water)
	 * @param biomes Array of biomes to add entity spawning to
	 */
	@SuppressWarnings("unchecked")
	public static void AddSpawn(String entityName, int weightedProb, SpawnGroup spawnGroup, Biome... biomes) {
		Class<? extends Entity> entityClass = (Class<? extends Entity>) EntityRegistry.STRING_ID_TO_CLASS.get(entityName);
		if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {
			AddSpawn((Class<? extends LivingEntity>) entityClass, weightedProb, spawnGroup, biomes);
		}
	}
	
	/**
	 * Dispenses the entity associated with the selected item.
	 * @param world world to spawn in
	 * @param x x position
	 * @param y y position
	 * @param z z position
	 * @param xVel x velocity
	 * @param zVel z velocity
	 * @param stack item inside the dispenser to dispense
	 * @return whether dispensing was successful
	 */
	public static boolean DispenseEntity(World world, double x, double y, double z, int xVel, int zVel, ItemStack stack) {
		boolean result = false;
		Iterator<BaseMod> iter = MOD_LIST.iterator();

		while(iter.hasNext() && !result) {
			result = iter.next().DispenseEntity(world, x, y, z, xVel, zVel, stack);
		}

		return result;
	}
	
	/**
	 * Use this method if you need a list of loaded mods.
	 * @return the list of loaded {@link BaseMod ModLoader mods}
	 */
	public static List<BaseMod> getLoadedMods() {
		return Collections.unmodifiableList(MOD_LIST);
	}
	
	/**
	 * Use this to get a reference to the logger ModLoader uses.
	 * (Or just reference {@link #LOGGER})
	 * @return the logger instance
	 */
	public static Logger getLogger() {
		return LOGGER;
	}
	
	/**
	 * Use this method to get a reference to Minecraft instance.
	 * @return Minecraft client instance
	 */
	@NotNull
	@Environment(EnvType.CLIENT)
	public static Minecraft getMinecraftInstance() {
		if (instance == null) {
			instance = (Minecraft) FabricLoaderImpl.INSTANCE.getGameInstance();
		}
		return instance;
	}
	
	/**
	 * Used for getting value of private fields.
	 * @param instanceClass Class to use with instance.
	 * @param instance Object to get private field from.
	 * @param fieldIndex Name of the field.
	 * @param <T> Return type
	 * @param <E> Type of instance
	 * @return Value of private field
	 * @throws IllegalArgumentException if instance isn't compatible with instanceClass
	 * @throws SecurityException if the thread is not allowed to access field
	 */
	@SuppressWarnings("unchecked")
	public static <T, E> T getPrivateValue(Class<? super E> instanceClass, E instance, int fieldIndex) throws IllegalArgumentException, SecurityException {
		try {
			Field f = instanceClass.getDeclaredFields()[fieldIndex];
			f.setAccessible(true);
			return (T)f.get(instance);
		} catch (IllegalAccessException var4) {
			LOGGER.throwing("ModLoader", "getPrivateValue", var4);
			ThrowException("An impossible error has occured!", var4);
			return null;
		}
	}
	
	/**
	 * Used for getting value of private fields.
	 * @param instanceClass Class to use with instance
	 * @param instance Object to get private field from
	 * @param fieldName Name of the field
	 * @param <T> Return type
	 * @param <E> Type of instance
	 * @return Value of private field
	 * @throws IllegalArgumentException if instance isn't compatible with instanceClass
	 * @throws SecurityException if the thread is not allowed to access field
	 * @throws NoSuchFieldException if field does not exist
	 */
	@SuppressWarnings("unchecked")
	public static <T, E> T getPrivateValue(Class<? super E> instanceClass, E instance, String fieldName) throws IllegalArgumentException, SecurityException, NoSuchFieldException {
		try {
			Field f = instanceClass.getDeclaredField(fieldName);
			f.setAccessible(true);
			return (T)f.get(instance);
		} catch (IllegalAccessException var4) {
			LOGGER.throwing("ModLoader", "getPrivateValue", var4);
			ThrowException("An impossible error has occured!", var4);
			return null;
		}
	}
	
	/**
	 * Assigns a model id for blocks to use for the given mod.
	 * @param mod to assign id to
	 * @param full3DItem if true the item will have 3D model created from {@link #RenderInvBlock(BlockRenderer, Block, int, int)}, if false will be a flat image
	 * @return assigned block model id
	 */
	public static int getUniqueBlockModelID(BaseMod mod, boolean full3DItem) {
		int id = nextBlockModelID++;
		BLOCK_MODELS.put(id, mod);
		BLOCK_SPECIAL_INV.put(id, full3DItem);
		return id;
	}
	
	/**
	 * Gets next Entity ID to use.
	 * @return Assigned ID
	 */
	public static int getUniqueEntityId() {
		return highestEntityId++;
	}

	private static int getUniqueItemSpriteIndex() {
		while(itemSpriteIndex < USED_ITEM_SPRITES.length) {
			if (!USED_ITEM_SPRITES[itemSpriteIndex]) {
				USED_ITEM_SPRITES[itemSpriteIndex] = true;
				--itemSpritesLeft;
				return itemSpriteIndex++;
			}

			++itemSpriteIndex;
		}

		Exception e = new Exception("No more empty item sprite indices left!");
		LOGGER.throwing("ModLoader", "getUniqueItemSpriteIndex", e);
		ThrowException(e);
		return 0;
	}

	/**
	 * Gets next available index for this sprite map.
	 * @param path path to sprite sheet to get available index from
	 * @return Assigned sprite index to use
	 */
	public static int getUniqueSpriteIndex(String path) {
		if (path.equals("/gui/items.png")) {
			return getUniqueItemSpriteIndex();
		} else if (path.equals("/terrain.png")) {
			return getUniqueTerrainSpriteIndex();
		} else {
			Exception e = new Exception("No registry for this texture: " + path);
			LOGGER.throwing("ModLoader", "getUniqueItemSpriteIndex", e);
			ThrowException(e);
			return 0;
		}
	}

	private static int getUniqueTerrainSpriteIndex() {
		while(terrainSpriteIndex < USED_TERRAIN_SPRITES.length) {
			if (!USED_TERRAIN_SPRITES[terrainSpriteIndex]) {
				USED_TERRAIN_SPRITES[terrainSpriteIndex] = true;
				--terrainSpritesLeft;
				return terrainSpriteIndex++;
			}

			++terrainSpriteIndex;
		}

		Exception e = new Exception("No more empty terrain sprite indices left!");
		LOGGER.throwing("ModLoader", "getUniqueItemSpriteIndex", e);
		ThrowException(e);
		return 0;
	}

	private static void init() {
		hasInit = true;
		String usedItemSpritesString = "1111111111111111111111111111111111111101111111011111111111111001111111111111111111111111111011111111100110000011111110000000001111111001100000110000000100000011000000010000001100000000000000110000000000000000000000000000000000000000000000001100000000000000";
		String usedTerrainSpritesString = "1111111111111111111111111111110111111111111111111111110111111111111111111111000111111011111111111111001111111110111111111111100011111111000010001111011110000000111111000000000011111100000000001111000000000111111000000000001101000000000001111111111111000011";

		for(int i = 0; i < 256; ++i) {
			USED_ITEM_SPRITES[i] = usedItemSpritesString.charAt(i) == '1';
			if (!USED_ITEM_SPRITES[i]) {
				++itemSpritesLeft;
			}

			USED_TERRAIN_SPRITES[i] = usedTerrainSpritesString.charAt(i) == '1';
			if (!USED_TERRAIN_SPRITES[i]) {
				++terrainSpritesLeft;
			}
		}

		try {
			instance = getMinecraftInstance();
			instance.gameRenderer = new EntityRendererProxy(instance);
			field_modifiers = Field.class.getDeclaredField("modifiers");
			field_modifiers.setAccessible(true);
			Field[] fieldArray = Biome.class.getDeclaredFields();
			List<Biome> biomes = new LinkedList<>();

			for (Field field : fieldArray) {
				Class<?> fieldType = field.getType();
				if ((field.getModifiers() & 8) != 0 && fieldType.isAssignableFrom(Biome.class)) {
					Biome biome = (Biome) field.get(null);
					if (!(biome instanceof HellBiome) && !(biome instanceof SkyBiome)) {
						biomes.add(biome);
					}
				}
			}

			standardBiomes = biomes.toArray(new Biome[0]);
		} catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException var10) {
			LOGGER.throwing("ModLoader", "init", var10);
			ThrowException(var10);
			throw new RuntimeException(var10);
		}

		try {
			loadConfig();
			if (props.containsKey("loggingLevel")) {
				cfgLoggingLevel = Level.parse(props.getProperty("loggingLevel"));
			}

			LOGGER.setLevel(cfgLoggingLevel);
			if ((LOG_FILE.exists() || LOG_FILE.createNewFile()) && LOG_FILE.canWrite() && logHandler == null) {
				logHandler = new FileHandler(LOG_FILE.getPath());
				logHandler.setFormatter(new SimpleFormatter());
				LOGGER.addHandler(logHandler);
			}

			LOGGER.fine(VERSION + " Initializing...");
			File source = new File(ModLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			MOD_DIR.mkdirs();
			if (!BabricatedForge.MOD_CACHE_FOLDER.mkdirs()) {
				BabricatedForge.LOGGER.debug("Could not create mod cache directory. It may already exist.");
			}
			readFromModFolder(BabricatedForge.MOD_CACHE_FOLDER);
			readFromClassPath(source);
			LOGGER.log(Level.INFO, "Done initializing.");
			props.setProperty("loggingLevel", cfgLoggingLevel.getName());

			for(BaseMod mod : MOD_LIST) {
				mod.ModsLoaded();
				if (!props.containsKey(mod.getClass().getName())) {
					props.setProperty(mod.getClass().getName(), "on");
				}
			}

			instance.options.keyBindings = RegisterAllKeys(instance.options.keyBindings);
			instance.options.load();
			initStats();
			saveConfig();
		} catch (Throwable var9) {
			LOGGER.throwing("ModLoader", "init", var9);
			ThrowException("ModLoader has failed to initialize.", var9);
			if (logHandler != null) {
				logHandler.close();
			}

			throw new RuntimeException(var9);
		}
	}

	@SuppressWarnings("unchecked")
	private static void initStats() {
		for(int id = 0; id < Block.BY_ID.length; ++id) {
			if (!Stats.idMap.containsKey(16777216 + id) && Block.BY_ID[id] != null && Block.BY_ID[id].isStatEnabled()) {
				String str = TranslationStorage.getInstance().translate("stat.mineBlock", Block.BY_ID[id].getTranslatedName());
				Stats.mineBlock[id] = new StatEntity(16777216 + id, str, id).register();
				Stats.blocksMinedList.add(Stats.mineBlock[id]);
			}
		}

		for(int id = 0; id < Item.byId.length; ++id) {
			if (!Stats.idMap.containsKey(16908288 + id) && Item.byId[id] != null) {
				String str = TranslationStorage.getInstance().translate("stat.useItem", Item.byId[id].getTranslatedName());
				Stats.useItem[id] = new StatEntity(16908288 + id, str, id).register();
				if (id >= Block.BY_ID.length) {
					Stats.useStatList.add(Stats.useItem[id]);
				}
			}

			if (!Stats.idMap.containsKey(16973824 + id) && Item.byId[id] != null && Item.byId[id].hasDurability()) {
				String str = TranslationStorage.getInstance().translate("stat.breakItem", Item.byId[id].getTranslatedName());
				Stats.breakItem[id] = new StatEntity(16973824 + id, str, id).register();
			}
		}

		HashSet<Integer> idHashSet = new HashSet<>();

		for(Object result : RecipeRegistry.getInstance().getRecipes()) {
			idHashSet.add(((Recipe)result).getOutput().itemId);
		}

		for(Object result : SmeltingRecipeRegistry.getInstance().getRecipes().values()) {
			idHashSet.add(((ItemStack)result).itemId);
		}

		for(int id : idHashSet) {
			if (!Stats.idMap.containsKey(16842752 + id) && Item.byId[id] != null) {
				String str = TranslationStorage.getInstance().translate("stat.craftItem", Item.byId[id].getTranslatedName());
				Stats.timesCrafted[id] = new StatEntity(16842752 + id, str, id).register();
			}
		}
	}
	
	/**
	 * Use this method to check if GUI is opened for the player.
	 * @param gui The type of GUI to check for. If null, will check for any GUI
	 * @return true if GUI is open
	 */
	public static boolean isGUIOpen(@Nullable Class<? extends Screen> gui) {
		Minecraft game = getMinecraftInstance();
		if (gui == null) {
			return game.currentScreen == null;
		} else {
			return gui.isInstance(game.currentScreen);
		}
	}
	
	/**
	 * Checks if a mod is loaded.
	 * @param modName name of the mod to check for
	 * @return true if a mod with supplied name exists in the mod list
	 */
	public static boolean isModLoaded(String modName) {
		Class<?> chk;

		try {
			chk = Class.forName(modName);
		} catch (ClassNotFoundException var4) {
			return false;
		}

		for(BaseMod mod : MOD_LIST) {
			if (chk.isInstance(mod)) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Reads the config file and stores the contents in props.
	 * @throws IOException
	 */
	public static void loadConfig()  {
		CONFIG_DIR.mkdir();
		try {
			if (CONFIG_FILE.exists() || CONFIG_FILE.createNewFile()) {
				if (CONFIG_FILE.canRead()) {
					InputStream in = Files.newInputStream(CONFIG_FILE.toPath());
					props.load(in);
					in.close();
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.CONFIG, "Config could not be loaded!", e);
		}
	}
	
	/**
	 * Loads an image from a file in the jar into a BufferedImage.
	 * @param textureManager Reference to texture cache
	 * @param path Path inside the jar to the image (starts with /)
	 * @return Loaded image to override with
	 * @throws FileNotFoundException if the image is not found
	 * @throws Exception if the image is corrupted
	 */
	public static BufferedImage loadImage(TextureManager textureManager, String path)
			throws FileNotFoundException, Exception {
		TexturePackManager packManager = textureManager.texturePackManager;
		InputStream input = packManager.texturePack.getResourceAsStream(path);
		if (input == null) {
			throw new FileNotFoundException("Image not found: " + path);
		} else {
			BufferedImage image = ImageIO.read(input);
			if (image == null) {
				throw new Exception("Image corrupted: " + path);
			} else {
				return image;
			}
		}
	}
	
	/**
	 * Is called when an item is picked up from the world.
	 * @param player that picked up item
	 * @param item that was picked up
	 */
	public static void OnItemPickup(PlayerEntity player, ItemStack item) {
		for(BaseMod mod : MOD_LIST) {
			mod.OnItemPickup(player, item);
		}
	}
	
	/**
	 * This method is called every tick while minecraft is running.
	 * @param minecraft instance of the game class
	 */
	public static void OnTick(Minecraft minecraft) {
		if (!hasInit) {
			init();
			LOGGER.fine("Initialized");
		}

		if (texPack == null || !Objects.equals(minecraft.options.skin, texPack)) {
			texturesAdded = false;
			texPack = minecraft.options.skin;
		}

		if (!texturesAdded && minecraft.textureManager != null) {
			RegisterAllTextureOverrides(minecraft.textureManager);
			texturesAdded = true;
		}

		long newClock = 0L;
		if (minecraft.world != null) {
			newClock = minecraft.world.getWorldTime();
			Iterator<Entry<BaseMod, Boolean>> iterator = inGameHooks.entrySet().iterator();

			while(iterator.hasNext()) {
				Entry<BaseMod, Boolean> modSet = iterator.next();
				if ((clock != newClock || !modSet.getValue()) && !modSet.getKey().OnTickInGame(minecraft)) {
					iterator.remove();
				}
			}
		}

		if (minecraft.currentScreen != null) {
			Iterator<Entry<BaseMod, Boolean>> iter = inGUIHooks.entrySet().iterator();

			while(iter.hasNext()) {
				Entry<BaseMod, Boolean> modSet = iter.next();
				if ((clock != newClock || !(modSet.getValue() & minecraft.world != null)) && !modSet.getKey().OnTickInGUI(minecraft, minecraft.currentScreen)) {
					iter.remove();
				}
			}
		}

		if (clock != newClock) {
			for(Entry<BaseMod, Map<KeyBinding, boolean[]>> modSet : keyList.entrySet()) {
				for(Entry<KeyBinding, boolean[]> keySet : modSet.getValue().entrySet()) {
					boolean state = Keyboard.isKeyDown(keySet.getKey().key);
					boolean[] keyInfo = keySet.getValue();
					boolean oldState = keyInfo[1];
					keyInfo[1] = state;
					if (state && (!oldState || keyInfo[0])) {
						modSet.getKey().KeyboardEvent(keySet.getKey());
					}
				}
			}
		}

		clock = newClock;
	}
	
	/**
	 * Opens GUI for use with mods.
	 * @param player instance to open GUI for
	 * @param screen instance of GUI to open for player
	 */
	public static void OpenGUI(PlayerEntity player, Screen screen) {
		if (!hasInit) {
			init();
			LOGGER.fine("Initialized");
		}

		Minecraft game = getMinecraftInstance();
		if (game.player == player) {
			if (screen != null) {
				game.openScreen(screen);
			}
		}
	}
	
	/**
	 * Used for generating new blocks in the world.
	 * @param source Generator to pair with
	 * @param chunkX X coordinate of chunk
	 * @param chunkZ Z coordinate of chunk
	 * @param world World to generate blocks in
	 */
	public static void PopulateChunk(WorldSource source, int chunkX, int chunkZ, World world) {
		if (!hasInit) {
			init();
			LOGGER.fine("Initialized");
		}

		Random rnd = new Random(world.getSeed());
		long xSeed = rnd.nextLong() / 2L * 2L + 1L;
		long zSeed = rnd.nextLong() / 2L * 2L + 1L;
		rnd.setSeed((long)chunkX * xSeed + (long)chunkZ * zSeed ^ world.getSeed());

		for(BaseMod mod : MOD_LIST) {
			if (source.toString().equals("RandomLevelSource")) {
				mod.GenerateSurface(world, rnd, chunkX << 4, chunkZ << 4);
			} else if (source.toString().equals("HellRandomLevelSource")) {
				mod.GenerateNether(world, rnd, chunkX << 4, chunkZ << 4);
			}
		}
	}

	private static void readFromClassPath(File source) throws IOException {
		LOGGER.finer("Adding mods from " + source.getCanonicalPath());
		ClassLoader loader = ModLoader.class.getClassLoader();
		if (source.isFile() && (source.getName().endsWith(".jar") || source.getName().endsWith(".zip"))) {
			LOGGER.finer("Zip found.");
			InputStream input = Files.newInputStream(source.toPath());
			ZipInputStream zip = new ZipInputStream(input);
			ZipEntry entry;

			while(true) {
				entry = zip.getNextEntry();
				if (entry == null) {
					input.close();
					break;
				}

				String name = entry.getName();
				if (!entry.isDirectory() && name.startsWith("mod_") && name.endsWith(".class")) {
					addMod(loader, name);
				}
			}
		} else if (source.isDirectory()) {
			Package pkg = ModLoader.class.getPackage();
			if (pkg != null) {
				String pkgdir = pkg.getName().replace('.', File.separatorChar);
				source = new File(source, pkgdir);
			}

			LOGGER.finer("Directory found.");
			File[] files = source.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					if (file.isFile() && name.startsWith("mod_") && name.endsWith(".class")) {
						addMod(loader, name);
					}
				}
			}
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static void readFromModFolder(File folder) throws IOException, IllegalArgumentException, SecurityException {
		ClassLoader loader = Minecraft.class.getClassLoader();
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("folder must be a Directory.");
		} else {
			File[] sourceFiles = folder.listFiles();
			if (sourceFiles != null && sourceFiles.length > 0) {
				for (File source : sourceFiles) {
					if (source.isDirectory() || source.isFile() && (source.getName().endsWith(".jar") || source.getName().endsWith(".zip"))) {
						FabricLauncherBase.getLauncher().addToClassPath(source.toPath());
					}
				}
			}

			if (sourceFiles != null && sourceFiles.length > 0) {
				for (File sourceFile : sourceFiles) {
					File source = sourceFile;
					if (source.isDirectory() || source.isFile() && (source.getName().endsWith(".jar") || source.getName().endsWith(".zip"))) {
						LOGGER.finer("Adding mods from " + source.getCanonicalPath());
						if (!source.isFile()) {
							if (source.isDirectory()) {
								Package pkg = ModLoader.class.getPackage();
								if (pkg != null) {
									String packageDirectory = pkg.getName().replace('.', File.separatorChar);
									source = new File(source, packageDirectory);
								}

								LOGGER.finer("Directory found.");
								File[] directoryFiles = source.listFiles();
								if (directoryFiles != null) {
									for (File directoryFile : directoryFiles) {
										String name = directoryFile.getName();
										if (directoryFile.isFile() && name.startsWith("mod_") && name.endsWith(".class")) {
											addMod(loader, name);
										}
									}
								}
							}
						} else {
							LOGGER.finer("Zip found.");
							InputStream input = Files.newInputStream(source.toPath());
							ZipInputStream zip = new ZipInputStream(input);
							ZipEntry entry;

							while (true) {
								entry = zip.getNextEntry();
								if (entry == null) {
									zip.close();
									input.close();
									break;
								}

								String name = entry.getName();
								if (!entry.isDirectory() && name.startsWith("mod_") && name.endsWith(".class")) {
									addMod(loader, name);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Appends all mod key handlers to the given array and returns it.
	 * @param keyBindings Array of the original keys
	 * @return the appended array
	 */
	public static KeyBinding[] RegisterAllKeys(KeyBinding[] keyBindings) {
		List<KeyBinding> combinedList = new LinkedList<>(Arrays.asList(keyBindings));

		for(Map<KeyBinding, boolean[]> keyMap : keyList.values()) {
			combinedList.addAll(keyMap.keySet());
		}

		return combinedList.toArray(new KeyBinding[0]);
	}
	
	/**
	 * Processes all registered texture overrides.
	 * @param manager Reference to texture cache
	 */
	public static void RegisterAllTextureOverrides(TextureManager manager) {
		ANIM_LIST.clear();
		Minecraft game = getMinecraftInstance();

		for(BaseMod mod : MOD_LIST) {
			mod.RegisterAnimation(game);
		}

		for(TextureBinder anim : ANIM_LIST) {
			manager.addTextureBinder(anim);
		}

		for(Entry<Integer, Map<String, Integer>> overlay : overrides.entrySet()) {
			for(Entry<String, Integer> overlayEntry : overlay.getValue().entrySet()) {
				String overlayPath = overlayEntry.getKey();
				int index = overlayEntry.getValue();
				int dst = overlay.getKey();

				try {
					BufferedImage im = loadImage(manager, overlayPath);
					TextureBinder anim = new ModTextureStatic(index, dst, im);
					manager.addTextureBinder(anim);
				} catch (Exception var11) {
					LOGGER.throwing("ModLoader", "RegisterAllTextureOverrides", var11);
					ThrowException(var11);
					throw new RuntimeException(var11);
				}
			}
		}
	}
	
	/**
	 * Adds block to list of blocks the player can use.
	 * @param block to add
	 */
	public static void RegisterBlock(Block block) {
		RegisterBlock(block, null);
	}
	
	/**
	 * Adds block to list of blocks the player can use. Includes the item to use for block (unsafely).
	 * @param block to add
	 * @param itemClass class to use for block item
	 */
	@SuppressWarnings("unchecked")
	public static void RegisterBlock(@NotNull Block block, Class<? extends BlockItem> itemClass) {
		try {
			List<Block> list = (List<Block>) Session.defaultCreativeInventory;
			list.add(block);
			int id = block.id;
			BlockItem item;
			if (itemClass != null) {
				item = itemClass.getConstructor(Integer.TYPE)
						.newInstance(id - 256);
			} else {
				item = new BlockItem(id - 256);
			}

			if (Block.BY_ID[id] != null && Item.byId[id] == null) {
				Item.byId[id] = item;
			}
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException |
				 InvocationTargetException | NoSuchMethodException var5) {
			LOGGER.throwing("ModLoader", "RegisterBlock", var5);
			ThrowException(var5);
		}
	}
	
	/**
	 * Registers an entity ID.
	 * @param entityClass type of entity to register
	 * @param entityName name of entity
	 * @param entityId an arbitrary number that <b>cannot</b> be reused for other entities
	 */
	public static void RegisterEntityID(Class<? extends Entity> entityClass, String entityName, int entityId) {
		try {
			EntityRegistry.register(entityClass, entityName, entityId);
		} catch (IllegalArgumentException var4) {
			LOGGER.throwing("ModLoader", "RegisterEntityID", var4);
			ThrowException(var4);
		}
	}
	
	/**
	 * Use this to add an assignable key to the options menu.
	 * @param mod
	 * @param keyBinding
	 * @param allowRepeat
	 */
	public static void RegisterKey(BaseMod mod, KeyBinding keyBinding, boolean allowRepeat) {
		Map<KeyBinding, boolean[]> keyMap = keyList.get(mod);
		if (keyMap == null) {
			keyMap = new HashMap<>();
		}

		keyMap.put(keyBinding, new boolean[]{allowRepeat, false});
		keyList.put(mod, keyMap);
	}
	
	/**
	 * Registers a tile entity.
	 * @param blockEntityClass
	 * @param id
	 */
	public static void RegisterTileEntity(Class<? extends BlockEntity> blockEntityClass, String id) {
		RegisterTileEntity(blockEntityClass, id, null);
	}
	
	/**
	 * Registers a tile entity.
	 * @param blockEntityClass
	 * @param id
	 * @param renderer
	 */
	public static void RegisterTileEntity(Class<? extends BlockEntity> blockEntityClass, String id, BlockEntityRenderer renderer) {
		try {
			BlockEntity.register(blockEntityClass, id);
			if (renderer != null) {
				BlockEntityRenderDispatcher ref = BlockEntityRenderDispatcher.INSTANCE;
				Map<Class<? extends BlockEntity>, BlockEntityRenderer> renderers = ref.customRenderers;
				renderers.put(blockEntityClass, renderer);
				renderer.setRenderDispatcher(ref);
			}
		} catch (IllegalArgumentException var5) {
			LOGGER.throwing("ModLoader", "RegisterTileEntity", var5);
			ThrowException(var5);
		}
	}
	
	/**
	 * Remove entity from spawn list for all biomes except Hell.
	 * @param entityClass
	 * @param spawnGroup
	 */
	public static void RemoveSpawn(Class<? extends LivingEntity> entityClass, SpawnGroup spawnGroup) {
		RemoveSpawn(entityClass, spawnGroup, (Biome) null);
	}
	
	/**
	 * Remove entity from spawn list for selected biomes.
	 * @param entityClass
	 * @param spawnGroup
	 * @param biomes
	 */
	public static void RemoveSpawn(Class<? extends LivingEntity> entityClass, SpawnGroup spawnGroup, Biome... biomes) {
		if (entityClass == null) {
			throw new IllegalArgumentException("entityClass cannot be null");
		} else if (spawnGroup == null) {
			throw new IllegalArgumentException("spawnList cannot be null");
		} else {
			if (biomes == null) {
				biomes = standardBiomes;
			}

			for (Biome biome : biomes) {
				List<EntityEntry> list = biome.getSpawnList(spawnGroup);
				if (list != null) {
					list.removeIf(entry -> entry.entryClass == entityClass);
				}
			}
		}
	}
	
	/**
	 * Remove entity from spawn list for all biomes except Hell.
	 * @param entityName
	 * @param spawnGroup
	 */
	public static void RemoveSpawn(String entityName, SpawnGroup spawnGroup) {
		RemoveSpawn(entityName, spawnGroup, (Biome) null);
	}
	
	/**
	 * Remove entity from spawn list for selected biomes.
	 * @param entityName
	 * @param spawnGroup
	 * @param biomes
	 */
	@SuppressWarnings("unchecked")
	public static void RemoveSpawn(String entityName, SpawnGroup spawnGroup, Biome... biomes) {
		Class<? extends Entity> entityClass = (Class<? extends Entity>) EntityRegistry.STRING_ID_TO_CLASS.get(entityName);
		if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {
			RemoveSpawn((Class<? extends LivingEntity>) entityClass, spawnGroup, biomes);
		}
	}
	
	/**
	 * Determines how the block should be rendered.
	 * @param modelID
	 * @return
	 */
	public static boolean RenderBlockIsItemFull3D(int modelID) {
		if (!BLOCK_SPECIAL_INV.containsKey(modelID)) {
			return modelID == 16;
		} else {
			return BLOCK_SPECIAL_INV.get(modelID);
		}
	}
	
	/**
	 * Renders a block in inventory.
	 * @param renderer
	 * @param block
	 * @param metadata
	 * @param modelID
	 */
	public static void RenderInvBlock(BlockRenderer renderer, Block block, int metadata, int modelID) {
		BaseMod mod = BLOCK_MODELS.get(modelID);
		if (mod != null) {
			mod.RenderInvBlock(renderer, block, metadata, modelID);
		}
	}
	
	/**
	 * Renders a block in the world.
	 * @param renderer
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param block
	 * @param modelID
	 * @return
	 */
	public static boolean RenderWorldBlock(BlockRenderer renderer, BlockView world, int x, int y, int z, Block block, int modelID) {
		BaseMod mod = (BaseMod) BLOCK_MODELS.get(modelID);
		return mod != null && mod.RenderWorldBlock(renderer, world, x, y, z, block, modelID);
	}
	
	/**
	 * Saves props to the config file.
	 * @throws IOException
	 */
	public static void saveConfig() throws IOException {
		CONFIG_DIR.mkdir();
		if (CONFIG_FILE.exists() || CONFIG_FILE.createNewFile()) {
			if (CONFIG_FILE.canWrite()) {
				OutputStream out = Files.newOutputStream(CONFIG_FILE.toPath());
				props.store(out, "ModLoader Config");
				out.close();
			}
		}
	}
	
	/**
	 * Enable or disable BaseMod.OnTickInGame(net.minecraft.client.Minecraft)
	 * @param mod
	 * @param enable
	 * @param useClock
	 */
	public static void SetInGameHook(BaseMod mod, boolean enable, boolean useClock) {
		if (enable) {
			inGameHooks.put(mod, useClock);
		} else {
			inGameHooks.remove(mod);
		}
	}
	
	/**
	 * Enable or disable BaseMod.OnTickInGUI(net.minecraft.client.Minecraft, da)
	 * @param mod
	 * @param enable
	 * @param useClock
	 */
	public static void SetInGUIHook(BaseMod mod, boolean enable, boolean useClock) {
		if (enable) {
			inGUIHooks.put(mod, useClock);
		} else {
			inGUIHooks.remove(mod);
		}
	}
	
	/**
	 * Used for setting value of private fields.
	 * @param instanceClass
	 * @param instance
	 * @param fieldIndex
	 * @param value
	 * @param <T>
	 * @param <E>
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	@SuppressWarnings("unused")
	public static <T, E> void setPrivateValue(Class<? super T> instanceClass, T instance, int fieldIndex, E value) throws IllegalArgumentException, SecurityException {
		try {
			Field f = instanceClass.getDeclaredFields()[fieldIndex];
			f.setAccessible(true);
			int modifiers = field_modifiers.getInt(f);
			if ((modifiers & 16) != 0) {
				field_modifiers.setInt(f, modifiers & -17);
			}

			f.set(instance, value);
		} catch (IllegalAccessException var6) {
			LOGGER.throwing("ModLoader", "setPrivateValue", var6);
			ThrowException("An impossible error has occured!", var6);
		}

	}
	
	/**
	 * Used for setting value of private fields.
	 * @param instanceClass
	 * @param instance
	 * @param fieldName
	 * @param value
	 * @param <T>
	 * @param <E>
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	public static <T, E> void setPrivateValue(Class<? super T> instanceClass, T instance, String fieldName, E value) throws IllegalArgumentException, SecurityException, NoSuchFieldException {
		try {
			Field f = instanceClass.getDeclaredField(fieldName);
			int modifiers = field_modifiers.getInt(f);
			if ((modifiers & 16) != 0) {
				field_modifiers.setInt(f, modifiers & -17);
			}

			f.setAccessible(true);
			f.set(instance, value);
		} catch (IllegalAccessException var6) {
			LOGGER.throwing("ModLoader", "setPrivateValue", var6);
			ThrowException("An impossible error has occured!", var6);
		}

	}

	private static void setupProperties(Class<? extends BaseMod> mod) throws IllegalArgumentException, IllegalAccessException, IOException, SecurityException {
		Properties modprops = new Properties();
		File modcfgfile = new File(CONFIG_DIR, mod.getName() + ".cfg");
		if (modcfgfile.exists() && modcfgfile.canRead()) {
			modprops.load(Files.newInputStream(modcfgfile.toPath()));
		}

		StringBuilder helptext = new StringBuilder();

		Field[] var7;
		for(Field field : var7 = mod.getFields()) {
			if ((field.getModifiers() & 8) != 0 && field.isAnnotationPresent(MLProp.class)) {
				Class<?> type = field.getType();
				MLProp annotation = field.getAnnotation(MLProp.class);
				String key = annotation.name().length() == 0 ? field.getName() : annotation.name();
				Object currentvalue = field.get(null);
				StringBuilder range = new StringBuilder();
				if (annotation.min() != Double.NEGATIVE_INFINITY) {
					range.append(String.format(",>=%.1f", annotation.min()));
				}

				if (annotation.max() != Double.POSITIVE_INFINITY) {
					range.append(String.format(",<=%.1f", annotation.max()));
				}

				StringBuilder info = new StringBuilder();
				if (annotation.info().length() > 0) {
					info.append(" -- ");
					info.append(annotation.info());
				}

				helptext.append(String.format("%s (%s:%s%s)%s\n", key, type.getName(), currentvalue, range, info));
				if (modprops.containsKey(key)) {
					String strvalue = modprops.getProperty(key);
					Object value = null;
					if (type.isAssignableFrom(String.class)) {
						value = strvalue;
					} else if (type.isAssignableFrom(Integer.TYPE)) {
						value = Integer.parseInt(strvalue);
					} else if (type.isAssignableFrom(Short.TYPE)) {
						value = Short.parseShort(strvalue);
					} else if (type.isAssignableFrom(Byte.TYPE)) {
						value = Byte.parseByte(strvalue);
					} else if (type.isAssignableFrom(Boolean.TYPE)) {
						value = Boolean.parseBoolean(strvalue);
					} else if (type.isAssignableFrom(Float.TYPE)) {
						value = Float.parseFloat(strvalue);
					} else if (type.isAssignableFrom(Double.TYPE)) {
						value = Double.parseDouble(strvalue);
					}

					if (value != null) {
						if (value instanceof Number) {
							double num = ((Number)value).doubleValue();
							if (annotation.min() != Double.NEGATIVE_INFINITY && num < annotation.min()
									|| annotation.max() != Double.POSITIVE_INFINITY && num > annotation.max()) {
								continue;
							}
						}

						LOGGER.finer(key + " set to " + value);
						if (!value.equals(currentvalue)) {
							field.set(null, value);
						}
					}
				} else {
					LOGGER.finer(key + " not in config, using default: " + currentvalue);
					modprops.setProperty(key, currentvalue.toString());
				}
			}
		}

		if (!modprops.isEmpty() && (modcfgfile.exists() || modcfgfile.createNewFile()) && modcfgfile.canWrite()) {
			modprops.store(Files.newOutputStream(modcfgfile.toPath()), helptext.toString());
		}
	}
	
	/**
	 * Is called when an item is picked up from crafting result slot.
	 * @param player
	 * @param item
	 */
	public static void TakenFromCrafting(PlayerEntity player, ItemStack item) {
		for(BaseMod mod : MOD_LIST) {
			mod.TakenFromCrafting(player, item);
		}
	}
	
	/**
	 * Is called when an item is picked up from furnace result slot.
	 * @param player
	 * @param item
	 */
	public static void TakenFromFurnace(PlayerEntity player, ItemStack item) {
		for(BaseMod mod : MOD_LIST) {
			mod.TakenFromFurnace(player, item);
		}
	}
	
	/**
	 * Used for catching an error and generating an error report.
	 * @param message
	 * @param e
	 */
	public static void ThrowException(String message, Throwable e) {
		Minecraft game = getMinecraftInstance();
		if (game != null) {
			game.showGameStartupError(new GameStartupError(message, e));
		} else {
			throw new RuntimeException(e);
		}
	}

	private static void ThrowException(Throwable e) {
		ThrowException("Exception occured in ModLoader", e);
	}

	private ModLoader() {
	}
}
