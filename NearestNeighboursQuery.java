import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Scanner;

public class NearestNeighboursQuery {

    private final int diastasis;
    private final int k;
    private final List<Double> givenCoor;
    private final String datafile;
    private PriorityQueue<Records> pq;


    NearestNeighboursQuery(int diastasis,int k, List<Double> givenCoor)
    {
        this.diastasis=diastasis;
        this.k=k;
        this.givenCoor=new ArrayList<>(givenCoor);
        this.datafile="datafile";
    }

    public void CalculateKNN()throws Exception{
        pq= new PriorityQueue<>(k,Comparator.comparing(Records::getDistance).reversed());
        String string;
        String[] parts;
        double distance= 0;
        List<Double> dimensions = new ArrayList<>();

        Scanner sr=new Scanner(new File(datafile));
        String lines= sr.nextLine();
        for(int i=0;i<Integer.parseInt(lines)-1;i++)
        {
            sr.nextLine();
        }

        while(sr.hasNextLine())
        {
            string =sr.nextLine();
            parts= string.split("\\s+");

            for(int i=0;i<diastasis;i++)
            {
                dimensions.add(Double.valueOf(parts[i+1]));
            }
            for(int i=0;i<diastasis;i++)
            {
                distance+=Math.pow(dimensions.get(i)-givenCoor.get(i), 2);
            }
            distance= Math.sqrt(distance);

            if(pq.size() >=k)
            {
                pq.add(new Records(parts[0],new ArrayList<>(dimensions),distance));
                pq.poll();
            }
            else {
                pq.add(new Records(parts[0],new ArrayList<>(dimensions),distance));
            }
            distance=0;
            dimensions.clear();

        }
        sr.close();
        for(int i=0;i<k;i++)
        {
            Objects.requireNonNull(pq.poll()).showRecord();
        }


    }
}
