import java.io.IOException;
import java.util.Random;
import java.util.*;
import java.io.*;

public class randomizeGen {
    static int maxWorkflows =10;
    static int maxWSize = 10;
    static double PERBRANCH = 0.3;
    static ArrayList<Integer> startPoint = new ArrayList<>(maxWorkflows);
    static ArrayList<ArrayList<task>> workflows = new ArrayList<>();

    public static void generate(){
        workflows.add(genRandWorkflow(0,null,null));
        Random rand = new Random();
        for(int i=1; i<maxWorkflows; i++) {
            int randWf = rand.nextInt((i));
            workflows.add(genRandWorkflow(i,rand.nextInt(startPoint.get(randWf))+1,randWf));
        }
    }
    // Function to generate random graph
    public static ArrayList<task> genRandWorkflow(int wf, Integer stpt, Integer stwf) {

        Random rand = new Random();
        int wSize = rand.nextInt(maxWSize/2) + 3;
        int branchCount = (int)(wSize* PERBRANCH) + 1;
        int counter = 1;
        int randIdx;
        ArrayList<task> workflow = new ArrayList<>();

        workflow.add(new gentask("w" + wf, "gen", String.valueOf(stwf),String.valueOf(stpt)));
        workflow.add(new task("w" + wf, "t1", false, new ArrayList<>(Arrays.asList(0))));
        while (counter < wSize) {
            workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(counter))));
            counter++;
        }
        int linear = counter;

         for(int i = 0; i< branchCount; i++){
             randIdx = rand.nextInt(linear-2)+1;
                 workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(randIdx))));
                 counter++;
                int branchLen = rand.nextInt(4);
                for(int j=0; j<branchLen; j++){
                    workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(counter))));
                    counter++;
                }
                task merge = workflow.get(rand.nextInt(linear-1-randIdx)+randIdx+2);
                merge.addIdxParent(counter);
         }
        startPoint.add(counter);
        return workflow;

    }



    public static void main(String[] args) {
       String[] arr = {"gnome-terminal","ls"};
        try {
            Process process = Runtime.getRuntime().exec("gnome-terminal");
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generate();
        System.out.println(workflows);

        //

    }


}
