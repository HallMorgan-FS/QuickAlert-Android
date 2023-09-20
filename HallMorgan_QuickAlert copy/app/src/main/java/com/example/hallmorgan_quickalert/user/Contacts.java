package com.example.hallmorgan_quickalert.user;


import java.io.Serializable;

public class Contacts implements Serializable {

    private final String name;
    private final String number;
    private boolean isSelected;
    private String uniqueID;

    public Contacts(String _name, String _number){
        name = _name;
        number = _number;
    }

    public String getName(){return name;}
    public String getNumber(){return number;}
    public void setSelected(boolean show) {
        isSelected = show;}
    public boolean isSelected(){return isSelected;}

    public void setID(String id) {
        this.uniqueID = id;
    }
    public String getId(){
        return uniqueID;
    }
}
