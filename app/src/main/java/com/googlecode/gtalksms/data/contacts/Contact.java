package com.googlecode.gtalksms.data.contacts;

import java.util.ArrayList;

public class Contact implements Comparable<Contact> {
    public final ArrayList<Long> ids = new ArrayList<Long>();
    public final ArrayList<Long> rawIds = new ArrayList<Long>();
    public String name;

    public int compareTo(Contact another) {
        return name.compareTo(another.name);
    }
}
