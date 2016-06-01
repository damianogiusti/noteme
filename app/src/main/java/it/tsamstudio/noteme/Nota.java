package it.tsamstudio.noteme;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.UUID;

class MalformedHexColorException extends RuntimeException {
    public MalformedHexColorException() {
        super("il colore specificato non è in formato hex valido");
    }
}

/**
 * Created by adamo on 11/05/2016.
 */
public class Nota implements Parcelable {

    private static final String TAG = "Nota";

    //attributi
    private String id;
    private String title;           //titolo
    private int color;           //colore
    private String tag;             //tag
    private String text;            //testo
    private String image;           //path eventuale immagine salvate
    private String audio;           //path eventuale audio registrato
    private Date creationDate;      // data creazione (la rappresentazione long della data sarà l'ID)
    private Date lastModifiedDate;  // data utima modifica
    private Date expireDate;        // data di scadenza della nota, dopo la quale sarà eliminata

    public Nota() {
        id = UUID.randomUUID().toString();
        color = 0;
    }

    public Nota(String uuid) {
        id = uuid;
        color = 0;
    }

    public String getID() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String imgage) {
        this.image = imgage;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Nota))
            return false;
        Nota nota = (Nota)o;
        return getID().equals(nota.getID());
    }

    // parte per la parcellizzazione

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeInt(color);
        dest.writeString(tag);
        dest.writeString(text);
        dest.writeString(image);
        dest.writeString(audio);
        dest.writeLong(creationDate.getTime());
        dest.writeLong(lastModifiedDate.getTime());
        dest.writeLong((expireDate != null) ? expireDate.getTime() : 0);
    }

    public static Parcelable.Creator<Nota> CREATOR = new ClassLoaderCreator<Nota>() {
        @Override
        public Nota createFromParcel(Parcel source, ClassLoader loader) {
            return new Nota(source);
        }

        @Override
        public Nota createFromParcel(Parcel source) {
            return new Nota(source);
        }

        @Override
        public Nota[] newArray(int size) {
            return new Nota[size];
        }
    };

    private Nota(Parcel in) {
        id = in.readString();
        title = in.readString();
        color = in.readInt();
        tag = in.readString();
        text = in.readString();
        image = in.readString();
        audio = in.readString();
        creationDate = new Date(in.readLong());
        lastModifiedDate = new Date(in.readLong());
        long longDate = in.readLong();
        expireDate = (longDate == 0) ? null : new Date(longDate);
    }
}
