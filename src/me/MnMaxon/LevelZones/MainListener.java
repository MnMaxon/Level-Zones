package me.MnMaxon.LevelZones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainListener implements Listener {
	@EventHandler
	public void onSpawn(CreatureSpawnEvent e) {
		if (Main.chunkRegions.containsKey(e.getLocation().getChunk())) {
			Region reg = null;
			for (Region region : Region.matchByName(Main.chunkRegions.get(e.getLocation().getChunk()))) {
				if (region.contains(e.getLocation()))
					reg = region;
			}
			if (reg != null) {
				if (reg.BlackListedEntities.contains(e.getEntity().getType())) {
					e.setCancelled(true);
					return;
				} else if (!reg.WhiteListedEntities.contains(e.getEntity().getType()))
					return;
				int level = new Random().nextInt(reg.maxLevel - reg.minLevel + 1) + reg.minLevel;
				e.getEntity().setMaxHealth(
						e.getEntity().getMaxHealth() + Main.extraHealth * level
								+ (new Random().nextDouble() * (Main.variation * 2)) - Main.variation);
				e.getEntity().setHealth(e.getEntity().getMaxHealth());
				e.getEntity().setCustomName(
						ChatColor.GREEN + "[lvl " + level + "] " + ChatColor.RED
								+ simpleHealth(e.getEntity().getHealth()) + "HP");
			}
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() != null && e.getItem().equals(Main.wand)) {
			if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				MetaLists.pointTwo.add(e.getPlayer(), e.getClickedBlock().getLocation());
				e.getPlayer().sendMessage(
						ChatColor.GREEN + "Point two set at: " + e.getClickedBlock().getX() + ", "
								+ e.getClickedBlock().getY() + ", " + e.getClickedBlock().getZ());
				e.setCancelled(true);
			} else if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				MetaLists.pointOne.add(e.getPlayer(), e.getClickedBlock().getLocation());
				e.getPlayer().sendMessage(
						ChatColor.GREEN + "Point one set at: " + e.getClickedBlock().getX() + ", "
								+ e.getClickedBlock().getY() + ", " + e.getClickedBlock().getZ());
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof LivingEntity && e.getEntity().isValid()) {
			LivingEntity ent = (LivingEntity) e.getEntity();
			if (ent.getCustomName() != null && ent.getCustomName().contains(ChatColor.GREEN + "[lvl ")) {
				int level = getLevel(ent);
				if (level == -1) {
					Bukkit.getLogger().log(Level.WARNING, "[LZ] Error at getLevel()  (tell the developer, MnMaxon)");
					return;
				}
				ent.setCustomName(ChatColor.GREEN + "[lvl " + level + "] " + ChatColor.RED
						+ simpleHealth(ent.getHealth() - e.getDamage()) + "HP");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHit(EntityDamageByEntityEvent e) {
		LivingEntity ent = null;
		if (e.getDamager() instanceof Projectile) {
			if (((Projectile) e.getDamager()).getShooter() instanceof LivingEntity)
				ent = (LivingEntity) ((Projectile) e.getDamager()).getShooter();
		} else if (e.getDamager() instanceof LivingEntity)
			ent = (LivingEntity) e.getDamager();
		if (ent == null)
			return;
		if (ent.getCustomName() != null && ent.getCustomName().contains(ChatColor.GREEN + "[lvl ")) {
			int level = getLevel(ent);
			if (level == -1) {
				Bukkit.getLogger().log(Level.WARNING, "[LZ] Error at getLevel()  (tell the developer, MnMaxon)");
				return;
			}
			e.setDamage(e.getDamage() + (Main.extraDamage * level));
		}
		if (ent.isValid() && ent.getCustomName() != null && ent.getCustomName().contains(ChatColor.GREEN + "[lvl ")) {
			int level = getLevel(ent);
			if (level == -1) {
				Bukkit.getLogger().log(Level.WARNING, "[LZ] Error at getLevel()  (tell the developer, MnMaxon)");
				return;
			}
			ent.setCustomName(ChatColor.GREEN + "[lvl " + level + "] " + ChatColor.RED
					+ simpleHealth(ent.getHealth() - e.getDamage()) + "HP");
		}
	}

	public static int getLevel(LivingEntity ent) {
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(ChatColor.stripColor(ent.getCustomName()).split(
				"] ")));
		for (String part : parts)
			if (part.contains("[lvl ")) {
				part = part.replace("[lvl ", "");
				try {
					return Integer.parseInt(part);
				} catch (NumberFormatException ex) {
					Bukkit.getLogger().log(Level.WARNING,
							"[LZ] Error at getLevel() FORMAT EXCEPTION (tell the developer, MnMaxon)");
					return -1;
				}
			}
		return -1;
	}

	public static double simpleHealth(double i) {
		i = i / 2;
		i = Math.round(i * 10) / 10.0;
		return i;
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getInventory().getName().contains(ChatColor.GREEN + "Editor: ")) {
			e.setCancelled(true);
			Region region = Region
					.matchByName(ChatColor.stripColor(e.getInventory().getName()).replace("Editor: ", ""));
			if (region == null)
				return;
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				EntityType type = null;
				try {
					type = EntityType.valueOf(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()));
				} catch (IllegalArgumentException ex) {
				}
				if (type != null) {
					if (e.getCurrentItem().getDurability() == 14)
						e.getCurrentItem().setDurability((short) 5);
					else
						e.getCurrentItem().setDurability((short) 14);
				} else {
					if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Exit and Save")) {
						region.BlackListedEntities = new ArrayList<EntityType>();
						region.WhiteListedEntities = new ArrayList<EntityType>();
						int level1 = 0;
						int level2 = 0;
						for (int i = 0; i < 53; i++) {
							ItemStack is = e.getInventory().getContents()[i];
							if (is != null && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
								EntityType entType = null;
								try {
									entType = EntityType.valueOf(ChatColor
											.stripColor(is.getItemMeta().getDisplayName()));
								} catch (IllegalArgumentException ex) {
								}
								if (entType != null) {
									if (is.getDurability() == 14)
										region.BlackListedEntities.add(entType);
									else
										region.WhiteListedEntities.add(entType);
								}
								if (is.getItemMeta().getDisplayName().contains("Min Level ("))
									level1 = Integer.parseInt(ChatColor.stripColor(is.getItemMeta().getDisplayName()
											.replace("Min Level (", "").replace(")", "")));
								else if (is.getItemMeta().getDisplayName().contains("Max Level ("))
									level2 = Integer.parseInt(ChatColor.stripColor(is.getItemMeta().getDisplayName()
											.replace("Max Level (", "").replace(")", "")));
							}
						}
						if (level1 < level2) {
							region.minLevel = level1;
							region.maxLevel = level2;
						} else {
							region.maxLevel = level1;
							region.minLevel = level2;
						}
						region.save();
						MetaLists.save.add(e.getWhoClicked());
						e.getWhoClicked().closeInventory();
					} else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Items"))
						e.getWhoClicked().openInventory(Main.getItemInventory());
					else if (e.getCurrentItem().getItemMeta().getDisplayName()
							.equals(ChatColor.RED + "Exit Without Saving"))
						e.getWhoClicked().closeInventory();
					else if (e.getCurrentItem().getItemMeta().getDisplayName().contains(" Level (")) {
						ItemMeta im = e.getCurrentItem().getItemMeta();
						int level = Integer.parseInt(ChatColor.stripColor(e.getCurrentItem().getItemMeta()
								.getDisplayName().replace("Min Level (", "").replace("Max Level (", "")
								.replace(")", "")));
						int num = 1;
						if (e.isShiftClick())
							num = 5;
						int newLevel = 0;
						if (e.isLeftClick())
							newLevel = level + num;
						else if (e.isRightClick())
							newLevel = level - num;
						if (newLevel < 0)
							newLevel = 0;
						im.setDisplayName(im.getDisplayName().replace(level + "", newLevel + ""));
						e.getCurrentItem().setItemMeta(im);
					}
				}
			}

		} else if (e.getInventory().getName().contains(ChatColor.GREEN + "Config")) {

			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				double amount = .1;
				if (e.isShiftClick())
					amount = 1.0;
				if (e.isRightClick())
					amount = -amount;
				if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Exit and Save")) {
					for (int i = 0; i < 8; i++) {
						ItemStack is = e.getInventory().getContents()[i];
						if (is != null && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
							if (is.getItemMeta().getDisplayName().contains("Variation (")) {
								double var = Double.parseDouble(ChatColor.stripColor(is.getItemMeta().getDisplayName()
										.replace("Variation (", "").replace(")", "")));
								Main.variation = var;
								Main.MainConfig.set("Variation", var);
							} else if (is.getItemMeta().getDisplayName().contains("Extra Damage (")) {
								double var = Double.parseDouble(ChatColor.stripColor(is.getItemMeta().getDisplayName()
										.replace("Extra Damage (", "").replace(")", "")));
								Main.extraDamage = var;
								Main.MainConfig.set("Extra Damage Per Level", var);
							} else if (is.getItemMeta().getDisplayName().contains("Extra Health (")) {
								double var = Double.parseDouble(ChatColor.stripColor(is.getItemMeta().getDisplayName()
										.replace("Extra Health (", "").replace(")", "")));
								Main.extraHealth = var;
								Main.MainConfig.set("Extra Health Per Level", var);
							} else if (is.getItemMeta().getDisplayName().contains("Extra Experience (")) {
								double var = Double.parseDouble(ChatColor.stripColor(is.getItemMeta().getDisplayName()
										.replace("Extra Experience (", "").replace(")", "")));
								Main.extraExperience = var;
								Main.MainConfig.set("Extra Experience Per Level", var);
							}
						}
					}
					MetaLists.save.add(e.getWhoClicked());
					Main.MainConfig.save();
					e.getWhoClicked().closeInventory();
				} else if (e.getCurrentItem().getItemMeta().getDisplayName()
						.equals(ChatColor.RED + "Exit Without Saving"))
					e.getWhoClicked().closeInventory();
				else if (e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Items"))
					e.getWhoClicked().openInventory(Main.getItemInventory());
				else {
					changeName(e.getCurrentItem(), "Variation", amount);
					changeName(e.getCurrentItem(), "Extra Damage", amount);
					changeName(e.getCurrentItem(), "Extra Health", amount);
					changeName(e.getCurrentItem(), "Extra Experience", amount);
				}
			}

		} else if (e.getInventory().getName().contains(ChatColor.DARK_GREEN + "Items")) {

			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				String name = e.getCurrentItem().getItemMeta().getDisplayName();
				if (name.contains("Page "))
					e.getInventory().setContents(
							Main.getItemContents(Integer.parseInt(ChatColor.stripColor(name.replace("Page ", "")))));
				if (name.contains("Level "))
					e.getWhoClicked().openInventory(
							Main.getStrictItemInventory(Integer.parseInt(ChatColor.stripColor(name
									.replace("Level ", "")))));
			}

		} else if (e.getInventory().getName().contains(ChatColor.DARK_GREEN + "Level ")) {

			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()
					&& e.getCurrentItem().getItemMeta().hasDisplayName()) {
				String name = e.getCurrentItem().getItemMeta().getDisplayName();
				ItemListInfo iLI = ItemListInfo.get(Integer.parseInt(e.getInventory().getName()
						.replace(ChatColor.DARK_GREEN + "Level ", "")));
				if (name.contains("Exit and Save")) {
					int i = 0;
					ItemStack[] items = new ItemStack[54];
					for (int x = 0; x < e.getInventory().getSize() - 1; x++) {
						ItemStack is = e.getInventory().getContents()[x];
						if (is != null && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
							String itemName = is.getItemMeta().getDisplayName();
							if (itemName.contains(ChatColor.GREEN + "Chance ("))
								iLI.setPercent(Double.parseDouble(itemName.replace(ChatColor.GREEN + "Chance (", "")
										.replace(")", "")));
							else if (!itemName.contains("Exit and Save") && !itemName.contains("Exit Without Saving")) {
								items[i] = is;
								i++;
							}
						} else if (is != null) {
							items[i] = is;
							i++;
						}
					}
					if (i != 0) {
						iLI.setItems(items);
						iLI.save();
					} else {
						iLI.setItems(null);
						iLI.save();
						iLI.setItems(items);
					}
					MetaLists.save.add(e.getWhoClicked());
					e.getWhoClicked().openInventory(Main.getItemInventory());
				} else if (name.contains("Exit Without Saving")) {
					e.getWhoClicked().openInventory(Main.getItemInventory());
				} else if (name.contains(ChatColor.GREEN + "Chance (")) {
					double oldChance = Double.parseDouble(name.replace(ChatColor.GREEN + "Chance (", "").replace(")",
							""));
					double amount = 1.0;
					if (oldChance + amount < 1.0)
						if (e.isShiftClick())
							amount = .1;
						else
							amount = .05;
					else if (e.isShiftClick())
						amount = 10.0;
					if (e.isRightClick())
						amount = -amount;
					double newChance = Math.round((oldChance + amount) * 100.0) / 100.0;
					if (newChance < 0.0)
						newChance = 0.0;
					ItemMeta im = e.getCurrentItem().getItemMeta();
					im.setDisplayName(name.replace(oldChance + "", newChance + ""));
					e.getCurrentItem().setItemMeta(im);
				} else {
					e.setCancelled(false);
				}
			} else {
				e.setCancelled(false);
			}

		}
	}

	public static void changeName(ItemStack is, String name, double amount) {
		ItemMeta im = is.getItemMeta();
		if (im.getDisplayName().contains(name)) {
			double var = Double.parseDouble(ChatColor.stripColor(is.getItemMeta().getDisplayName()
					.replace(name + " (", "").replace(")", "")));
			double num = ((double) Math.round((var + amount) * 100)) / 100;
			im.setDisplayName(im.getDisplayName().replace(var + "", num + ""));
			is.setItemMeta(im);
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if ((e.getInventory().getName().contains(ChatColor.GREEN + "Editor: ")
				|| e.getInventory().getName().contains(ChatColor.GREEN + "Config") || e.getInventory().getName()
				.contains(ChatColor.DARK_GREEN + "Level "))
				&& e.getPlayer() instanceof Player)
			if (MetaLists.save.contains(e.getPlayer())) {
				MetaLists.save.remove(e.getPlayer());
				((Player) e.getPlayer()).sendMessage(ChatColor.GREEN + "Changes saved");
			} else
				((Player) e.getPlayer()).sendMessage(ChatColor.RED + "Changes have not been saved!");
	}

	@EventHandler
	public void onDeath(EntityDeathEvent e) {
		LivingEntity ent = e.getEntity();
		if (ent.getCustomName() != null && ent.getCustomName().contains(ChatColor.GREEN + "[lvl ")) {
			int level = getLevel(ent);
			if (Main.greatestNumber != -1) {
				ItemListInfo iLI = ItemListInfo.get(level);
				ItemStack[] items = iLI.getItems();
				double rand = new Random().nextDouble() * 100.0;
				if (rand < iLI.getPercent()) {
					e.getEntity().getLocation().getWorld()
							.dropItem(e.getEntity().getLocation(), items[new Random().nextInt(items.length)]);
				}
			}
			if (level == -1) {
				Bukkit.getLogger().log(Level.WARNING, "[LZ] Error at getLevel()  (tell the developer, MnMaxon)");
				return;
			}
			e.setDroppedExp(e.getDroppedExp() + (int) (Main.extraExperience));
		}
	}
}
