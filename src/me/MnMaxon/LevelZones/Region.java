package me.MnMaxon.LevelZones;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

public class Region {
	public Location min = null;
	public Location max = null;
	public int minLevel = 0;
	public int maxLevel = 0;
	private String regionName = "";
	public String path = null;
	public ArrayList<EntityType> WhiteListedEntities = new ArrayList<EntityType>();
	public ArrayList<EntityType> BlackListedEntities = new ArrayList<EntityType>();
	public static ArrayList<EntityType> entityTypes = new ArrayList<EntityType>();

	public Region(Location point1, Location point2, int level1, int level2, String name) {
		int minX = 0;
		int minY = 0;
		int minZ = 0;
		int maxX = 0;
		int maxY = 0;
		int maxZ = 0;
		if (point1.getBlockX() < point2.getBlockX()) {
			minX = point1.getBlockX();
			maxX = point2.getBlockX();
		} else {
			minX = point2.getBlockX();
			maxX = point1.getBlockX();
		}
		if (point1.getBlockY() < point2.getBlockY()) {
			minY = point1.getBlockY();
			maxY = point2.getBlockY();
		} else {
			minY = point2.getBlockY();
			maxY = point1.getBlockY();
		}
		if (point1.getBlockZ() < point2.getBlockZ()) {
			minZ = point1.getBlockZ();
			maxZ = point2.getBlockZ();
		} else {
			minZ = point2.getBlockZ();
			maxZ = point1.getBlockZ();
		}
		min = new Location(point1.getWorld(), minX, minY, minZ);
		max = new Location(point1.getWorld(), maxX, maxY, maxZ);
		regionName = name;
		path = Main.dataFolder + "/Regions/" + name + ".yml";
		if (level1 < level2) {
			minLevel = level1;
			maxLevel = level2;
		} else {
			maxLevel = level1;
			minLevel = level2;
		}
		WhiteListedEntities = entityTypes;
	}

	public void save() {
		YamlConfiguration cfg = Config.Load(path);
		cfg.set("Name", getName());
		cfg.set("World", min.getWorld().getName());
		cfg.set("Min.X", min.getBlockX());
		cfg.set("Min.Y", min.getBlockY());
		cfg.set("Min.Z", min.getBlockZ());
		cfg.set("Max.X", max.getBlockX());
		cfg.set("Max.Y", max.getBlockY());
		cfg.set("Max.Z", max.getBlockZ());
		cfg.set("MaxLevel", maxLevel);
		cfg.set("MinLevel", minLevel);
		for (EntityType type : WhiteListedEntities)
			cfg.set("Mobs." + type.name(), true);
		for (EntityType type : BlackListedEntities)
			cfg.set("Mobs." + type.name(), false);
		Config.Save(cfg, path);
	}

	public Boolean delete() {
		return delete(getName());
	}

	public static void load(String name) {
		// TODO
		name = name.replace(".yml", "");
		String path = Main.dataFolder + "/Regions/" + name + ".yml";
		YamlConfiguration cfg = Config.Load(path);
		World world = Bukkit.getWorld(cfg.getString("World"));
		if (world == null)
			return;
		Location minLoc = new Location(world, cfg.getInt("Min.X"), cfg.getInt("Min.Y"), cfg.getInt("Min.Z"));
		Location maxLoc = new Location(world, cfg.getInt("Max.X"), cfg.getInt("Max.Y"), cfg.getInt("Max.Z"));
		Region region = new Region(minLoc, maxLoc, cfg.getInt("MinLevel"), cfg.getInt("MaxLevel"), name);
		region.WhiteListedEntities = new ArrayList<EntityType>();
		for (EntityType type : entityTypes)
			if (cfg.getBoolean("Mobs." + type.name()))
				region.WhiteListedEntities.add(type);
			else
				region.BlackListedEntities.add(type);
		Main.stringMatcher.remove(name.toLowerCase());
		Main.stringMatcher.put(name.toLowerCase(), region);
	}

	public static Boolean delete(String name) {
		if (matchByName(name) == null)
			return false;
		name = matchByName(name).getName();
		String path = Main.dataFolder + "/Regions/" + name + ".yml";
		return new File(path).delete();
	}

	public static Region matchByName(String name) {
		return Main.stringMatcher.get(name.toLowerCase());
	}

	public boolean contains(Location loc) {
		if (loc.getWorld().equals(min.getWorld()) && min.getBlockX() <= loc.getBlockX()
				&& min.getBlockY() <= loc.getBlockY() && min.getBlockZ() <= loc.getBlockZ()
				&& max.getBlockX() >= loc.getBlockX() && max.getBlockY() >= loc.getBlockY()
				&& max.getBlockZ() >= loc.getBlockZ())
			return true;
		return false;
	}

	public static ArrayList<Region> matchByName(List<String> list) {
		ArrayList<Region> regions = new ArrayList<Region>();
		for (String s : list) {
			Region region = matchByName(s);
			if (region != null)
				regions.add(region);
		}
		return regions;
	}

	public String getName() {
		return regionName;
	}
}
