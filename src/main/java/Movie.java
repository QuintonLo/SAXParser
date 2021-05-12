import java.util.HashSet;

public class Movie {
    private String id;

    private String title;

    private int year;

    private String director;

    HashSet<String> Genres;

    public Movie(){
        year = -1;
        Genres = new HashSet<String>();
        id = "";
        title = "";
        director = "";
    }

    public Movie(String id, String title, int year, String director) {
        this.id = id;
        this.title = title;
        this.year  = year;
        this.director = director;

    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public HashSet<String> getGenres(){
        return Genres;
    }

    public void addGenre(String genre){
        this.Genres.add(genre);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Id: " + getId());
        sb.append(", ");
        sb.append("Title: " + getTitle());
        sb.append(", ");
        sb.append("Year: " + getYear());
        sb.append(", ");
        sb.append("Director: " + getDirector());
        sb.append(", ");
        sb.append("Genres: " + getGenres().toString());


        return sb.toString();
    }
}
