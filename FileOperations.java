import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.lang.model.element.NestingKind;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FileOperations{


    private final int dimensions;
    private final String fileName;


    private final ArrayList<Entry> entries;

    FileOperations(int dimensions,String fileName){
        this.dimensions=dimensions;
        this.fileName=fileName;
        entries=new ArrayList<>();
    }

    /**
     * This method is responsible for reading data from the "fileName" and puts the data on the
     * ArrayList<Entry> entries
     *
     * @throws FileNotFoundException When the "filename" file doesnt open for what ever reason we get a marked fail
     */


    public void Parsing() throws IOException, ParserConfigurationException, SAXException {

        try {
            File osmFile = new File(fileName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(osmFile);

            Element root = document.getDocumentElement();
            NodeList nodeList = root.getElementsByTagName("node");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element node = (Element) nodeList.item(i);
                String idValue = node.getAttribute("id");
                String lat = node.getAttribute("lat");
                String lon = node.getAttribute("lon");

                NodeList tagList = node.getElementsByTagName("tag");
                String nodeName = "";
                for (int j = 0; j < tagList.getLength(); j++) {
                    Element tag = (Element) tagList.item(j);
                    String key = tag.getAttribute("k");
                    if (key.equals("name")) {
                        nodeName = tag.getAttribute("v");
                        break;
                    }
                }

                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);

                ArrayList<Double> coordinates = new ArrayList<>();
                coordinates.add(latitude);
                coordinates.add(longitude);

                if (nodeName.isEmpty()) {
                    entries.add(new Entry(idValue, coordinates));
                } else {
                    ArrayList<String> name = new ArrayList<>();
                    name.add(nodeName);
                    entries.add(new Entry(idValue, coordinates, name));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print the entries
        for (Entry entry : entries) {
            System.out.print(entry.getId()+" ");
            System.out.print(entry.getCoordinates()+" ");
            System.out.print(entry.getName());
            System.out.println();
        }
    }



    /**
     *This method uses a buffer size of 32KB to save the ArrayList<Entry> entries to a file named datafile
     */
    public void writeDataFile(){
        File dataFile = new File("datafile");
        BufferedWriter buffer = null;

        try {
            buffer = new BufferedWriter(new FileWriter(dataFile));
            BufferedWriter buffer2 = buffer;

            int length;
            int blocks = 1;
            int lines = 1;
            int totalBytes;
            int bytesLeft = 32768;
            StringBuilder block0 = new StringBuilder();
            // creating the metadata block
            for (Entry entry : entries) {
                String temp = entry.getId() + " " + entry.getCoordinates();//Creating a string for the info of its entry
                if (entry.getName() != null) {
                    temp += " " + entry.getName();
                }
                length = temp.getBytes(StandardCharsets.UTF_8).length + 2;//getting the length of its entry in bytes
                totalBytes = bytesLeft - length;

                if (totalBytes >= 0) {//if the block has space decrease the total bytes and increase the line count or create a new block to write
                    bytesLeft -= length;
                    lines++;

                } else {
                    blocks += 1;
                    block0.append(blocks);
                    block0.append(" ").append(lines);
                    block0.append(" ").append(32768 - bytesLeft).append("\n");
                    lines = 1;
                    bytesLeft = 32768 - length;

                }
            }

            blocks += 1;
            block0.append(blocks);
            block0.append(" ").append(lines);
            block0.append(" ").append(32768 - bytesLeft).append("\n");

            String temp = 1 + " " + blocks + " ";
            int block1Bytes = temp.getBytes().length + block0.toString().getBytes().length + 2;

            block1Bytes = block1Bytes + String.valueOf(block1Bytes).getBytes().length - (blocks)
                    + String.valueOf(blocks + 1).getBytes().length;
            buffer2.write(String.valueOf(blocks + 1));
            buffer2.newLine();
            buffer2.write(temp + block1Bytes);
            buffer2.newLine();
            buffer2.write(block0.toString());

            for (Entry entry : entries) {
                temp = entry.getId() + " " + entry.getCoordinates();
                if (entry.getName() != null) {
                    temp += " " + entry.getName();
                }
                buffer2.write(temp);
                buffer2.newLine();
            }
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert buffer != null;
                buffer.close();
            } catch (Exception ignored) {
            }
        }
    }

}