package com.example.hallmorgan_quickalert.user;


import java.io.Serializable;

public class Contacts implements Serializable {

    private final String name;
    private final String number;
    private boolean showDeleteButton;

    public Contacts(String _name, String _number){
        name = _name;
        number = _number;
    }

    public String getName(){return name;}
    public String getNumber(){return number;}
    public void setShowDeleteButton(boolean show) {showDeleteButton = show;}
    public boolean isShowDeleteButton(){return showDeleteButton;}

}
