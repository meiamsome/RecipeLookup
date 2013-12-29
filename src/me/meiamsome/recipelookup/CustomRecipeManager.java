package me.meiamsome.recipelookup;

import org.bukkit.configuration.ConfigurationSection;

public class CustomRecipeManager {
	RecipeLookup parent;
	public CustomRecipeManager(RecipeLookup par) {
		parent = par;
		if(!parent.getConfig().contains("CustomRecipes")) {
			parent.getConfig().createSection("CustomRecipes");
		}
		ConfigurationSection cs = parent.getConfig().getConfigurationSection("CustomRecipes");
		for(String keys: cs.getKeys(false)) {
			
		}
	}
	
	private load() {
		
	}

}
