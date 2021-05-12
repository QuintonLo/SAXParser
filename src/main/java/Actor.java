public class Actor {
    private String id;
    private int birthYear;
    private String name;


    public Actor(){
        birthYear = -1;
    }

    public Actor(String name, int birthYear) {
        this.birthYear = birthYear;
        this.name = name;

    }

    public int getdob() {
        return birthYear;
    }
    public void setdob(int birthYear) {
        this.birthYear = birthYear;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() { return id;}
    public void setId(String id) {this.id = id;}


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ID: " + getId());
        sb.append(", ");
        sb.append("Name: " + getName());
        sb.append(", ");
        sb.append("dob: " + getdob());

        return sb.toString();
    }
}
