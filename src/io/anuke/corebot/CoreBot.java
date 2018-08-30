package io.anuke.corebot;

import java.io.File;

public class CoreBot {
	public static final String[] allServers = {"mindustry.us.to", "mindustry.oa.to", "batata69.zapto.org",
			"thebabinator.servegame.com", "mindustry.pastorhudson.com", "mindustry.indielm.com"};
	public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
	public static final long guildID = 391020510269669376L;
	public static final File prefsFile = new File("prefs.properties");
	public static final String bugChannelName = "bugs";
	public static final String crashReportChannelName = "crashes";

	public static final long messageDeleteTime = 20000;

	public static Messages messages = new Messages();
	public static Commands commands = new Commands();
	public static Net net = new Net();
	public static Prefs prefs = new Prefs(prefsFile);
	public static Reports reports = new Reports();

	public static void main(String[] args){
		new CoreBot();
	}
}
