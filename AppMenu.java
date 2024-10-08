import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;




public class AppMenu {

    public void throwMenu() throws Exception
    {
        System.out.println("-----Hello!-----\n");
        System.out.println("Those are your actions. ");
        System.out.println("1) Build R star Tree. ");
        System.out.println("2) Exit the program. \n");

        Scanner sr = new Scanner(System.in);

        String x;
        String file="";
        int dimensions=0;

        do {
            System.out.println("Choose your action! ");
            x=sr.next();
            switch (x) {
                case "1":
                    System.out.println("How many dimensions you want? ");
                    dimensions=sr.nextInt();

                    System.out.println("Give the name of your file! ");
                    file=sr.next();
                    break;
                case "2" :
                    System.out.println("You have exit the program!");
                    return;
            }

        }while (!x.equals("1"));



        //Create Datafile from given file.
        FileOperations fp=new FileOperations(dimensions, file);
        fp.Parsing();
        fp.writeDataFile();

        //Initialise R star Tree and build it
        RstarTree rst=new RstarTree(dimensions,file);
        long startTime = System.nanoTime();
        rst.BuildRstarTree();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        rst.WriteIndexFile();

        System.out.println("Your R star Tree is ready!\n");
        System.out.println("R* Tree Build: " + duration/1000000 + "ms");
        System.out.println("R* Tree Build: " + duration + "ns");
        System.out.println("Next you will find the actions that you can do on this Tree.\n");

        System.out.println("Choose your action!");
        System.out.println("1) Insert new point.");
        System.out.println("2) Delete point");
        System.out.println("3) Find all the points inside an area(RQ).");
        System.out.println("4) Find K nearest neighbors from a point(KNNQ).");
        System.out.println("5) Find the SkyLine(SLQ).");
        System.out.println("6) Bottom Up R* Tree build.");
        System.out.println("7) Exit the program.\n");

        String y = sr.next();
        List<Double> givenCoor = new ArrayList<>();

        switch(y) {
            case "1" :
                System.out.println("-----Insert Menu-----\n");
                System.out.println("Give an ID: ");

                String idInsert=sr.next();
                this.typeCoordinates(dimensions, sr, givenCoor);
                rst.InsertNewEntry(dimensions,givenCoor, idInsert);
                System.out.println("Given point inserted succefully!");
                break;
            case "2" :
                System.out.println("-----Delete Menu-----\n");
                System.out.println("Give id: ");
                String idDelete=sr.next();
                rst.DeleteEntry(idDelete);
                System.out.println("Given point deleted succefully!");
                break;
            case "3" :
                System.out.println("-----Range Query Menu-----\n");
                System.out.println("Give 1st Coordinates: ");
                //Coordinates given are about the min and the max limit of the array we are searching in
                for(int i=0;i<dimensions;i++) {
                    givenCoor.add(sr.nextDouble());
                }
                System.out.println("Give 2nd Coordinates: ");
                for(int i=0;i<dimensions;i++) {
                    givenCoor.add(sr.nextDouble());
                }

                System.out.println("R star Tree Range Query Results: ");
                rst.RangeQuery(dimensions,givenCoor);

                System.out.println("Serial Range Query Results: ");
                SerialManager sm=new SerialManager(dimensions, givenCoor);
                sm.RQ();
                break;
            case "4" :
                System.out.println("-----KNN Query Menu-----\n");
                System.out.println("Give number K of nearest neighbors: ");
                int k=sr.nextInt();
                this.typeCoordinates(dimensions, sr, givenCoor);
                System.out.println("R star Tree K Nearest Neighbors: ");
                rst.kNNQuery(dimensions, k, givenCoor);

                System.out.println("Serial K Nearest Neighbors: ");
                SerialManager sm2=new SerialManager(dimensions,givenCoor, k);
                sm2.KNN();
                break;

            case "5" :
                System.out.println("-----Skyline Query Menu-----\n");
                rst.SkylineQuery(dimensions);
                break;
            case "6":
                long startTime2 = System.nanoTime();
                rst.bottomUpRstarTreeBuild();
                long endTime2 = System.nanoTime();
                long duration2 = (endTime2 - startTime2);

                System.out.println("Your R* Tree Bottom up build is finished !\n");
                System.out.println("R* Tree Bottom up Build: " + duration2/1000000 + "ms");
                System.out.println("R* Tree Bottom up Build: " + duration2 + "ns");

            case "7" :
                System.out.println("You have exit the program! ");
                return;

        }
        System.out.println("-----Adios!-----");
    }


    private void typeCoordinates(int diastasis, Scanner sr, List<Double> givenCoor)
    {
        System.out.println("Give your coordinates please: ");
        for(int i=0;i<diastasis;i++)
        {
            givenCoor.add(Double.parseDouble(sr.next()));
        }
    }





}