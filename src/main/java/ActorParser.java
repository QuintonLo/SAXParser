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

public class ActorParser extends DefaultHandler {
    HashSet<Actor> actorList;
    HashMap<String, HashSet<Integer>> allActors = new HashMap<String, HashSet<Integer>>();
    private String tempVal;
    private Actor tempActor;
    int dup = 0;
    int skip = 0;
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

    public ActorParser() {
        actorList = new HashSet<Actor>();
    }

    public void runExample() {
        completeActors();
        parseDocument();
        insertStarsInMovies();

        try {
            inconsistencyAggregate.write("Number of items skipped: " + skip + "\n");
            inconsistencyAggregate.write("Number of duplicate items: " + dup + "\n\n");
            inconsistency.close();
            inconsistencyAggregate.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void completeActors() {
        String query = "SELECT name, birthYear FROM stars";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet results = statement.executeQuery();
            while(results.next()){
                String name = results.getString("name").toLowerCase();
                int birthYear = results.getInt("birthYear");
                if(allActors.containsKey(name)){
                    allActors.get(name).add(birthYear);
                }
                else{
                    HashSet<Integer> birthYears= new HashSet<Integer>();
                    birthYears.add(birthYear);
                    allActors.put(name, birthYears);
                }

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
            sp.parse("actors63.xml", this);

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
        System.out.println("Number of movies = " + actorList.size());

        for(Actor item : actorList){
            System.out.println(item.toString());
        }

    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        if (qName.equalsIgnoreCase("actor")){
            tempActor = new Actor();
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
        if(tempVal.charAt(0) == ' '){
            tempVal = tempVal.substring(1);
        }
        if(tempVal.length() > 0){
            if(tempVal.charAt(tempVal.length()-1) == ' '){
                tempVal = tempVal.substring(0, tempVal.length()-1);
            }
        }
        tempVal = tempVal.replaceAll("\\\\", "");
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("actor")) {
            //add it to the list
            if(!allActors.containsKey(tempActor.getName().toLowerCase())){
                HashSet<Integer> birthYears = new HashSet<Integer>();
                birthYears.add(tempActor.getdob());
                allActors.put(tempActor.getName().toLowerCase(), birthYears);
                actorList.add(tempActor);
            }
            else if (!allActors.get(tempActor.getName().toLowerCase()).contains(tempActor.getdob())){
                allActors.get(tempActor.getName().toLowerCase()).add(tempActor.getdob());
                actorList.add(tempActor);
            }
            else{
                System.out.println("Actor " + tempActor.getName() + " is a duplicate actor");
                dup++;
                try {
                    inconsistency.write("Actor " + tempActor.getName() + " is a duplicate actor\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        } else if (qName.equalsIgnoreCase("dob")) {
            if(!tempVal.equals("")){
                try{
                    tempActor.setdob(Integer.parseInt(tempVal));
                }
                catch (Exception e){
                    System.out.println("Year " + tempVal + " is not an integer");
                    skip++;
                    try {
                        inconsistency.write("Year " + tempVal + " is not an integer\n");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        } else if (qName.equalsIgnoreCase("stagename")) {
            tempActor.setName(tempVal);
        }
    }

    private void insertStarsInMovies(){

        try{
            int count = 0;
            connection.setAutoCommit(false);
            String query = "CALL insertActorXML (?, ?);\n";
            PreparedStatement statement = connection.prepareStatement(query);
            for(Actor currentActor : actorList){
                statement.setString(1, currentActor.getName());
                if(currentActor.getdob() > -1){
                    statement.setInt(2, currentActor.getdob());
                }
                else{
                    statement.setString(2, null);
                }
                count++;
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            inconsistencyAggregate.write("Done with Actors. Count: " + count + "\n");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
    public static void main(String[] args) {
        ActorParser spe = new ActorParser();
        spe.runExample();
    }
}
