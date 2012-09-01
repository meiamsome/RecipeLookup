package me.meiamsome.recipelookup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;



public class RecipeLookup extends JavaPlugin implements Listener {
	HashMap<Player, Recipe> inInventory = new HashMap<Player, Recipe>();
	HashMap<Player, List<Recipe>> playerRecipes = new HashMap<Player, List<Recipe>>();
	HashMap<Player, Integer> schedual = new HashMap<Player, Integer>();
	MetricsManager mm;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		saveDefaultConfig();
		mm = new MetricsManager(this);
	}
	@Override
	public void onDisable() {
		for(Player p : inInventory.keySet()) {
			p.getOpenInventory().getTopInventory().clear();
			p.closeInventory();
		}
		inInventory.clear();
		for(Player p : playerRecipes.keySet()) {
			p.getOpenInventory().getTopInventory().clear();
			p.closeInventory();
		}
		playerRecipes.clear();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!command.getName().equalsIgnoreCase("recipe")) return false;
		int arg = 0;
		ItemStack is;
		if(args.length == 0) {
			if(!(sender instanceof Player)) return false;
			is = ((Player)sender).getItemInHand().clone();
			is.setAmount(1);
		} else {
			String mat = args[arg++];
			byte data = -1;
			if(mat.contains(":")) {
				String[] var = mat.split(":");
				if(var.length > 2) {
					sender.sendMessage(ChatColor.RED + "Unknown item '"+args[0]+"'");
					return true;
				}
				mat = var[0];
				try {
					data = Byte.parseByte(var[1]);
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "Unknown item '"+args[0]+"'");
					return true;
				}
			}
			if(getMaterial(mat) == null) {
				sender.sendMessage(ChatColor.RED + "Unknown item '"+args[0]+"'");
				return true;
			}
			is = new ItemStack(getMaterial(mat));
			is.setDurability(data);
		}
		if(args.length > 2) return false;
		List<Recipe> recipes = getServer().getRecipesFor(is);
		Recipe r = null;
		if(recipes.size() == 0) {
			sender.sendMessage(ChatColor.RED + "There are no recipes for "+getStringWithoutAmount(is));
			return true;
		}
		if(recipes.size() > 1) {
			List<Recipe> newRecp = new ArrayList<Recipe>();
			for(Recipe recp: recipes) {
				if(recp instanceof ShapelessRecipe || recp instanceof ShapedRecipe) newRecp.add(recp);
			}
			if(args.length == arg) {
				sender.sendMessage(ChatColor.GREEN + "There are "+recipes.size()+" recipes for "+getStringWithoutAmount(is));
				sender.sendMessage(ChatColor.GREEN + "Use /recipe "+getStringWithoutAmount(is).toLowerCase().replace(' ', '_')+" <recipe no>");
				sender.sendMessage(ChatColor.GREEN + "To view different recipes");
				if(getConfig().getBoolean("show in window", true) && newRecp.size() > 1 && sender instanceof Player) {
					r = null;
					playerRecipes.put((Player) sender, newRecp);
				} else return true;
			} else {
				if(args[arg].equalsIgnoreCase("all") && getConfig().getBoolean("show in window", true) && sender instanceof Player) {
					r = null;
					playerRecipes.put((Player) sender, newRecp);
				} else {
					try {
						r = recipes.get(Integer.parseInt(args[arg++])-1);
					} catch(Exception e) {}//Exception handled by the if statement. Magical.
					if(r == null) {
						sender.sendMessage(ChatColor.RED + "Unknown recipe id '"+args[arg-1]+"'");
						return true;
					}
				}
			}
		} else r = recipes.get(0);
		showRecipe(sender, r);
		mm.recordSearch(getStringWithoutAmount(is));
		return true;
	}
	
	public void showRecipe(CommandSender sender, Recipe r) {
		if(sender instanceof Player && getConfig().getBoolean("show in window", true)) {
			if(showVisualRecipe((Player) sender, r))
				return;
		}
		if(r instanceof ShapelessRecipe) {
			ShapelessRecipe cr = (ShapelessRecipe) r;
			sender.sendMessage(ChatColor.GREEN + "You need: ");
			for(ItemStack item: cr.getIngredientList())
				sender.sendMessage(ChatColor.GREEN + getString(item));
			sender.sendMessage(ChatColor.GREEN + "Arranged in any way in the crafting area.");
		} else if(r instanceof ShapedRecipe) {
			ShapedRecipe sr = (ShapedRecipe) r;
			sender.sendMessage(ChatColor.GREEN + "You need: ");
			HashMap<Character, ItemStack> items = new HashMap<Character, ItemStack>();
			for(String s: sr.getShape()) {
				String send = "";
				for(Character c: s.toCharArray()) {
					if(sr.getIngredientMap().get(c) == null) {
						send += "- ";
						continue;
					}
					ItemStack is = sr.getIngredientMap().get(c);
					Character ch = getStringWithoutAmount(is).toUpperCase().charAt(0);
					if(items.containsKey(ch)) {
						int a = 1;
						String str = getStringWithoutAmount(is).toUpperCase();
						while(items.containsKey(ch) && !items.get(ch).equals(is)) {
							if(a < str.length())
								ch = str.charAt(a++);
							else ch = (char) ('a' + a - str.length());
						}
					}
					items.put(ch, is);
					send+=ch+" ";
				}
				sender.sendMessage(ChatColor.GREEN+send);
			}
			boolean where = true;
			for(Character c: items.keySet()) {
				sender.sendMessage(ChatColor.GREEN + (where?"Where ":"and ") + c + " represents "
						+ getString(items.get(c)));
				where = false;
			}
		} else if(r instanceof FurnaceRecipe) {
			FurnaceRecipe fr = (FurnaceRecipe) r;
			sender.sendMessage(ChatColor.GREEN + "You need to smelt: " + getString(fr.getInput()));
		} else if(r == null) {
			sender.sendMessage(ChatColor.RED + "This is unsupported");
			return;
		} else {
			sender.sendMessage(ChatColor.RED + "Unknown recipe type " + r.getClass().getSimpleName() + "!");
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "In order to get " +getString(r.getResult()));
	}
	
	public boolean showVisualRecipe(Player p, Recipe r) {
		if(r instanceof ShapelessRecipe || r instanceof ShapedRecipe) {
			inInventory.put(p, r);
			p.openWorkbench(null, true);
			return true;
		}
		if(r == null && playerRecipes.containsKey(p)) {
			p.openWorkbench(null, true);
			return true;
		}
		return false;
	}
	@EventHandler (priority = EventPriority.HIGHEST)
	public void invClick(InventoryClickEvent e) {
		if(!(e.getWhoClicked() instanceof Player)) return;
		if(inInventory.containsKey((Player)e.getWhoClicked())) e.setCancelled(true);
		if(playerRecipes.containsKey((Player)e.getWhoClicked())) e.setCancelled(true);
	}
	@EventHandler
	public void invOpen(final InventoryOpenEvent e) {
		if(!inInventory.containsKey(e.getPlayer()) && !playerRecipes.containsKey(e.getPlayer())) return;
		Recipe r = inInventory.get(e.getPlayer());
		if(r == null) {
			schedual.put((Player) e.getPlayer(),Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				int num = 0;
				Player p = (Player) e.getPlayer();
				@Override
				public void run() {
					if(playerRecipes.get(p)!=null) {
						showInventoryRecipe(playerRecipes.get(p).get(num++ % playerRecipes.get(p).size()), p.getOpenInventory());
					}
				}
				
			}, 0, 20));
		} else showInventoryRecipe(r, e.getView());
	}
	@EventHandler
	public void invClose(InventoryCloseEvent e) {
		if(inInventory.containsKey(e.getPlayer()) || playerRecipes.containsKey(e.getPlayer())) e.getInventory().clear();
		inInventory.remove(e.getPlayer());
		playerRecipes.remove(e.getPlayer());
		if(schedual.containsKey(e.getPlayer())) {
			Bukkit.getScheduler().cancelTask(schedual.get(e.getPlayer()));
			schedual.remove(e.getPlayer());
		}
	}
	
	public int recipeWidth(ShapedRecipe sr) {
		int ret = 0;
		for(String s: sr.getShape()) ret = Math.max(ret, s.length());
		return ret;
	}
	public int recipeHeight(ShapedRecipe sr) {
		return sr.getShape().length;
	}
	
	public boolean showInventoryRecipe(Recipe r, InventoryView i) {
		i.getTopInventory().clear();
		ItemStack item;
		if(r instanceof ShapedRecipe) {
			ShapedRecipe sr = (ShapedRecipe) r;
			int x, y = recipeHeight(sr) == 3?0:1;
			for(String s : sr.getShape()) {
				x = recipeWidth(sr) == 3? 0:1;
				for(Character c: s.toCharArray()) {
					item = sr.getIngredientMap().get(c);
					if(item!=null) {
						item = item.clone();
						if(item.getDurability() == -1) item.setDurability((short) 0);
					}
					i.getTopInventory().setItem(x++ + (y)*3 + 1, item);
				}
				y++;
			}
		} else if(r instanceof ShapelessRecipe) {
			ShapelessRecipe sr = (ShapelessRecipe) r;
			int pos = sr.getIngredientList().size() > 4? 1: 9;
			if(sr.getIngredientList().size() > 4) {
				for(ItemStack is: sr.getIngredientList()) {
					item = is;
					if(item!=null) {
						item = item.clone();
						if(item.getDurability() == -1) item.setDurability((short) 0);
					}
					i.getTopInventory().setItem(pos ++, item);
				}
			} else {
				for(ItemStack is: sr.getIngredientList()) {
					item = is;
					if(item!=null) {
						item = item.clone();
						if(item.getDurability() == -1) item.setDurability((short) 0);
					}
					i.getTopInventory().setItem(pos--, item);
					if(pos == 7) pos--;
				}
				
			}
		} else {
			return false;
		}
		getServer().getPluginManager().callEvent(new CraftItemEvent(r, i, SlotType.RESULT, 0, false, false));
		return true;
	}
	
	public String getString(ItemStack item) {
		if(item.getData().getData() == -1)
			return "" + item.getAmount() + " of any " + getStringWithoutAmount(item);
		return "" + item.getAmount() + " " + getStringWithoutAmount(item);
	}
	public String getStringWithoutAmount(ItemStack item) {
		if(item.getData().getData() <= 0)
			return firstCaps(item.getType().toString().replace('_', ' '));
		return firstCaps(item.getType().toString().replace('_', ' ') + ":" +item.getData().getData());
	}
	public String firstCaps(String s) {
		s = s.toLowerCase();
		String res ="";
		for(String a: s.split(" ")) res += a.toUpperCase().charAt(0) + a.toLowerCase().substring(1) + " ";
		return res.trim();
	}
	// Acrobot's code.
    public static Material getMaterial(String name) {
        Material material = Material.matchMaterial(name);

        if (material != null) {
            return material;
        }

        name = name.replace(" ", "").toUpperCase();

        short length = Short.MAX_VALUE;

        for (Material currentMaterial : Material.values()) {
            String matName = currentMaterial.name();

            if (matName.replace("_", "").startsWith(name) && matName.length() < length) {
                length = (short) matName.length();
                material = currentMaterial;
            }
        }

        return material;
    }
}
