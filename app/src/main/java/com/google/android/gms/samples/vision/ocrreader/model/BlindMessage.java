package com.google.android.gms.samples.vision.ocrreader.model;


public class BlindMessage {
    private String uid;
    private String author;
    private String mText;

    public BlindMessage() {
    }

    public BlindMessage(String uid, String author, String mText) {
        this.uid = uid;
        this.author = author;
        this.mText = mText;
    }

    public String getUid() {
        return uid;
    }

    public String getAuthor() {
        return author;
    }

    public String getmText() {
        return mText;
    }
}
