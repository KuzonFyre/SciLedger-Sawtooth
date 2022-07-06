package quorumTesting;


public class quorumcalc {
    public final int AVGPERC = 10;
    public double avgGoodQuor;
    public network net;
    public int quorCount;
    public double quorpernet;
    public double threshold;
    public quorumcalc(network net, int quorCount, double quorpernet,double threshold) {
        int goodNode;
        int goodQuorum;
        this.quorCount = quorCount;
        this.net = net;
        this.quorpernet = quorpernet;
        this.threshold = threshold;

        for (int k = 0; k < AVGPERC; k++) {
            goodQuorum = 0;
            int quorSize = (int) (this.net.getNet().length * quorpernet);
            for (int j = 0; j < this.quorCount; j++) {
                goodNode=0;
                for (int i = 0; i < quorSize; i++) {
                    if (this.net.getNet()[i].goodState()) {
                        goodNode++;
                    }
                }
                if ((double)goodNode/quorSize>= this.threshold) {
                    goodQuorum++;
                }
                this.net.reshuffle();
            }
            this.avgGoodQuor += goodQuorum/(double)this.quorCount;
        }
        this.avgGoodQuor = this.avgGoodQuor/AVGPERC;
    }

    @Override
    public String toString() {
        return "\nNetwork Size:" + this.net +
        "\nQuorums Calculated:" + this.quorCount +
        "\nQuorum ratio to Network Size:" + this.quorpernet +
        "\nQuorum Threshold:" + this.threshold +
        "\nAverage Good Quorums: " + this.avgGoodQuor;
    }
}
