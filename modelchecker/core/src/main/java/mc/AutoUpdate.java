package mc;

import mc.util.Utils;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.IOUtils;

import java.io.IOException;
import java.net.URL;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Created by Sanjay on 18/07/2017.
 */
public class AutoUpdate {
    private Logger logger = LoggerFactory.getLogger(AutoUpdate.class);
    private static final String version = "v4.7";
    private static final String githubAPI = "https://api.github.com/repos/DavidSheridan/Model-Checker/releases/latest";
    private String[] getDownloadInfo() {
        try {
            JSONObject obj = new JSONObject(IOUtils.toString(new URL(githubAPI).openStream()));
            if (obj.getString("tag_name").equals(version)) {
                logger.info(""+ansi().render("@|green Model checker up to date!|@"));
                return null;
            }
            return new String[]{obj.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"),obj.getString("tag_name"),obj.getString("body")};
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public boolean isUpdated() {
        return getDownloadInfo() == null;
    }
    public void checkForUpdates() {
        if (!Utils.isJar()) return;
        AnsiConsole.systemInstall();
        String[] downloadInfo = getDownloadInfo();
        if (downloadInfo == null) return;
        logger.info(""+ansi().render("@|red Update found! (Current version: "+version+", New version: "+downloadInfo[1]+")|@"));
        logger.info(""+ansi().render("@|yellow Update changes: "+downloadInfo[2]+"|@"));
        logger.info(""+ansi().render("@|yellow Download it from: "+downloadInfo[0]+"|@"));
    }
}
