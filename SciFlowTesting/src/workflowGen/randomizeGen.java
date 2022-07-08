package workflowGen;
import java.io.IOException;
import java.util.Random;
import java.util.*;

public class randomizeGen {
    static int maxWorkflows =10;
    static int maxWSize = 10;
    static double perInv = 0.3;
    static double PERBRANCH = 0.3;
    static ArrayList<Integer> startPoint = new ArrayList<>(maxWorkflows);

    public static void generate(){

        generateRandomGraphs(0,null,null);
        Random rand = new Random();
        for(int i=1; i<maxWorkflows; i++) {
            int randWf = rand.nextInt((i));
            generateRandomGraphs(i,rand.nextInt(startPoint.get(randWf))+1,randWf);
        }
    }
    // Function to generate random graph
    public static void generateRandomGraphs(int wf, Integer stpt,Integer stwf) {

        Random rand = new Random();
        int wSize = rand.nextInt(maxWSize/2) + 3;
        int branchCount = (int)(wSize* PERBRANCH) + 1;
        int c = 1;

        ArrayList<task> workflow = new ArrayList<>();

        workflow.add(new gentask("w" + wf, "gen", String.valueOf(stwf),String.valueOf(stpt)));
        workflow.add(new task("w" + wf, "t1", false, new ArrayList<>(Arrays.asList(0))));
        while (c < wSize) {
            workflow.add(new task("w" + wf, "t" + (c+1), false, new ArrayList<>(Arrays.asList(c))));
            c++;
        }
        int linear = c;
        int randIdx;
         for(int i = 0; i< branchCount; i++){
             randIdx = rand.nextInt(linear-2)+1;
                 workflow.add(new task("w" + wf, "t" + (c+1), false, new ArrayList<>(Arrays.asList(randIdx))));
                 c++;
                int branchLen = rand.nextInt(4);
                for(int j=0; j<branchLen; j++){
                    workflow.add(new task("w" + wf, "t" + (c+1), false, new ArrayList<>(Arrays.asList(c))));
                    c++;
                }
                task merge = workflow.get(rand.nextInt(linear-1-randIdx)+randIdx+2);
                merge.addIdxParent(c);



         }
        System.out.println(workflow);
        startPoint.add(c);
    }



    public static void main(String[] args) {
        Runtime run = Runtime.getRuntime();
        try {
            run.exec("notepad.exe");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generate();
    }

    // number of tasks
    // shape of workflow
    // parameter (how many to run)

}
