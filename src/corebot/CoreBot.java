package corebot;

import java.io.File;

public class CoreBot{
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final long guildID = 802240559489613845L;
    public static final File prefsFile = new File("prefs.properties");
    public static final long modChannelID = 803286895978741761L;
    public static final long pluginChannelID = 0L;
    public static final long crashReportChannelID = 0L;
    public static final long announcementsChannelID = 0L;
    public static final long screenshotsChannelID = 0L;
    public static final long artChannelID = 0L;
    public static final long mapsChannelID = 416719902641225732L;
    public static final long moderationChannelID = 802254084857528363L;
    public static final long schematicsChannelID = 803286677615280168L;
    public static final long baseSchematicsChannelID = 0L;
    public static final long generalChannelID = 802241725791338506L;

    public static final long messageDeleteTime = 20000;

    public static ContentHandler contentHandler = new ContentHandler();
    public static Messages messages = new Messages();
    public static Commands commands = new Commands();
    public static Net net = new Net();
    public static Prefs prefs = new Prefs(prefsFile);

    //crash reporting disabled until V6 is out
    //public static Reports reports = new Reports();

    public static void main(String[] args){
        new CoreBot();
    }
}
