package fr.chavanet.EyesHaveEars;

import android.view.View;

public class ConsignesModel {
    private String date;
    private String name;
    private String status;
    private String text;
    private int color;
    private int progressBarVisible;
    private int okIconVisible;
    private int nokIconVisible;
    public static final int None = 0;
    public static final int inProgress = 1;
    public static final int ok = 2;
    public static final int nok = 3;

    public ConsignesModel(String date, String name, String status, String text, int color,int flag){
        this.date = date;
        this.name = name;
        this.status = status;
        this.text = text;
        this.color = color;
        switch (flag) {
            case 0:
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 1:
                this.progressBarVisible = View.VISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 2:
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.VISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 3:
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.VISIBLE;
                break;
        }


    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setText(String text) {
        this.text = text;
    }
    public String getText() {
        return text;
    }

    public void setColor(int color) {
        this.color = color;
    }
    public int getColor() {
        return color;
    }

    public int getProgresBar () {return this.progressBarVisible; }
    public void setProgresBar (int visible) {this.progressBarVisible = visible; }

    public int getOkIcon () {return this.okIconVisible; }
    public void setOkIcon (int visible) {this.okIconVisible = visible; }

    public int getNokIcon () {return this.nokIconVisible;}
    public void setNokIcon (int visible) {this.nokIconVisible = visible; }

    public void setIcons(int flag) {
        switch (flag) {
            case 0:  // All clear
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 1:  // in progress
                this.progressBarVisible = View.VISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 2: // Ok
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.VISIBLE;
                this.nokIconVisible = View.INVISIBLE;
                break;
            case 3: // Nok
                this.progressBarVisible = View.INVISIBLE;
                this.okIconVisible = View.INVISIBLE;
                this.nokIconVisible = View.VISIBLE;
                break;
        }

    }

}