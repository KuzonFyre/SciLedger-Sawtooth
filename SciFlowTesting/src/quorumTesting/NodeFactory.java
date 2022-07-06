package quorumTesting;

//import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NodeFactory {
    public static void main(String[] args) {
        File file = new File("./output/data.csv");
        network n1 = new network(100);
        network n2 = new network(500);
        network n3 = new network(1000);

        double [] quorumThresholds = {.85,.80,.775,.75,.70};
        double [] quorumSizes = {.10,.2,.25,.275};

        for (double quorumSize : quorumSizes) {
            for (double quorumThreshold : quorumThresholds) {
                quorumcalc calc1 = new quorumcalc(n1, 10000, quorumSize, quorumThreshold);
                quorumcalc calc2 = new quorumcalc(n2, 10000, quorumSize, quorumThreshold);
                quorumcalc calc3 = new quorumcalc(n3, 10000, quorumSize, quorumThreshold);
//                try {
//                    // create FileWriter object with file as parameter
//                    FileWriter outputfile = new FileWriter(file,true);
//
//                    // create CSVWriter object filewriter object as parameter
//                    CSVWriter writer = new CSVWriter(outputfile);
//
//                    String[] data1 = { calc1.net.toString(), calc1.quorpernet+"", calc1.threshold+"",calc1.avgGoodQuor+"" };
//                    String[] data2 = { calc2.net.toString(), calc2.quorpernet+"", calc2.threshold+"",calc2.avgGoodQuor+"" };
//                    String[] data3 = { calc3.net.toString(), calc3.quorpernet+"", calc3.threshold+"",calc3.avgGoodQuor+"" };
//                    writer.writeNext(data1);
//                    writer.writeNext(data2);
//                    writer.writeNext(data3);
//                    // closing writer connection
//                    writer.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
                System.out.println(calc2);
                System.out.println(calc1);
                System.out.println(calc3);
            }
        }





    }


}
