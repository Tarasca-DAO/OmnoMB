package concept.omno;

import concept.platform.Block;
import concept.platform.EconomicCluster;
import concept.utility.JsonFunction;
import org.json.simple.JSONObject;

public class PlatformContext {

    final ApplicationContext applicationContext;
    public EconomicCluster economicCluster;

    PlatformContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void update() {
        JSONObject response;

        response = applicationContext.ardorApi.getBlockWithRetry(false);

        if (response == null) {
            return;
        }

        int height = JsonFunction.getInt(response, "height", 0);
        applicationContext.logDebugMessage("--> PlatformContext | ARDOR Height: " + height);

        if (height == 0) {
            return;
        }

        EconomicCluster economicCluster;

        try {
            economicCluster = new EconomicCluster(applicationContext.ardorApi, height);
        } catch (Exception e) {
            return;
        }

        if (!economicCluster.isValid(applicationContext.ardorApi)) {
            return;
        }

        this.economicCluster = economicCluster;
    }

    public int getHeight() {
        return economicCluster.height;
    }

    public Block getBlock() {
        return new Block(economicCluster);
    }
}
