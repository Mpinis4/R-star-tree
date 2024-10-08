import java.util.ArrayList;
import java.util.List;

public class SerialManager {

    private final int diastasis;
    private final List<Double> givenCoor;
    private final int k;


    SerialManager(int diastasis,List<Double> givenCoor, int k)
    {
        this.diastasis=diastasis;
        this.givenCoor=new ArrayList<>(givenCoor);
        this.k=k;
    }

    SerialManager(int diastasis,List<Double> givenCoor)
    {
        this.diastasis=diastasis;
        this.givenCoor= new ArrayList<>(givenCoor);
        this.k=0;
    }

    public void KNN() throws Exception{
        NearestNeighboursQuery sknn=new NearestNeighboursQuery(diastasis,k,givenCoor);
        long startTime= System.nanoTime();
        sknn.CalculateKNN();
        long endTime=System.nanoTime();

        long duration= (endTime-startTime);
        System.out.println("SerialKNN: "+ duration/1000000+"ms");
        System.out.println("SerialKNN: "+ duration+ "ns");

    }

    public void RQ() throws Exception
    {
        RangeQuery srq=new RangeQuery(diastasis,givenCoor);

        long startTime= System.nanoTime();
        srq.RQ();
        long endTime= System.nanoTime();

        long duration = (endTime-startTime);
        System.out.println("SerialRQ: "+ duration/1000000+ "ms");
        System.out.println("SerialRQ: "+ duration + "ns");
    }


}
