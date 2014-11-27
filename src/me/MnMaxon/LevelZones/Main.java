package me.MnMaxon.LevelZones;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	public static String dataFolder;
	public static Main plugin;
	public static SuperYaml MainConfig;
	public static SuperYaml Chunks;
	public static final BlockFace[] axis = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	public static final ItemStack wand = new ItemStack(Material.BLAZE_ROD);
	public static HashMap<Chunk, ArrayList<String>> chunkRegions = new HashMap<Chunk, ArrayList<String>>();
	public static HashMap<Integer, ItemListInfo> ItemList = new HashMap<Integer, ItemListInfo>();
	public static HashMap<String, Region> stringMatcher = new HashMap<String, Region>();
	public static double extraDamage;
	public static double extraHealth;
	public static double extraExperience;
	public static double variation;
	public static int maxPages = 5;
	public static int greatestNumber = -1;

	@Override
	public void onEnable() {
		ItemMeta im = wand.getItemMeta();
		im.setDisplayName(ChatColor.RED + "LZ Wand");
		List<String> lore = new ArrayList<String>();
		lore.add("Left Click to set point 1");
		lore.add("Right Click to set point 2");
		im.setLore(lore);
		wand.setItemMeta(im);

		Region.entityTypes = new ArrayList<EntityType>(Arrays.asList(EntityType.BLAZE, EntityType.CAVE_SPIDER,
				EntityType.CREEPER, EntityType.ENDER_DRAGON, EntityType.ENDERMAN, EntityType.GHAST, EntityType.GIANT,
				EntityType.IRON_GOLEM, EntityType.MAGMA_CUBE, EntityType.OCELOT, EntityType.PIG_ZOMBIE,
				EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.WITCH,
				EntityType.WITHER, EntityType.WOLF, EntityType.ZOMBIE));

		plugin = this;
		dataFolder = this.getDataFolder().getAbsolutePath();
		reloadConfigs();
		getServer().getPluginManager().registerEvents(new MainListener(), this);
	}

	public static void reloadConfigs() {
		MainConfig = new SuperYaml(dataFolder + "/Config.yml");
		boolean save = false;
		if (MainConfig.get("Extra Damage Per Level") == null) {
			MainConfig.set("Extra Damage Per Level", .25);
			save = true;
		}
		if (MainConfig.get("Extra Health Per Level") == null) {
			MainConfig.set("Extra Health Per Level", .5);
			save = true;
		}
		if (MainConfig.get("Extra Experience Per Level") == null) {
			MainConfig.set("Extra Experience Per Level", 1);
			save = true;
		}
		if (MainConfig.get("Variation") == null) {
			MainConfig.set("Variation", 1);
			save = true;
		}
		ItemList = new HashMap<Integer, ItemListInfo>();
		greatestNumber = -1;
		for (int x = 0; x < maxPages * 45; x++) {
			String filePath = dataFolder + "/Items/" + x + ".yml";
			if (new File(filePath).exists()) {
				SuperYaml itemYaml = new SuperYaml(filePath);
				ItemStack[] items = new ItemStack[53];
				int i = 0;
				for (String itemName : itemYaml.getConfigurationSection("Items").getKeys(false)) {
					items[i] = itemYaml.getItemStack("Items." + itemName);
					i++;
				}
				ItemList.put(x, new ItemListInfo(x, x, itemYaml.getDouble("Chance"), items));
				greatestNumber = x;
			} else
				ItemList.put(x, new ItemListInfo(x, greatestNumber));
		}
		if (save)
			MainConfig.save();
		extraDamage = MainConfig.getDouble("Extra Damage Per Level") * 2;
		extraHealth = MainConfig.getDouble("Extra Health Per Level") * 2;
		extraExperience = MainConfig.getDouble("Extra Experience Per Level") * 20.0;
		variation = MainConfig.getDouble("Variation") * 2;
		Chunks = new SuperYaml(dataFolder + "/Chunks.yml");
		chunkRegions = new HashMap<Chunk, ArrayList<String>>();
		stringMatcher = new HashMap<String, Region>();
		File folder = new File(dataFolder + "/Regions");
		if (!folder.exists())
			folder.mkdir();
		for (File file : folder.listFiles())
			if (file.getName().endsWith(".yml"))
				Region.load(file.getName());
		if (Chunks.getConfigurationSection("Chunks") != null)
			for (String worldName : Chunks.getConfigurationSection("Chunks").getKeys(false))
				for (String chunkPath : Chunks.getConfigurationSection("Chunks." + worldName).getKeys(false)) {
					String path = "Chunks." + worldName + "." + chunkPath;
					chunkPath = chunkPath.replace("[", "").replace("]", "").replace(" ", "");
					// SAVE THIS LIKE [x10, z5]
					ArrayList<String> xAndZ = new ArrayList<String>(Arrays.asList(chunkPath.split(",")));
					int x = 0;
					int z = 0;
					for (String s : xAndZ)
						if (s.contains("x"))
							x = Integer.parseInt(s.replace("x", ""));
						else if (s.contains("z"))
							z = Integer.parseInt(s.replace("z", ""));
					World world = Bukkit.getServer().getWorld(worldName);
					Chunk chunk = world.getChunkAt(x, z);
					if (!chunkRegions.containsKey(chunk)) {
						ArrayList<String> rList = new ArrayList<String>();
						for (String rName : Chunks.config.getStringList(path))
							if (Region.matchByName(rName) != null)
								rList.add(Region.matchByName(rName).getName());
						if (!rList.isEmpty())
							chunkRegions.put(chunk, rList);
					}
				}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			displayHelp(sender);
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You need to be a player to do this!");
			return false;
		}
		Player p = (Player) sender;
		if (args[0].equalsIgnoreCase("wand")) {

			if (!p.hasPermission("LevelZone.create")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (p.getInventory().firstEmpty() == -1)
				p.sendMessage(ChatColor.RED + "You're inventory is full");
			else
				p.getInventory().addItem(wand);

		} else if (args[0].equalsIgnoreCase("create")) {

			if (!p.hasPermission("LevelZone.create")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (!MetaLists.pointOne.contains(p) || !MetaLists.pointTwo.contains(p)) {
				p.sendMessage(ChatColor.RED + "You must set two points with the wand first!");
				p.sendMessage(ChatColor.RED + "For the wand, type: /LZ Wand");
			} else if (args.length != 4) {
				p.sendMessage(ChatColor.RED + "Use like: /LZ Create [Area_Name] [MinLevel] [MaxLevel]");
			} else if (stringMatcher.containsKey(args[1])) {
				p.sendMessage(ChatColor.RED + "There is already a zone with that name");
			} else {
				Location loc1 = (Location) MetaLists.pointOne.get(p);
				Location loc2 = (Location) MetaLists.pointTwo.get(p);
				if (!loc1.getWorld().equals(loc2.getWorld())) {
					p.sendMessage(ChatColor.RED + "Your selection points are in two different world!");
					return false;
				}
				String ArenaName = args[1];
				int minLevel;
				int maxLevel;
				try {
					minLevel = Integer.parseInt(args[2]);
					maxLevel = Integer.parseInt(args[3]);
					if (maxLevel < 0 || minLevel < 0) {
						p.sendMessage(ChatColor.RED + "Use like: /LZ Create [Area_Name] [MinLevel] [MaxLevel]");
						p.sendMessage(ChatColor.RED + "MinLevel and MaxLevel must be positive integers (or 0)");
						return false;
					}
				} catch (NumberFormatException ex) {
					p.sendMessage(ChatColor.RED + "Use like: /LZ Create [Area_Name] [MinLevel] [MaxLevel]");
					p.sendMessage(ChatColor.RED + "MinLevel and MaxLevel must be positive integers (or 0)");
					return false;
				}
				Region region = new Region(loc1, loc2, minLevel, maxLevel, ArenaName);
				region.save();
				Main.stringMatcher.remove(ArenaName.toLowerCase());
				Main.stringMatcher.put(ArenaName.toLowerCase(), region);
				Location from = region.min;
				Location to = region.min;
				to = to.getBlock().getRelative(-1, 0, 0).getLocation();
				ArrayList<Chunk> chunks = new ArrayList<Chunk>();
				for (int x = region.min.getBlockX(); x <= region.max.getBlockX() + 1; x++) {
					from = to;
					to = from.getBlock().getRelative(1, 0, 0).getLocation();
					if (!chunks.contains(to.getChunk()))
						chunks.add(to.getChunk());
				}
				from = region.min;
				to = region.min;
				to = to.getBlock().getRelative(0, 0, -1).getLocation();
				for (int z = region.min.getBlockZ(); z <= region.max.getBlockZ() + 1; z++) {
					from = to;
					to = from.getBlock().getRelative(0, 0, 1).getLocation();
					if (!chunks.contains(to.getChunk()))
						chunks.add(to.getChunk());
				}
				for (Chunk c : chunks) {
					String path = "Chunks." + c.getWorld().getName() + ".[x" + c.getX() + ", z" + c.getZ() + "]";
					ArrayList<String> list = new ArrayList<String>();
					if (Chunks.get("Chunks") != null)
						list = new ArrayList<String>(Chunks.getStringList(path));
					{
						ArrayList<String> newList = new ArrayList<String>();
						for (String name : list)
							newList.add(Region.matchByName(name).getName());
						list = newList;
					}
					list.add(ArenaName);
					Chunks.set(path, list);
					Main.chunkRegions.remove(c);
					Main.chunkRegions.put(c, list);
				}
				Chunks.save();
				p.sendMessage(ChatColor.GREEN + "Arena: " + ChatColor.DARK_AQUA + ArenaName + ChatColor.GREEN
						+ " successfully set with a level range from " + ChatColor.DARK_AQUA + minLevel + "-"
						+ maxLevel);
			}

		} else if (args[0].equalsIgnoreCase("edit")) {

			if (!p.hasPermission("LevelZone.edit")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (args.length != 2)
				p.sendMessage(ChatColor.RED + "Use like: /LZ Edit [Area_Name]");
			else {
				Region region = Region.matchByName(args[1]);
				if (region == null) {
					p.sendMessage(ChatColor.RED + args[1] + " could not be found");
					p.sendMessage(ChatColor.RED + args[1] + " Type /LK List  for a list of Areas");
					return false;
				}
				p.openInventory(getRegionGui(region));
			}

		} else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
			if (!p.hasPermission("LevelZone.edit")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (args.length != 2)
				p.sendMessage(ChatColor.RED + "Use like: /LK Remove [Area_Name]");
			else {
				Region region = Region.matchByName(args[1]);
				if (region == null) {
					p.sendMessage(ChatColor.RED + args[1] + " could not be found");
					p.sendMessage(ChatColor.RED + args[1] + " Type /LK List  for a list of Areas");
					return false;
				}
				if (region.delete())
					p.sendMessage(ChatColor.GREEN + "Delete successful!");
				else
					p.sendMessage(ChatColor.RED + "Something went wrong...");
				stringMatcher.remove(Region.matchByName(args[1]).getName());
			}

		} else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("cfg")) {

			if (!p.hasPermission("LevelZone.edit")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (args.length != 1)
				p.sendMessage(ChatColor.RED + "Use like: /LZ Config");
			else
				p.openInventory(getConfigGui());

		} else if (args[0].equalsIgnoreCase("expand")) {

			if (!p.hasPermission("LevelZone.expand")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else if (!MetaLists.pointOne.contains(p) || !MetaLists.pointTwo.contains(p)) {
				p.sendMessage(ChatColor.RED + "You must set two points before you can do this!");
			} else if (args.length != 3) {
				p.sendMessage(ChatColor.RED + "Use like: /LZ Expand [North/South/East/West/Up/Down] [Amount]");
			} else {
				Location loc1 = (Location) MetaLists.pointOne.get(p);
				Location loc2 = (Location) MetaLists.pointTwo.get(p);
				int amount = 0;
				try {
					amount = Integer.parseInt(args[2]);
				} catch (NumberFormatException ex) {
					p.sendMessage(ChatColor.RED + "Use like: /LZ Expand [North/South/East/West/Up/Down] [Amount]");
					p.sendMessage(ChatColor.RED + "[Amount] has to be a positive number!");
					return false;
				}
				if (amount < 1) {
					p.sendMessage(ChatColor.RED + "Use like: /LZ Expand [North/South/East/West/Up/Down] [Amount]");
					p.sendMessage(ChatColor.RED + "[Amount] has to be a positive number!");
					return false;
				}
				if (args[1].equalsIgnoreCase("north") || args[1].equalsIgnoreCase("n")) {
					if (loc1.getZ() < loc2.getZ())
						MetaLists.pointTwo.add(p, loc2.add(0, 0, amount));
					else
						MetaLists.pointOne.add(p, loc1.add(0, 0, amount));
				} else if (args[1].equalsIgnoreCase("south") || args[1].equalsIgnoreCase("s")) {
					if (loc1.getZ() > loc2.getZ())
						MetaLists.pointTwo.add(p, loc2.add(0, 0, -amount));
					else
						MetaLists.pointOne.add(p, loc1.add(0, 0, -amount));
				} else if (args[1].equalsIgnoreCase("east") || args[1].equalsIgnoreCase("e")) {
					if (loc1.getX() < loc2.getX())
						MetaLists.pointTwo.add(p, loc2.add(amount, 0, 0));
					else
						MetaLists.pointOne.add(p, loc1.add(amount, 0, 0));
				} else if (args[1].equalsIgnoreCase("west") || args[1].equalsIgnoreCase("w")) {
					if (loc1.getX() > loc2.getX())
						MetaLists.pointTwo.add(p, loc2.add(-amount, 0, 0));
					else
						MetaLists.pointOne.add(p, loc1.add(-amount, 0, 0));
				} else if (args[1].equalsIgnoreCase("up") || args[1].equalsIgnoreCase("u")) {
					if (loc1.getY() < loc2.getY())
						MetaLists.pointTwo.add(p, loc2.add(0, amount, 0));
					else
						MetaLists.pointOne.add(p, loc1.add(0, amount, 0));
				} else if (args[1].equalsIgnoreCase("down") || args[1].equalsIgnoreCase("d")) {
					if (loc1.getY() < loc2.getY())
						MetaLists.pointOne.add(p, loc1.add(0, -amount, 0));
					else
						MetaLists.pointTwo.add(p, loc2.add(0, -amount, 0));
				} else {
					p.sendMessage(ChatColor.RED + "Use like: /LZ Expand [North/South/East/West/Up/Down] [Amount]");
					return false;
				}
				p.sendMessage(ChatColor.GREEN + "Success!");
			}

		} else if (args[0].equalsIgnoreCase("list")) {

			boolean first = true;
			String message = "";
			int amount = 0;
			for (String name : stringMatcher.keySet()) {
				if (!first)
					name = ", " + name;
				first = false;
				message = message + name;
				amount++;
			}
			message = ChatColor.GREEN + "Current Zones (" + amount + "):" + message;
			p.sendMessage(message);

		} else if (args[0].equalsIgnoreCase("pos1")) {

			if (!p.hasPermission("LevelZone.pos")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else {
				Location loc = p.getLocation().getBlock().getLocation();
				MetaLists.pointOne.add(p, loc);
				p.sendMessage(ChatColor.GREEN + "Point one set at: " + loc.getX() + ", " + loc.getY() + ", "
						+ loc.getZ());
			}

		} else if (args[0].equalsIgnoreCase("pos2")) {

			if (!p.hasPermission("LevelZone.pos")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			} else {
				Location loc = p.getLocation().getBlock().getLocation();
				MetaLists.pointTwo.add(p, loc);
				p.sendMessage(ChatColor.GREEN + "Point two set at: " + loc.getX() + ", " + loc.getY() + ", "
						+ loc.getZ());
			}

		} else if (args[0].equalsIgnoreCase("direction")) {

			p.sendMessage(ChatColor.DARK_AQUA + "You are facing "
					+ (axis[Math.round(p.getLocation().getYaw() / 90f) & 0x3]));

		} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {

			reloadConfigs();
			p.sendMessage(ChatColor.GREEN + "Config reloaded!");

		} else
			displayHelp(p);
		return false;
	}

	private Inventory getRegionGui(Region region) {
		Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Editor: " + region.getName());
		ItemStack[] contents = inv.getContents();
		int i = 0;
		for (EntityType entType : Region.entityTypes) {
			short data = 14;
			if (region.WhiteListedEntities.contains(entType))
				data = 5;
			ItemStack is = new ItemStack(Material.WOOL);
			is.setDurability(data);
			ItemMeta im = is.getItemMeta();
			im.setDisplayName(ChatColor.AQUA + entType.name());
			is.setItemMeta(im);
			contents[i] = is;
			i++;
		}
		contents[45] = easyName(Material.WOOD_PICKAXE, ChatColor.RED + "Exit Without Saving", null);
		contents[53] = easyName(Material.DIAMOND_PICKAXE, ChatColor.GREEN + "Exit and Save", null);
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Left click to increase");
		lore.add("Right click to decrease");
		contents[48] = easyName(Material.WOOD_SWORD, ChatColor.GREEN + "Min Level (" + region.minLevel + ")", lore);
		contents[49] = easyName(Material.DIAMOND_SWORD, ChatColor.GREEN + "Max Level (" + region.maxLevel + ")", lore);
		inv.setContents(contents);
		return inv;
	}

	private Inventory getConfigGui() {
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GREEN + "Config");
		ItemStack[] contents = inv.getContents();

		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Left click to increase");
		lore.add("Right click to decrease");
		lore.add("Shift click to change by 1");

		contents[0] = easyName(Material.DIAMOND_SPADE, ChatColor.GREEN + "Variation (" + variation + ")", lore);
		contents[1] = easyName(Material.DIAMOND_SWORD, ChatColor.GREEN + "Extra Damage (" + extraDamage + ")", lore);
		ItemStack pot = easyName(Material.POTION, ChatColor.GREEN + "Extra Health (" + extraHealth + ")", lore);
		pot.setDurability((short) 8261);
		contents[2] = pot;
		contents[3] = easyName(Material.EXP_BOTTLE, ChatColor.GREEN + "Extra Experience (" + extraExperience + ")",
				lore);
		contents[5] = easyName(Material.BEACON, ChatColor.GREEN + "Items", null);
		contents[7] = easyName(Material.WOOD_PICKAXE, ChatColor.RED + "Exit Without Saving", null);
		contents[8] = easyName(Material.DIAMOND_PICKAXE, ChatColor.GREEN + "Exit and Save", null);

		inv.setContents(contents);
		return inv;
	}

	private static ItemStack easyName(Material type, String name, ArrayList<String> lore) {
		ItemStack is = new ItemStack(type);
		ItemMeta im = is.getItemMeta();
		if (name != null)
			im.setDisplayName(name);
		if (lore != null)
			im.setLore(lore);
		is.setItemMeta(im);
		return is;
	}

	private void displayHelp(CommandSender s) {
		s.sendMessage(ChatColor.AQUA + "========= Level Zone =========");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Reload");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Config");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Wand");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Create [Area_Name] [MinLevel] [MaxLevel]");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Edit [Area_Name]");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Remove [Area_Name]");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Expand [North/South/East/West/Up/Down] [Amount]");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ Direction");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ list");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ pos1");
		s.sendMessage(ChatColor.DARK_PURPLE + "/LZ pos2");
	}

	public static Inventory getItemInventory() {
		Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Items");
		inv.setContents(getItemContents(1));
		return inv;
	}

	public static ItemStack[] getItemContents(int page) {
		if (page > maxPages)
			page = maxPages;
		Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Items");
		ItemStack[] contents = inv.getContents();
		for (int x = (page - 1) * 45; x < page * 45; x++) {
			ItemStack item = easyName(Material.WOOL, ChatColor.BLUE + "Level " + x, null);
			if (ItemListInfo.getStrict(x) == x)
				item.setDurability((short) 5);
			contents[x - ((page - 1) * 45)] = item;
		}
		if (page != 1)
			contents[45] = easyName(Material.ARROW, ChatColor.BLUE + "Page " + (page - 1), null);
		if (page != maxPages)
			contents[53] = easyName(Material.ARROW, ChatColor.BLUE + "Page " + (page + 1), null);
		return contents;
	}

	public static Inventory getStrictItemInventory(int level) {
		Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Level " + level);
		ItemListInfo iLI = ItemListInfo.get(level);
		ItemStack[] contents = inv.getContents();
		contents[52] = easyName(Material.WOOD_PICKAXE, ChatColor.RED + "Exit Without Saving", null);
		contents[53] = easyName(Material.DIAMOND_PICKAXE, ChatColor.GREEN + "Exit and Save", null);
		contents[0] = easyName(Material.BLAZE_ROD, ChatColor.GREEN + "Chance (" + iLI.getStrictPercent() + ")", null);
		inv.setContents(contents);
		for (int x = 0; x < iLI.getStrictItems().length - 1; x++)
			if (iLI.getStrictItems()[x] != null && inv.firstEmpty() != -1)
				inv.setItem(inv.firstEmpty(), iLI.getStrictItems()[x]);
		return inv;
	}
}