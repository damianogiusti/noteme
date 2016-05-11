package it.tsamstudio.noteme;

import android.media.Image;
import android.media.MediaPlayer;
import android.provider.MediaStore;

/**
 * Created by adamo on 11/05/2016.
 */
public class Note {

    //chiave degli attributi
    public final String KEY_TITLE="key_title";         //chiave titolo
    public final String KEY_COLOR="color";             //chiave colore
    public final String KEY_TAG="tag";
    public final String KEY_TEXT ="text";            //chiave testo
    public final String  KEY_IMAGE ="imgage";       //chiave eventuale immagine
    public final String KEY_AUDIO ="audio";         //chiave eventuale audio



    //attributi
    private String title;           //titolo
    private String color;           //colore
    private String tag;             //tag
    private String text;            //testo
    private String imgage;          //path eventuale immagine salvate
    private String audio;           //path eventuale audio registrato

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImgage() {
        return imgage;
    }

    public void setImgage(String imgage) {
        this.imgage = imgage;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }


}
