package me.meiamsome.recipelookup;

import java.util.HashMap;

import me.meiamsome.recipelookup.Metrics.Graph;

public class MetricsManager {
	Metrics met = null;
	Graph searchGraph;
	private HashMap<String, CustomPlotter> searchPlotters  = new HashMap<String, CustomPlotter>();
	RecipeLookup plugin;
	
	MetricsManager(RecipeLookup pl) {
		plugin = pl;
		initMet();
	}
	private void initMet() {
		try {
			met = new Metrics(plugin);
			handleSearchTerms();
			others();
			met.start();
		} catch (Exception e) {};
	}
	private void handleSearchTerms() {
		searchGraph = met.createGraph("Searches");
	}
	private void others() {
		Graph g = met.createGraph("Allow window");
		g.addPlotter(new Metrics.Plotter("Yes") {
			@Override
			public int getValue() {
				return plugin.getConfig().getBoolean("show in window")?1:0;
			}
		});
		g.addPlotter(new Metrics.Plotter("No") {
			@Override
			public int getValue() {
				return plugin.getConfig().getBoolean("show in window")?0:1;
			}
		});
	}
	
	public void recordSearch(String itemName) {
		if(!searchPlotters.containsKey(itemName)) {
			searchPlotters.put(itemName, new CustomPlotter(itemName));
			searchGraph.addPlotter(searchPlotters.get(itemName));
		}
		searchPlotters.get(itemName).data++;
	}
	
	private class CustomPlotter extends Metrics.Plotter {
		public int data = 0;
		public CustomPlotter(String name) {
			super(name);
		}

		@Override
		public int getValue() {
			int t = data;
			data = 0;
			return t;
		}
	}
}
