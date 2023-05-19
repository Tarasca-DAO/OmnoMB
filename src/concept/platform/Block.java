package concept.platform;

public class Block {
    EconomicCluster economicCluster;

    public Block(EconomicCluster economicCluster) {
        this.economicCluster = economicCluster.clone();
    }

    public int getHeight() {
        return economicCluster.height;
    }

    public long getBlockId() {
        return economicCluster.blockId;
    }

    public int getTimestamp() {
        return economicCluster.timestamp;
    }
}
