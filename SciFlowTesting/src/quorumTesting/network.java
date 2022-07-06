package quorumTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class network{
    public final double PERCENTMALICOUS = .10;
    private final Node[] net;
    public int netSize;
    public network(int netSize){
        net = new Node[netSize];
        int malCount = (int) (netSize * PERCENTMALICOUS);

        for(int i=0; i<netSize-malCount; i++) {
            net[i] = new Node(true);
        }for(int j=netSize-malCount; j<netSize; j++){
            net[j] = new Node(false);
        }

        List<Node> shuffleList = Arrays.asList(net);
        Collections.shuffle(shuffleList);
        shuffleList.toArray(net);
        this.netSize=netSize;
    }

    public void reshuffle(){
        List<Node> shuffleList = Arrays.asList(net);
        Collections.shuffle(shuffleList);
        shuffleList.toArray(net);
    }
    public Node[] getNet() {
        return net;
    }

    @Override
    public String toString() {
        return netSize+"";
    }

    public static class Node {
        private final boolean node;

        public Node(boolean mal){
            this.node = mal;
        }
        public boolean goodState() {
            return node;
        }

        @Override
        public String toString() {
            return node + " ";
        }

    }
}
