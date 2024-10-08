import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

class RstarTree {
    private final int dimensions;
    private final String file;
    private final Node root;
    private final List<Integer> block_bytes=new ArrayList<>();
    private final List<Integer> block_lines=new ArrayList<>();
    private List<String> lines=new ArrayList<>();
    final int Max=4;
    final int Min=2;

    RstarTree(int dimensions, String file)
    {
        this.dimensions =dimensions;
        this.file=file;
        this.root=new Node(dimensions,null);
    }

    private void Get_Metadata() throws IOException
    {
        FileInputStream fis=new FileInputStream(new File("datafile"));

        BufferedReader br= new BufferedReader(new InputStreamReader(fis));

        String number_of_blocks= br.readLine();

        block_bytes.add(Integer.parseInt(number_of_blocks));
        block_lines.add(1);
        String string;
        String[] parts;

        for(int i=0;i<Integer.parseInt(number_of_blocks)-1;i++)
        {
            string=br.readLine();
            parts=string.split("\\s+");
            block_lines.add(Integer.parseInt(parts[1]));
            block_bytes.add(Integer.parseInt(parts[2]));
        }
        lines=Files.readAllLines(Paths.get("datafile"));
        br.close();

    }

    public void Results_Datafile(int line,int block)
    {
        int skip_lines=1;
        for(int i=1;i<block;i++)
        {
            skip_lines+=block_lines.get(i);
        }

        System.out.println(lines.get(skip_lines+line-1));


    }


