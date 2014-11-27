package me.MnMaxon.LevelZones;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class ItemListInfo {
	public int level = -1;
	public int strict = -1;
	public double percent = 5.0;
	private ItemStack[] items = null;

	ItemListInfo(int level, int strict, double percent, ItemStack[] items) {
		this.level = level;
		this.strict = strict;
		this.percent = percent;
		this.items = items;
	}

	ItemListInfo(int level, int strict) {
		this.level = level;
		this.strict = strict;
	}

	public ItemStack[] getItems() {
		if (level != strict)
			return getItems(level - 1);
		if (items == null)
			return new ItemStack[5];
		return items;
	}

	public int getLevel() {
		return level;
	}

	public int getStrict() {
		return strict;
	}

	public double getStrictPercent() {
		return percent;
	}

	public double getPercent() {
		if (level != strict) {
			Bukkit.broadcastMessage("1: " + level);
			return getPercent(level - 1);
		}
		Bukkit.broadcastMessage("2: " + level + " ~ " + percent);
		return percent;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setStrict(int level) {
		strict = level;
	}

	public void setPercent(double chance) {
		percent = chance;
	}

	public void setItems(ItemStack[] items) {
		this.items = items;
	}

	public ItemStack[] getStrictItems() {
		if (items == null)
			return new ItemStack[5];
		return items;
	}

	public static int getStrict(int level) {
		return Main.ItemList.get(level).getStrict();
	}

	public static ItemStack[] getStrictItems(int level) {
		return Main.ItemList.get(level).getStrictItems();
	}

	public static double getPercent(int level) {
		return Main.ItemList.get(level).getPercent();
	}

	public static double getStrictPercent(int level) {
		return Main.ItemList.get(level).getStrictPercent();
	}

	public static ItemStack[] getItems(int level) {
		return Main.ItemList.get(level).getItems();
	}

	public static ItemListInfo get(int level) {
		return Main.ItemList.get(level);
	}

	public void save() {
		File file = new File(Main.dataFolder + "/Items/" + level + ".yml");
		if (items == null && file.exists()) {
			this.strict = this.level - 1;
			file.delete();
		} else {
			this.strict = this.level;
			YamlConfiguration cfg = Config.Load(file.getPath());
			for (int x = 0; x < 53; x++)
				if (items[x] != null)
					cfg.set("Items." + x, items[x]);
			cfg.set("Chance", percent);
			try {
				cfg.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
