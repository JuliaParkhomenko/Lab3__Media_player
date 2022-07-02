package ua.nure.parkhomenko.lab3;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String path;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Song(long id, String title, String artist, String path) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path=path;
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

}
