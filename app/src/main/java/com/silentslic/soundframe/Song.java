package com.silentslic.soundframe;

/**
 * Class for storing data about specific song,
 * such as Name, Path, Id, etc
 */

public class Song {
    private String Name;
    private String Path;
    private String Duration;
    private int Id;


    public Song(int id, String name, String path, String duration) {
        Id = id;
        if (name == null)
            Name = "[no name]";
        else
            Name = name;
        Path = path;
        Duration = duration;
    }

    public String getName() {
        return Name;
    }

    public String getPath() {
        return Path;
    }

    public String getDuration() {
        return Duration;
    }

    public int getId() {
        return Id;
    }

    @Override
    public String toString() {
        return Name;
    }
}
