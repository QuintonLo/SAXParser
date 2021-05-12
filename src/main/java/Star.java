public class Star {
    private String id;
    private String director;
    private String name;


    public Star(){

    }

    public Star(String id, String name) {
        this.id = id;
        this.name = name;

    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDirector() {
        return director;
    }
    public void setDirector(String director) {
        this.director = director;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Id: " + getId());
        sb.append(", ");
        sb.append("Director: " + getDirector());
        sb.append(", ");
        sb.append("Name: " + getName());

        return sb.toString();
    }
}
