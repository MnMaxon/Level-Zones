package me.MnMaxon.LevelZones;

import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;

public class MetaLists {
	public static MetaLists pointOne = new MetaLists("pointOne");
	public static MetaLists pointTwo = new MetaLists("pointTwo");
	public static MetaLists save = new MetaLists("save");

	private String name;

	public MetaLists(String name) {
		this.name = name;
	}

	public Boolean contains(Entity ent) {
		if (ent == null)
			return false;
		return ent.hasMetadata(getName());
	}

	public Object get(Entity ent) {
		if (ent == null || !ent.hasMetadata(getName()))
			return null;
		return ent.getMetadata(getName()).get(0).value();
	}

	public void add(Entity ent) {
		if (ent == null)
			return;
		add(ent, true);
	}

	public void add(Entity ent, Object object) {
		if (ent == null)
			return;
		ent.setMetadata(getName(), new FixedMetadataValue(Main.plugin, object));
	}

	public void remove(Entity ent) {
		if (ent == null)
			return;
		ent.removeMetadata(getName(), Main.plugin);
	}

	public String getName() {
		return name;
	}
}