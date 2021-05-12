import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;

public class StarsParser extends DefaultHandler {
    HashSet<Star> starList;
    HashMap<String, HashSet<String>> allDirectors;
    HashMap<String, String> starIds = new HashMap<String, String>();
    private String tempVal;
    int skip = 0;
    //to maintain context
    private Star tempStar;
    private String director = "";
    FileWriter inconsistency;
    FileWriter inconsistencyAggregate;

    {
        try {
            inconsistency = new FileWriter("inconsistency.txt", true);
            inconsistencyAggregate = new FileWriter("inconsistencyAggregate.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String loginUser = "root";
    String loginPasswd = "Z0ccermysql";
    String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
    Connection connection;

    {
        try {
            connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public StarsParser(HashMap<String, HashSet<String>> dir) {
        allDirectors = dir;
        starList = new HashSet<Star>();
    }

    public StarsParser() {
        allDirectors = new HashMap<String, HashSet<String>>();
        starList = new HashSet<Star>();
    }
    public void runExample() {
        getStarIds();
        parseDocument();
        insertStarsInMovies();
        try {
            inconsistencyAggregate.write("Number of items skipped: " + skip);
            inconsistency.close();
            inconsistencyAggregate.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getStarIds(){
        String query = "SELECT * FROM stars;";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet results = statement.executeQuery();
            while (results.next()){
                String name = results.getString("name").toLowerCase();
                String id = results.getString("id");
                starIds.put(name, id);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
            sp.parse("casts124.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Iterate through the list and print
     * the contents
     */
    private void printData() {
        System.out.println("Number of movies = " + starList.size());

        for(Star item : starList){
            System.out.println(item.toString());
        }

    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        if (qName.equalsIgnoreCase("m")){
            tempStar = new Star();
            tempStar.setDirector(director);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
        if(tempVal.charAt(tempVal.length()-1) == ' '){
            tempVal = tempVal.substring(0, tempVal.length()-1);
        }
        tempVal = tempVal.replaceAll("\\\\", "");
        tempVal = tempVal.replaceAll("\'\'", "\'");
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("m")) {
            //add it to the list
            if(allDirectors.containsKey(tempStar.getDirector().toLowerCase())){
                if(allDirectors.get(tempStar.getDirector().toLowerCase()).contains(tempStar.getId().toLowerCase())){
                    if(starIds.containsKey(tempStar.getName().toLowerCase())) {
                        starList.add(tempStar);
                    }
                    else{
                        System.out.println("Actor " + tempStar.getName() + " does not exist");
                        skip++;
                        try {
                            inconsistency.write("Actor " + tempStar.getName() + " does not exist\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    System.out.println("Title " + tempStar.getId() + " does not exist");
                    skip++;
                    try {
                        inconsistency.write("Title " + tempStar.getId() + " does not exist\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                System.out.println("Director " + tempStar.getDirector() + " does not exist");
                skip++;
                try {
                    inconsistency.write("Director " + tempStar.getDirector() + " does not exist\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if (qName.equalsIgnoreCase("t")) {
            tempStar.setId(tempVal);
        } else if (qName.equalsIgnoreCase("a")) {
            tempStar.setName(tempVal);
        } else if (qName.equalsIgnoreCase("is")){
            director = tempVal;
        }

    }

    private void insertStarsInMovies(){
        try{
            connection.setAutoCommit(false);
            String query = "CALL linkStarMovieXML (?, ?, ?);\n";
            PreparedStatement statement = connection.prepareStatement(query);
            int count = 0;
            for(Star currentStar : starList){
                statement.setString(1, currentStar.getId());
                statement.setString(2, starIds.get(currentStar.getName().toLowerCase()));
                statement.setString(3, currentStar.getDirector());
                statement.addBatch();
                count++;
            }
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            inconsistencyAggregate.write("Done with Linking Stars to Movies. Count: " + count + "\n");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        StarsParser spe = new StarsParser();
        spe.runExample();
    }

}
