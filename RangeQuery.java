import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RangeQuery {

    private final int diastasis;
    private final List<Double> givenCoor;
    private final String datafile;
    private final List<Records> inRange;

    RangeQuery(int diastasis, List<Double> givenCoor)
    {
        this.diastasis=diastasis;
        this.givenCoor= new ArrayList<>(givenCoor);
        this.datafile="datafile";
        inRange=new ArrayList<>();
    }

    public void RQ() throws Exception
    {
        String string;
        String[] parts;
        List<Double> dimensions = new ArrayList<>();
        Scanner sr=new Scanner(new File(datafile));

        String lines=sr.nextLine();
        for(int i=0;i<Integer.parseInt(lines)-1;i++)
        {
            sr.nextLine();
        }

        while(sr.hasNextLine())
        {
            string =sr.nextLine();
            parts=string.split("\\s+");

            for(int i=0;i<diastasis;i++)
            {
                dimensions.add(Double.valueOf(parts[i+1]));
            }

            int counter=0;
            for(int i=0;i<givenCoor.size()/diastasis;i++)
            {
                if(dimensions.get(i)>=Math.min(givenCoor.get(i), givenCoor.get(i+diastasis)) && dimensions.get(i)<=Math.max(givenCoor.get(i), givenCoor.get(i+diastasis)))
                {
                    counter++;
                }
            }
            if(counter==diastasis)
            {
                inRange.add(new Records(parts[0],new ArrayList<>(dimensions)));
            }
            dimensions.clear();


        }
        sr.close();

        for(Records record:inRange)
        {
            record.showRecord();
        }
    }


}