    public void InsertNewEntry(int dimensions, List<Double> givenCoor, String id) throws IOException, ParserConfigurationException, SAXException {

        try {
            List<String> lines = Files.readAllLines(Paths.get(file));
            int index = lines.indexOf("</osm>");

            if (index != -1) {
                lines.add(index, "");
                lines.add(index,"</node>");
                lines.add(index,"<node id=\"" + id + "\" lat=\"" + givenCoor.get(0) + "\" lon=\"" + givenCoor.get(1) + "\">");
                lines.add(index,"");// Add a blank line before the new element
                Files.write(Paths.get(file), lines);
            } else {
                System.out.println("Invalid OSM file format. Missing closing </osm> tag.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        FileOperations fp=new FileOperations(dimensions,file);
        fp.Parsing();
        fp.writeDataFile();
        this.BuildRstarTree();

    }

    public void DeleteEntry(String idToDelete) throws IOException, ParserConfigurationException, SAXException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(file));
            int index = -1;

            // Find the index of the node to delete
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("<node id=\"" + idToDelete + "\"")) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Remove lines related to the node
                int startIndex = findStartOfNode(index, lines);
                int endIndex = findEndOfNode(index, lines);
                lines.subList(startIndex, endIndex + 1).clear();

                // Save the modified lines back to the file
                Files.write(Paths.get(file), lines);

                // Rebuild the data file
                FileOperations fp = new FileOperations(dimensions, file);
                fp.Parsing();
                fp.writeDataFile();

                // Rebuild the R*-tree
                this.BuildRstarTree();
            } else {
                System.out.println("Node with id " + idToDelete + " not found in the OSM file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findStartOfNode(int index, List<String> lines) {
        int i = index;
        while (i >= 0 && !lines.get(i).contains("<node")) {
            i--;
        }
        return i;
    }

    private int findEndOfNode(int index, List<String> lines) {
        int i = index;
        while (i < lines.size() && !lines.get(i).contains("</node>")) {
            i++;
        }
        return i;
    }


    public void RangeQuery(int dimensions,List<Double> givenCoor)
    {
        long startTime=System.nanoTime();

        Scanner sr=new Scanner(System.in);
        double[][] range=new double[dimensions][2];
        List<Record> results= new ArrayList<>();

        for(int i=0;i<2;i++)
        {
            for(int j=0;j<dimensions;j++)
            {
                range[j][i]=givenCoor.get(i*dimensions+j);
            }
        }

        for (int i = 0; i < dimensions; i++) {
            if(range[i][0]>range[i][1]){
                double temp = range[i][0];
                range[i][0] = range[i][1];
                range[i][1] = temp;
            }
        }

        for (int i = 0; i< dimensions; i++){
            range[i][0] -= 0.00000001;
            range[i][1] += 0.00000001;
        }

        LinkedList<Node> queue = new LinkedList<>();
        Node currNode;
        queue.add(root);


        while (queue.size() != 0) {
            currNode = queue.poll();
            boolean x=true;
            for(int i=0;i<dimensions;i++){
                if(Calculate_OverlapValue(range, currNode.getMbr())==0 ){
                    x=false;
                }
            }
            if(x){
                if (currNode.getChildren().size() != 0) {
                    queue.addAll(currNode.getChildren());
                } else {
                    for(int i=0;i<currNode.getRecords().size();i++){
                        x=true;
                        for(int j=0;j<dimensions;j++){
                            if (!(currNode.getRecords().get(i).getInfo().get(j) >= range[j][0] &&
                                    currNode.getRecords().get(i).getInfo().get(j) <= range[j][1])) {
                                x = false;
                                break;
                            }
                        }
                        if(x){
                            results.add(currNode.getRecords().get(i));
                        }
                    }
                }
            }
        }


        sr.close();
        long endTime =System.nanoTime();
        long duration =(endTime-startTime);
        for(Record result:results)
        {
            Results_Datafile(result.getLine(),result.getBlockID());
        }
        System.out.println("R star Tree RangeQueries: "+ duration/1000000+ "ms");
        System.out.println("R star Tree RangeQueries: "+ duration + "ns");

    }

    public void kNNQuery(int dim, int knn, List<Double> givenCoor)
    {
        long startTime = System.nanoTime();

        Scanner scanner = new Scanner(System.in);
        double[] point = new double[dim];
        Record[] results = new Record[knn];
        double[] distances = new double[knn];
        double maxdist=0;
        int inserted=0;

        for(int i=0;i<dim;i++){
            point[i] = givenCoor.get(i);
        }

        LinkedList<Node> queue = new LinkedList<>();
        Node currNode;
        int childSelected;
        queue.add(root);

        while (queue.size() != 0) {
            currNode = queue.poll();
            childSelected=0;
            double mindist = Double.MAX_VALUE;

            if (currNode.getChildren().size() == 0) {
                for (int i=0; i<currNode.getRecords().size();i++){
                    if(inserted<knn){
                        results[inserted]=currNode.getRecords().get(i);
                        distances[inserted]= euclideanDist(point,currNode.getRecords().get(i).getInfo());
                        inserted++;
                        if(distances[inserted-1]<maxdist){
                            maxdist=distances[inserted-1];
                        }
                        if(inserted==knn){
                            parallelBubbleSort(distances,results);
                        }
                    }else{
                        boolean flag=true;
                        int pos=knn;
                        for(int j=knn-1;j>=0 && flag;j--){
                            if(euclideanDist(point,currNode.getRecords().get(i).getInfo()) < distances[j]){
                                pos=j;
                            }else{
                                flag=false;
                            }
                        }
                        if(pos!=knn){
                            distances[knn-1] = euclideanDist(point,currNode.getRecords().get(i).getInfo());
                            results[knn-1] = currNode.getRecords().get(i);
                            if(distances[knn-1]<maxdist){
                                maxdist=distances[knn-1];
                            }
                            parallelBubbleSort(distances,results);
                        }
                    }
                }
            }else {
                for (int i = 0; i < currNode.getChildren().size();i++) {
                    double nodeDist = MINDIST(point, currNode.getChildren().get(i).getMbr());
                    if (nodeDist < mindist) {
                        mindist = nodeDist;
                        childSelected = i;
                    }
                }
                for (int i =0; i < currNode.getChildren().size();i++) {
                    if(i!=childSelected){
                        queue.add(currNode.getChildren().get(i));
                    }
                }
                queue.add(currNode.getChildren().get(childSelected));
            }
        }

        long endTime = System.nanoTime();

        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        for (int i = 0; i < knn; i++) {
            Results_Datafile(results[i].getLine(), results[i].getBlockID());
        }
        System.out.println("R* Tree KNN: " + duration/1000000 + "ms");
        System.out.println("R* Tree KNN: " + duration + "ns");
        scanner.close();  // Closes the scanner
    }

    static void parallelBubbleSort(double[] arr, Record[] arr2) {
        int n = arr.length;
        double temp;
        Record temp2;
        for (int i = 0; i < n; i++) {
            for (int j = 1; j < (n - i); j++) {
                if (arr[j - 1] > arr[j]) {
                    //swap elements
                    temp = arr[j - 1];
                    arr[j - 1] = arr[j];
                    arr[j] = temp;
                    temp2 = arr2[j - 1];
                    arr2[j - 1] = arr2[j];
                    arr2[j] = temp2;
                }
            }
        }
    }

    public double euclideanDist(double[] point, List<Double> record){
        double dist = 0 ;
        for(int i = 0; i< dimensions; i++){
            dist += Math.pow(point[i]-record.get(i),2);
        }
        dist = Math.sqrt(dist);
        return dist;
    }

    public double MINDIST(double[] point, double[][] mbr){
        double rj, pj;
        double sum = 0;

        for(int i = 0; i< dimensions; i++){
            pj = point[i];
            if(pj<mbr[i][0]) {
                rj = mbr[i][0];
            }else rj = Math.min(pj, mbr[i][1]);

            sum+=Math.pow(Math.abs(pj-rj),2);
        }
        sum = Math.sqrt(sum);
        return sum;
    }

    public  void SkylineQuery(int dimensions) throws IOException {
        List<Record> skylineResult = new ArrayList<>();
        long startTime = System.nanoTime();
        SkylineHelper(root,skylineResult,dimensions);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.

        for(Record skyLine: skylineResult) {
            System.out.println(skyLine.getID()+" "+skyLine.getInfo());
        }

        System.out.println("R* Tree Skyline Query: " + duration/1000000 + "ms");
        System.out.println("R* Tree SkylineQuery: " + duration + "ns");

    }

    private void SkylineHelper(Node node, List<Record> skylineResult,int dimensions) throws IOException {
        if (node.getChildren().size()==0) {
            for (Record record : node.getRecords()) {
                if (!isNotSkyline(record, skylineResult,dimensions)) {
                    skylineResult.add(record);
                }
            }
        } else {
            for (Node child : node.getChildren()) {
                SkylineHelper(child, skylineResult,dimensions);
            }
        }
    }

    private boolean isNotSkyline(Record record, List<Record> skylineResult,int dimensions) {
        int Dom=-1;
        for (Record skylineRecord : skylineResult) {
            Dom=dominates(record, skylineRecord,dimensions);
            if ((Dom>=1)&&(Dom<dimensions)) {
                return false;
            }else if(Dom==dimensions){
                skylineResult.remove(skylineRecord);
                return false;
            }
        }
        if(Dom==0){
            return true;
        }
        return false;
    }

    private int dominates(Record a, Record b,int dimensions) {
        int dominationCounter=0;
        for (int i = 0; i < dimensions; i++) {
            if(a.getInfo().get(i).compareTo(b.getInfo().get(i))>0){
                dominationCounter+=1;
            }
        }
        return dominationCounter;
    }

    public void bottomUpRstarTreeBuild() throws IOException {

        List<Node> leafNodes = bulkLoad();
        while (leafNodes.size() > 1) {
            List<Node> nextLevelNodes = new ArrayList<>();
            for (int i = 0; i < leafNodes.size(); i += Max) {
                int endIndex = Math.min(i + Max, leafNodes.size());
                List<Node> group = leafNodes.subList(i, endIndex);
                Node mergedNode = createParentNode(group);
                nextLevelNodes.add(mergedNode);
            }
            leafNodes = nextLevelNodes;
        }
        Node root1 = leafNodes.get(0);
        //Bfs(root1); //Evaluation of the R* tree build (schema creation)
    }


    public List<Node> bulkLoad() throws IOException {
        List<Node> records=new ArrayList<>();
        Scanner scanner = new Scanner(new File("datafile"));
        String string;
        String[] parts;
        ArrayList<Double> coordinates;
        ArrayList<String> name;
        List<String> metadata = new ArrayList<>();
        int last_line_hack = 0;
        // Create a list with the number of lines in each block from the metadata
        metadata.add(scanner.nextLine()); // first element of the list the lines of the metadata
        for (int i = 0; i < Integer.parseInt(metadata.get(0))-1; i++) {
            string = scanner.nextLine();
            parts = string.split("\\s+");
            metadata.add(parts[1]);
        }
        // Loop through the File lines to get id and coordinates based on dimension parameter
        // We need the following csv format:
        // 1st element = id
        // Rest dimension elements (e.g. 2) = coordinates
        for (int i = 2; i < Integer.parseInt(metadata.get(0)); i++) {
            if (i==Integer.parseInt(metadata.get(0))-1){
                last_line_hack = 1;
            }
            for (int j = 0; j < Integer.parseInt(metadata.get(i))-last_line_hack; j++) {
                string = scanner.nextLine();
                parts = string.split("\\s+");
                coordinates = new ArrayList<>();
                name=new ArrayList<>();
                for (int q = 0; q < dimensions; q++) {
                    coordinates.add(Double.valueOf(parts[q+1]));
                }
                name.add(String.valueOf(parts[dimensions]));
                Node node=new  Node(dimensions,null);
                Record record=new Record(parts[0], (ArrayList<Double>) coordinates,j+1, i,name);
                node.addRecord(record);
                node.adjustMbr(record);
                records.add(node); // j is line inside block, i is blockID

            }
        }

        scanner.close();
        return records;
    }

    public Node createParentNode(List<Node> children) {
        Node parentNode = new Node(dimensions, null); // Create a new parent node
        parentNode.setLeaf(false); // The parent is not a leaf

        for (Node child : children) {
            parentNode.getChildren().add(child); // Add the child to the parent's children
            parentNode.adjustMbr(child); // Adjust the parent's MBR based on the child's MBR
            child.setParent(parentNode); // Set the parent of the child to the new parent
        }

        return parentNode;
    }



    // Construct the IndexFile based on the R-Tree made from function BuildRTree.

    public void WriteIndexFile() throws FileNotFoundException {
        Bfs(root);
    }

    private void Bfs(Node root) throws FileNotFoundException {

        PrintWriter out = new PrintWriter("indexfile");
        int id=0;
        int fatherId;
        int childrenId=1;
        LinkedList<Node> queue = new LinkedList<>();
        LinkedList<Integer> queueF = new LinkedList<>();
        queue.add(root);
        queueF.add(id);
        Node currNode;

        while (queue.size() != 0) {
            currNode = queue.poll();
            fatherId = queueF.poll();

            //START OF PRINTING A NODE'S DETAILS

            id++;
            out.println(id);
            for(int i = 0; i< dimensions; i++){
                out.print(currNode.getMbr()[i][0] + " ");
            }
            for(int i = 0; i< dimensions; i++){
                out.print(currNode.getMbr()[i][1] + " ");
            }
            if(currNode.getChildren().size()!=0){
                out.print("\n");
            }
            for(int i=0;i<currNode.getChildren().size();i++){
                childrenId++;
                out.print(childrenId + " ");
            }
            if(currNode.getChildren().size()==0){                 // THIS IS TO LOOK FOR FIRST CHILD FOUND, SO THAT WE

                out.println();
                for(int i=0;i<currNode.getRecords().size();i++){
                    for(int j = 0; j< dimensions; j++){
                        out.print(currNode.getRecords().get(i).getInfo().get(j) + " ");
                    }
                    out.println(currNode.getRecords().get(i).getLine());
                }
                for(int i=0;i<Max-currNode.getRecords().size();i++){
                    out.println(-1);
                }
            }
            else{
                out.print("\n");
            }
            out.println(fatherId+"\n");

            //END OF PRINTING A NODE'S DETAILS

            for(int i=0;i<currNode.getChildren().size();i++){
                queue.add(currNode.getChildren().get(i));
                queueF.add(id);
            }
        }
        out.close();
    }




    /**
     * The main function of the class for building the tree.
     * @throws IOException
     */
    public void BuildRstarTree() throws IOException {
        // Parsing a  file into Scanner class constructor
        Scanner scanner = new Scanner(new File("datafile"));
        String string;
        String[] parts;
        ArrayList<Double> coordinates;
        ArrayList<String> name;
        List<String> metadata = new ArrayList<>();
        int last_line_hack = 0;
        // Create a list with the number of lines in each block from the metadata
        metadata.add(scanner.nextLine()); // first element of the list the lines of the metadata
        for (int i = 0; i < Integer.parseInt(metadata.get(0))-1; i++) {
            string = scanner.nextLine();
            parts = string.split("\\s+");
            metadata.add(parts[1]);
        }
        // Loop through the File lines to get id and coordinates based on dimension parameter
        // We need the following csv format:
        // 1st element = id
        // Rest dimension elements (e.g. 2) = coordinates
        for (int i = 2; i < Integer.parseInt(metadata.get(0)); i++) {
            if (i==Integer.parseInt(metadata.get(0))-1){
                last_line_hack = 1;
            }
            for (int j = 0; j < Integer.parseInt(metadata.get(i))-last_line_hack; j++) {
                string = scanner.nextLine();
                parts = string.split("\\s+");
                coordinates = new ArrayList<>();
                name=new ArrayList<>();
                for (int q = 0; q < dimensions; q++) {
                    coordinates.add(Double.valueOf(parts[q+1]));
                }
                name.add(String.valueOf(parts[dimensions]));
                Insert(new Record(parts[0], (ArrayList<Double>) coordinates,j+1, i,name), root); // j is line inside block, i is blockID
            }
        }



        scanner.close();  // Closes the scanner
        Get_Metadata();


    }

    /**
     * Function to insert a new entry into the R*Tree.
     * @param entry
     * @param currNode
     */
    private void Insert(Record entry, Node currNode){

        // Choose in which node we will try to insert the new entry
        Node selected =ChooseSubtree(currNode, entry);

        // If node has less than M entries, then insert the entry into this node.
        if (selected.getRecords().size()<Max){
            selected.getRecords().add(entry);
            selected.adjustMbr(entry);
        }

        // Else if node has M entries, then invoke OverflowTreatment
        else{
            OverflowTreatment( selected, entry);
        }

    }

    /**
     * According to the papers about the R*tree, the OverflowTreatment function should try to first Remove p elements
     * from the overflown node, and then ReInsert them to the tree again to sometimes prevent splits.
     * However, since we were asked to not implement a Remove/Delete function, we are also not able to implement the
     * ReInsert function as well.
     *
     * As a result, the OverflowTreatment function will only invoke the Split function.
     * @param overNode
     * @param extraEntry
     */
    private void OverflowTreatment( Node overNode, Record extraEntry ){
        Split_Leaf(overNode,extraEntry);
    }

    /**
     * Function to choose the appropriate insertion path and reach the node where we will insert a new entry.
     * @param currNode
     * @param entry
     * @return
     */
    private Node ChooseSubtree(Node currNode, Record entry){

        currNode.adjustMbr(entry);
        // If N is leaf, return N
        if (currNode.getChildren().size()==0) {
            return currNode;
        }

        // Else if the childpointers of N point to leaves, choose the child whose rectangle needs least overlap enlargement
        else if(currNode.getChildren().get(0).isLeaf()){
            int numOfChildren = currNode.getChildren().size();
            double [] Overlap = new double[numOfChildren];
            for (int i=0;i<numOfChildren;i++){
                for (int j=0;j<numOfChildren;j++){
                    if(i!=j){
                        Overlap[i]-=Calculate_OverlapValue(currNode.getChildren().get(i).getMbr(),currNode.getChildren().get(j).getMbr());
                    }
                }
                Node temp = new Node(dimensions, null);
                temp.adjustMbr(entry);
                temp.adjustMbr(currNode.getChildren().get(i));
                for (int j=0;j<numOfChildren;j++){
                    if(i!=j){
                        Overlap[i]+=Calculate_OverlapValue(temp.getMbr(),currNode.getChildren().get(j).getMbr());
                    }
                }
            }
            int minNode=0;
            double minOverlap = Overlap[0];
            for (int i=1;i<numOfChildren;i++){
                if (Overlap[i]<minOverlap){
                    minOverlap=Overlap[i];
                    minNode=i;
                }
            }

            return ChooseSubtree(currNode.getChildren().get(minNode),entry);
        }

        // Else if the childpointers of N point to non-leaves, choose child whose rectangle needs least area enlargement
        else{
            int numOfChildren = currNode.getChildren().size();
            double [] Area = new double[numOfChildren];
            for (int i=0;i<numOfChildren;i++){
                Area[i]-=Calculate_Area(currNode.getChildren().get(i).getMbr());
                Node temp = new Node(dimensions, null);
                temp.adjustMbr(entry);
                temp.adjustMbr(currNode.getChildren().get(i));
                Area[i]+=Calculate_Area(temp.getMbr());
            }
            int minNode=0;
            double minArea = Area[0];
            for (int i=1;i<numOfChildren;i++){
                if (Area[i]<minArea){
                    minArea=Area[i];
                    minNode=i;
                }
            }
            return ChooseSubtree(currNode.getChildren().get(minNode),entry);
        }
    }


    /**
     * Function to perform a node split ( NON-LEAF ).
     * @param parent
     * @param extraNode
     */
    private void Split_NonLeaf(Node parent, Node extraNode){
        int axis;
        List<List<Node>> bestDistribution;

        //Invoke ChooseSplitAxis_NonLeaf to determine the axis of the performed split
        axis = ChooseSplitAxis_NonLeaf(parent, extraNode);

        //Invoke ChooseSplitIndex_NonLeaf to determine the best distribution into 2 groups along the axis selected
        bestDistribution = ChooseSplitIndex_NonLeaf(parent, extraNode, axis);

        //If we are at root node, then create 2 new nodes
        if (parent.getParent()==null) {
            parent.adjustMbr(extraNode);

            Node child1 = new Node(dimensions, parent);
            Node child2 = new Node(dimensions, parent);

            for (int i = 0; i < bestDistribution.get(0).size(); i++) {          //Node of group no.1
                bestDistribution.get(0).get(i).setParent(child1);
                child1.getChildren().add(bestDistribution.get(0).get(i));
                child1.adjustMbr(bestDistribution.get(0).get(i));
            }
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                bestDistribution.get(1).get(i).setParent(child2);
                child2.getChildren().add(bestDistribution.get(1).get(i));
                child2.adjustMbr(bestDistribution.get(1).get(i));
            }
            parent.getChildren().clear();
            parent.getChildren().add(child1);
            parent.getChildren().add(child2);
        }
        //Else if parent node has M children, then split him too and go upwards
        else if(parent.getParent().getChildren().size()==Max){
            Node child = new Node(dimensions,null);
            for (int i=0;i<bestDistribution.get(0).size();i++){                 //Node of group no.1
                bestDistribution.get(0).get(i).setParent(child);
                child.getChildren().add(bestDistribution.get(0).get(i));
                child.adjustMbr(bestDistribution.get(0).get(i));
            }
            parent.getChildren().clear();
            parent.clearMbr();
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                bestDistribution.get(1).get(i).setParent(parent);
                parent.getChildren().add(bestDistribution.get(1).get(i));
                parent.adjustMbr(bestDistribution.get(1).get(i));
            }

            // RECURSION!
            // Now the parent node has more than M children so we have to split them.
            Split_NonLeaf(parent.getParent(),child);
        }
        //Else if parent node has less than M children, then create only 1 new node;
        else if(parent.getParent().getChildren().size()<Max){
            parent.getParent().adjustMbr(extraNode);
            Node child = new Node(dimensions,parent.getParent());
            for (int i=0;i<bestDistribution.get(0).size();i++){                 //Node of group no.1
                bestDistribution.get(0).get(i).setParent(child);
                child.getChildren().add(bestDistribution.get(0).get(i));
                child.adjustMbr(bestDistribution.get(0).get(i));
            }

            parent.getChildren().clear();
            parent.clearMbr();
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                bestDistribution.get(1).get(i).setParent(parent);
                parent.getChildren().add(bestDistribution.get(1).get(i));
                parent.adjustMbr(bestDistribution.get(1).get(i));
            }
            parent.getParent().getChildren().add(child);
        }
    }

    /**
     * Function to provide the new distributions so that we can create new nodes during a split ( NON-LEAF ).
     * @param parent
     * @param extraNode
     * @param axis
     * @return
     */
    private List<List<Node>> ChooseSplitIndex_NonLeaf(Node parent, Node extraNode, int axis) {
        List<Node> sortedNodes1 = new ArrayList<>();
        List<Node> sortedNodes2 = new ArrayList<>();
        List<List<Node>> bestDistribution;

        sortedNodes1.add(extraNode);
        sortedNodes2.add(extraNode);
        for (int j = 0; j < Max; j++) {
            sortedNodes1.add(parent.getChildren().get(j));
            sortedNodes2.add(parent.getChildren().get(j));
        }

        // Sort based on the lower value of the rectangles
        sortedNodes1.sort(Comparator.comparingDouble(o -> o.getMbr()[axis][0]));

        // Sort based on the upper value of the rectangles
        sortedNodes2.sort(Comparator.comparingDouble(o -> o.getMbr()[axis][1]));

        bestDistribution = Find_Best_Distribution_NonLeaf(sortedNodes1,sortedNodes2);
        return bestDistribution;
    }


    /**
     * Function to find the best possible distribution during a split ( NON-LEAF )
     * @param sortedNodes1
     * @param sortedNodes2
     * @return
     */
    private List<List<Node>> Find_Best_Distribution_NonLeaf(List<Node> sortedNodes1, List<Node> sortedNodes2){
        double overlap;
        double area;

        List<Node> group1 = new ArrayList<>();
        List<Node> group2 = new ArrayList<>();

        List<List<Node>> bestDistributionOverlap1 = new ArrayList<>();
        List<List<Node>> bestDistributionArea1 = new ArrayList<>();

        double[][] mbr1;
        double[][] mbr2;
        double minOverlap1 = Double.MAX_VALUE;
        double minArea1 = Double.MAX_VALUE;

        for (int k=1;k<=Max-2*Min+2;k++){
            for (int i=0;i<Min-1+k;i++){
                group1.add(sortedNodes1.get(i));
            }
            for (int i=Min-1+k;i<sortedNodes1.size();i++){
                group2.add(sortedNodes1.get(i));
            }

            mbr1 = Calculate_Mbr_NonLeaf(group1);
            mbr2 = Calculate_Mbr_NonLeaf(group2);

            overlap = Calculate_OverlapValue(mbr1,mbr2);
            area = Calculate_Area(mbr1) + Calculate_Area(mbr2);

            if(area < minArea1){
                minArea1 = area;
                bestDistributionArea1.clear();
                bestDistributionArea1.add(new ArrayList<>(group1));
                bestDistributionArea1.add(new ArrayList<>(group2));
            }

            if (overlap < minOverlap1){
                minOverlap1 = overlap;
                bestDistributionOverlap1.clear();
                bestDistributionOverlap1.add(new ArrayList<>(group1));
                bestDistributionOverlap1.add(new ArrayList<>(group2));
            }
            group1.clear();
            group2.clear();
        }

        List<List<Node>> bestDistributionOverlap2 = new ArrayList<>();
        List<List<Node>> bestDistributionArea2 = new ArrayList<>();

        double minOverlap2 = Double.MAX_VALUE;
        double minArea2 = Double.MAX_VALUE;

        for (int k=1;k<=Max-2*Min+2;k++){
            for (int i=0;i<Min-1+k;i++){
                group1.add(sortedNodes2.get(i));
            }
            for (int i=Min-1+k;i<sortedNodes2.size();i++){
                group2.add(sortedNodes2.get(i));
            }

            mbr1 = Calculate_Mbr_NonLeaf(group1);
            mbr2 = Calculate_Mbr_NonLeaf(group2);

            overlap = Calculate_OverlapValue(mbr1,mbr2);
            area = Calculate_Area(mbr1) + Calculate_Area(mbr2);

            if(area < minArea2){
                minArea2 = area;
                bestDistributionArea2.clear();
                bestDistributionArea2.add(new ArrayList<>(group1));
                bestDistributionArea2.add(new ArrayList<>(group2));
            }

            if (overlap < minOverlap2){
                minOverlap2 = overlap;
                bestDistributionOverlap2.clear();
                bestDistributionOverlap2.add(new ArrayList<>(group1));
                bestDistributionOverlap2.add(new ArrayList<>(group2));
            }
            group1.clear();
            group2.clear();
        }

        if(minOverlap1==0){
            if(minOverlap2!=0){
                return bestDistributionArea1;
            }else{
                if (minArea1<=minArea2){
                    return bestDistributionArea1;
                }else{
                    return bestDistributionArea2;
                }
            }
        }else{
            if(minOverlap2==0){
                return bestDistributionArea2;
            }else{
                if (minOverlap1<=minOverlap2){
                    return bestDistributionOverlap1;
                }else{
                    return bestDistributionOverlap2;
                }
            }
        }
    }

    /**
     * Function to determine the axis, perpendicular to which the split is performed ( NON-LEAF ).
     * @param parent
     * @param extraNode
     * @return
     */
    private int ChooseSplitAxis_NonLeaf(Node parent, Node extraNode){
        double [] S = new double[dimensions];

        for (int axis = 0; axis< dimensions; axis++){
            List<Node> sortedNodes = new ArrayList<>();

            sortedNodes.add(extraNode);
            for (int j=0;j<Max;j++){
                sortedNodes.add(parent.getChildren().get(j));
            }

            int axisUsed = axis;
            for (int i=0;i<2;i++){      // Calculate S according to the upper and lower values of the rectangles
                int iUsed = i;
                sortedNodes.sort(Comparator.comparingDouble(o -> o.getMbr()[axisUsed][iUsed]));
                S[axis] += Calculate_S_NonLeaf(sortedNodes);
            }
        }

        double min = S[0];
        int minAxis = 0;
        for (int i = 0; i< dimensions; i++){
            //System.out.println(S[i]);
            if (S[i]<min){
                min=S[i];
                minAxis=i;
            }
        }
        return minAxis;
    }

    /**
     * Function to calculate the sum of all the margin-values of the different distributions ( NON-LEAF ).
     * @param sortedNodes
     * @return
     */
    private double Calculate_S_NonLeaf(List<Node> sortedNodes){
        double sum = 0;
        List<Node> group1 = new ArrayList<>();
        List<Node> group2 = new ArrayList<>();
        double[][] mbr1;
        double[][] mbr2;

        for (int k=1;k<=Max-2*Min+2;k++) {
            for (int i = 0; i < Min - 1 + k; i++) {
                group1.add(sortedNodes.get(i));
            }
            for (int i = Min - 1 + k; i < sortedNodes.size(); i++) {
                group2.add(sortedNodes.get(i));
            }
            mbr1 = Calculate_Mbr_NonLeaf(group1);
            mbr2 = Calculate_Mbr_NonLeaf(group2);

            sum+=Calculate_MarginValue(mbr1);
            sum+=Calculate_MarginValue(mbr2);
            group1.clear();
            group2.clear();
        }
        return sum;
    }

    /**
     * Function to calculate the mbr of each of the two groups during a node-split ( NON-LEAF ).
     * @param group
     * @return
     */
    private double[][] Calculate_Mbr_NonLeaf(List<Node> group){
        double[][] mbr = new double[dimensions][2];

        for(int i = 0; i< dimensions; i++){
            for (Node node : group) {
                double upper = node.getMbr()[i][1];
                double lower = node.getMbr()[i][0];

                // In every dimension axis check if we need to adjust any of the upper or lower bounds of the mbr.
                if (lower < mbr[i][0] || mbr[i][0] == 0) {         //Check lower bound of mbr
                    mbr[i][0] = lower;
                }
                if (upper > mbr[i][1] || mbr[i][1] == 0) {         //Check upper bound of mbr
                    mbr[i][1] = upper;
                }
            }
        }
        return mbr;
    }

    /**
     * Function to perform a node split ( LEAF ).
     * @param overNode
     * @param extraEntry
     */
    private void Split_Leaf(Node overNode, Record extraEntry){

        int axis;
        List<List<Record>> bestDistribution;

        //Invoke ChooseSplitAxis_Leaf to determine the axis of the performed split
        axis = ChooseSplitAxis_Leaf(overNode, extraEntry);

        //Invoke ChooseSplitIndex_Leaf to determine the best distribution into 2 groups along the axis selected
        bestDistribution = ChooseSplitIndex_Leaf(overNode, extraEntry, axis);

        //If we are at root node, then create 2 new nodes
        if (overNode.getParent()==null){
            overNode.adjustMbr(extraEntry);
            Node child1 = new Node(dimensions,overNode);
            Node child2 = new Node(dimensions,overNode);
            for (int i=0;i<bestDistribution.get(0).size();i++){                 //Node of group no.1
                child1.getRecords().add(bestDistribution.get(0).get(i));
                child1.adjustMbr(bestDistribution.get(0).get(i));
            }
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                child2.getRecords().add(bestDistribution.get(1).get(i));
                child2.adjustMbr(bestDistribution.get(1).get(i));
            }
            overNode.getChildren().add(child1);
            overNode.getChildren().add(child2);
            overNode.setLeaf(false);
        }
        //Else if parent node has M children, then split him too and go upwards
        else if(overNode.getParent().getChildren().size()==Max){
            Node child = new Node(dimensions,null);
            for (int i=0;i<bestDistribution.get(0).size();i++){                 //Node of group no.1
                child.getRecords().add(bestDistribution.get(0).get(i));
                child.adjustMbr(bestDistribution.get(0).get(i));
            }
            overNode.getRecords().clear();
            overNode.clearMbr();
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                overNode.getRecords().add(bestDistribution.get(1).get(i));
                overNode.adjustMbr(bestDistribution.get(1).get(i));
            }

            // Now the parent node has more than M children so we have to split them.
            Split_NonLeaf(overNode.getParent(),child);
        }
        //Else if parent node has less than M children, then create only 1 new node;
        else if(overNode.getParent().getChildren().size()<Max){
            overNode.getParent().adjustMbr(extraEntry);
            Node child = new Node(dimensions,overNode.getParent());
            for (int i=0;i<bestDistribution.get(0).size();i++){                 //Node of group no.1
                child.getRecords().add(bestDistribution.get(0).get(i));
                child.adjustMbr(bestDistribution.get(0).get(i));
            }

            overNode.getRecords().clear();
            overNode.clearMbr();
            for (int i=0;i<bestDistribution.get(1).size();i++){                 //Node of group no.2
                overNode.getRecords().add(bestDistribution.get(1).get(i));
                overNode.adjustMbr(bestDistribution.get(1).get(i));
            }
            overNode.getParent().getChildren().add(child);
            overNode.getParent().setLeaf(false);                    //Might be unnecessary
        }
    }

    /**
     * Function to determine the axis, perpendicular to which the split is performed ( LEAF ).
     * @param overNode
     * @param extraEntry
     * @return
     */
    private int ChooseSplitAxis_Leaf(Node overNode, Record extraEntry){
        double [] S = new double[dimensions];

        for (int axis = 0; axis< dimensions; axis++){
            List<Record> sortedEntries = new ArrayList<>();

            sortedEntries.add(extraEntry);
            for (int j=0;j<Max;j++){
                sortedEntries.add(overNode.getRecords().get(j));
            }

            int axisUsed = axis;

            sortedEntries.sort(Comparator.comparingDouble(o -> o.getInfo().get(axisUsed)));

            S[axis] = Calculate_S_Leaf(sortedEntries);
        }

        double min = S[0];
        int minAxis = 0;
        for (int i = 0; i< dimensions; i++){
            //System.out.println(S[i]);
            if (S[i]<min){
                min=S[i];
                minAxis=i;
            }
        }
        return minAxis;
    }


    /**
     * Function to provide the new distributions so that we can create new nodes during a split ( LEAF ).
     * @param overNode
     * @param extraEntry
     * @param axis
     * @return
     */
    private List<List<Record>> ChooseSplitIndex_Leaf(Node overNode, Record extraEntry, int axis) {

        List<Record> sortedEntries = new ArrayList<>();
        List<List<Record>> bestDistribution;

        sortedEntries.add(extraEntry);
        for (int j = 0; j < Max; j++) {
            sortedEntries.add(overNode.getRecords().get(j));
        }

        sortedEntries.sort(Comparator.comparingDouble(o -> o.getInfo().get(axis)));

        bestDistribution = Find_Best_Distribution_Leaf(sortedEntries);
        return bestDistribution;
    }


    /**
     * Function to find the best possible distribution during a split ( LEAF ).
     * @param sortedEntries
     * @return
     */
    private List<List<Record>> Find_Best_Distribution_Leaf(List<Record> sortedEntries){
        double [] overlap = new double[Max-2*Min+2];
        double [] area = new double[Max-2*Min+2];

        List<Record> group1 = new ArrayList<>();
        List<Record> group2 = new ArrayList<>();

        List<List<Record>> bestDistributionOverlap = new ArrayList<>();
        List<List<Record>> bestDistributionArea = new ArrayList<>();

        double[][] mbr1;
        double[][] mbr2;
        double minOverlap = Double.MAX_VALUE ;
        double minArea = Double.MAX_VALUE ;

        for (int k=1;k<=Max-2*Min+2;k++){
            for (int i=0;i<Min-1+k;i++){
                group1.add(sortedEntries.get(i));
            }
            for (int i=Min-1+k;i<sortedEntries.size();i++){
                group2.add(sortedEntries.get(i));
            }

            mbr1 = Calculate_Mbr_Leaf(group1);
            mbr2 = Calculate_Mbr_Leaf(group2);

            overlap[k-1] = Calculate_OverlapValue(mbr1,mbr2);
            area[k-1] = Calculate_Area(mbr1) + Calculate_Area(mbr2);

            if(area[k-1] < minArea){
                minArea = area[k-1];
                bestDistributionArea.clear();
                bestDistributionArea.add(new ArrayList<>(group1));
                bestDistributionArea.add(new ArrayList<>(group2));
            }

            if (overlap[k-1] < minOverlap){
                minOverlap = overlap[k-1];
                bestDistributionOverlap.clear();
                bestDistributionOverlap.add(new ArrayList<>(group1));
                bestDistributionOverlap.add(new ArrayList<>(group2));
            }
            group1.clear();
            group2.clear();
        }

        if(minOverlap==0){
            return bestDistributionArea;
        }else{
            return bestDistributionOverlap;
        }
    }

    // Function to calculate the area-value of a given mbr.
    private double Calculate_Area(double[][] mbr){
        double area = 1;
        for(int i = 0; i< dimensions; i++){
            area = area * Math.abs(mbr[i][0]-mbr[i][1]);
        }
        return area;
    }

    // Function to calculate the overlap-value of 2 given mbrs.
    private double Calculate_OverlapValue(double[][] mbr1, double[][] mbr2){
        double overlap = 1;
        for(int i = 0; i< dimensions; i++){
            overlap = overlap * Math.max(0,Math.min(mbr1[i][1],mbr2[i][1])-Math.max(mbr1[i][0],mbr2[i][0]));
        }
        return overlap;
    }

    /**
     * Function to calculate the sum of all the margin-values of the different distributions ( LEAF ).
     * @param sortedEntries
     * @return
     */
    private double Calculate_S_Leaf(List<Record> sortedEntries){
        double sum = 0;
        List<Record> group1 = new ArrayList<>();
        List<Record> group2 = new ArrayList<>();
        double[][] mbr1;
        double[][] mbr2;

        for (int k=1;k<=Max-2*Min+2;k++){
            for (int i=0;i<Min-1+k;i++){
                group1.add(sortedEntries.get(i));
            }
            for (int i=Min-1+k;i<sortedEntries.size();i++){
                group2.add(sortedEntries.get(i));
            }

            mbr1 = Calculate_Mbr_Leaf(group1);
            mbr2 = Calculate_Mbr_Leaf(group2);

            sum+=Calculate_MarginValue(mbr1);
            sum+=Calculate_MarginValue(mbr2);
            group1.clear();
            group2.clear();
        }
        return sum;
    }

    /**
     * Function to calculate the mbr of each of the two groups during a node-split ( LEAF ).
     * @param group
     * @return
     */
    private double[][] Calculate_Mbr_Leaf(List<Record> group){
        double[][] mbr = new double[dimensions][2];

        for(int i = 0; i< dimensions; i++){
            for (Record record : group) {
                double entryCoord = record.getInfo().get(i);

                // In every dimension axis check if we need to adjust any of the upper or lower bounds of the mbr.
                if (entryCoord < mbr[i][0] || mbr[i][0] == 0) {         //Check lower bound of mbr
                    mbr[i][0] = entryCoord;
                }
                if (entryCoord > mbr[i][1] || mbr[i][1] == 0) {         //Check upper bound of mbr
                    mbr[i][1] = entryCoord;
                }
            }
        }
        return mbr;
    }

    /**
     * Function to calculate the margin-value of a given mbr.
     * @param mbr
     * @return
     */
    private double Calculate_MarginValue(double[][] mbr){
        double sum=0;
        for(int i = 0; i< dimensions; i++){
            sum+=Math.abs(mbr[i][0]-mbr[i][1]);
        }
        sum = sum * Math.pow(2,(dimensions -1));    // formula to calculate a bounding rectangle's margin-value in k-dimensions
        //System.out.println(sum);
        return sum;
    }


}