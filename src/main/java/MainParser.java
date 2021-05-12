
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

public class MainParser extends DefaultHandler {
    HashSet<String> Genres = new HashSet<String>();
    HashMap<String, Integer> genreIds = new HashMap<String, Integer>();
    HashSet<Movie> movieList;
    private HashMap<String, HashSet<String>> allDirectors = new HashMap<String, HashSet<String>>();
    HashSet<String> currGenres = new HashSet<String>();
    private String tempVal;
    int dup = 0;
    int skip = 0;
    //to maintain context
    private Movie tempMovie;
    FileWriter inconsistency;
    FileWriter inconsistencyAggregate;
    {
        try {
            inconsistency = new FileWriter("inconsistency.txt");
            inconsistencyAggregate = new FileWriter("inconsistencyAggregate.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    String loginUser = "root";
    String loginPasswd = "Z0ccermysql";
    String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
    Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

    public MainParser() throws SQLException {
        movieList = new HashSet<Movie>();
    }

    public void runExample() {
        completeDirectors();
        parseDocument();
        insertGenres();
        insertMovies();
        getGenres();
        writeData();
        try {
            inconsistencyAggregate.write("Number of items skipped: " + skip + "\n");
            inconsistencyAggregate.write("Number of duplicate items: " + dup + "\n\n");
            inconsistency.close();
            inconsistencyAggregate.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getGenres(){
        String query = "SELECT * FROM genres;";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet results = statement.executeQuery();
            while (results.next()){
                String name = results.getString("name").toLowerCase();
                int id = results.getInt("id");
                genreIds.put(name, id);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public HashMap<String, HashSet<String>> getAllDirectors(){
        return allDirectors;
    }

    private void completeDirectors() {
        String query = "SELECT title, director FROM movies";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet results = statement.executeQuery();
            while(results.next()){
                String dir = results.getString("director").toLowerCase();
                String title = results.getString("title").toLowerCase();
                if(allDirectors.containsKey(dir)){
                    allDirectors.get(dir).add(title.toLowerCase());
                }
                else{
                    HashSet<String> temp = new HashSet<String>();
                    temp.add(title.toLowerCase());
                    allDirectors.put(dir, temp);
                }
            }
            query = "SELECT name FROM genres;";
            statement = connection.prepareStatement(query);
            results = statement.executeQuery();
            while(results.next()){
                String genre = results.getString("name");
                currGenres.add(genre.toLowerCase());
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
            sp.parse("mains243.xml", this);

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
        System.out.println("Number of movies = " + movieList.size());

        for(Movie item : movieList){
            System.out.println(item.toString());
        }

    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        if (qName.equalsIgnoreCase("film")){
            tempMovie = new Movie();
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
        if (qName.equalsIgnoreCase("film")) {
            //add it to the list
            if(!tempMovie.getDirector().equals("") && tempMovie.getYear() != -1 && !tempMovie.getTitle().equals("")) {

                if(allDirectors.containsKey(tempMovie.getDirector().toLowerCase())){
                    HashSet<String> curr = allDirectors.get(tempMovie.getDirector().toLowerCase());
                    if(!curr.contains(tempMovie.getTitle().toLowerCase())) {
                        allDirectors.get(tempMovie.getDirector().toLowerCase()).add(tempMovie.getTitle().toLowerCase());
                        movieList.add(tempMovie);
                    }
                    else{
                        System.out.println("Title " + tempMovie.getTitle() + " is a duplicate movie");
                        dup++;
                        try {
                            inconsistency.write("Title " + tempMovie.getTitle() + " is a duplicate movie\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    HashSet<String> temp = new HashSet<String>();
                    temp.add(tempMovie.getTitle().toLowerCase());
                    allDirectors.put(tempMovie.getDirector().toLowerCase(), temp);
                    movieList.add(tempMovie);
                }

            }
            else{
                System.out.println("Title " + tempMovie.getTitle() + " has invalid data");
                skip++;
                try {
                    inconsistency.write("Title " + tempMovie.getTitle() + " has invalid data\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if (qName.equalsIgnoreCase("t")) {

            tempMovie.setTitle(tempVal);
        } else if (qName.equalsIgnoreCase("year")) {
            try{
                tempMovie.setYear(Integer.parseInt(tempVal));
            }
            catch(Exception e){
                System.out.println("year " + tempVal + " is in an invalid format");
                skip++;
                try {
                    inconsistency.write("year " + tempVal + " is in an invalid format\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        } else if (qName.equalsIgnoreCase("dir")) {
            tempMovie.setDirector(tempVal);
        } else if (qName.equalsIgnoreCase("cats")){
            String[] genres = tempVal.split("[^a-zA-Z]");
            for (String item : genres){
                if(!item.equals("")){
                    if(!currGenres.contains(item.toLowerCase())){
                        Genres.add(item.toLowerCase());
                        currGenres.add(item.toLowerCase());
                    }
                    else{
                        dup++;
                    }
                    tempMovie.addGenre(item.toLowerCase());
                }

            }

        }
    }

    public void insertGenres(){
        String query = "CALL insertGenreXML (?);\n";
        try {
            int count = 0;
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(query);
            for (String item : Genres) {
                statement.setString(1, item);
                statement.addBatch();
                count++;
            }
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            try {
                inconsistencyAggregate.write("Done with Genres. Count: " + count + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    private void insertMovies() {
        String query = "CALL insertMovieXML (?, ?, ?);\n";
        try {
            int counter = 0;
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(query);
            for (Movie item: movieList) {

                statement.setString(1, item.getTitle());
                statement.setInt(2, item.getYear());
                statement.setString(3, item.getDirector());
                statement.addBatch();
                counter++;
            }
            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            try {
                inconsistencyAggregate.write("Done with Movies. Count: " + counter + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void writeData(){
        try{
            String query = "CALL linkGenreMovieXML (?,?,?, ?);\n";
            try {
                int count = 0;
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(query);
                for(Movie currentMovie : movieList){
                    for(String genre : currentMovie.getGenres()) {
                        statement.setString(1, currentMovie.getTitle());
                        statement.setInt(2, genreIds.get(genre));
                        statement.setString(3, currentMovie.getDirector());
                        statement.setInt(4, currentMovie.getYear());
                        statement.addBatch();
                        count++;
                    }
                }
                statement.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
                inconsistencyAggregate.write("Done with Genres in Movies. Count: " + count + "\n");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        catch(Exception e){

        }
    }

    public static void main(String[] args) {
        try {
            long startTime = System.nanoTime();
            MainParser main = new MainParser();
            main.runExample();
            HashMap<String, HashSet<String>> dir = main.getAllDirectors();
            ActorParser actor = new ActorParser();
            actor.runExample();
            StarsParser star = new StarsParser(dir);
            star.runExample();
            long endTime = System.nanoTime();
            long seconds = (endTime-startTime)/1000000000;
            System.out.println("Process took " + seconds + " seconds");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

}
