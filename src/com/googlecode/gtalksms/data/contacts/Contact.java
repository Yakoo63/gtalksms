package com.googlecode.gtalksms.data.contacts;

import java.util.ArrayList;

public class Contact implements Comparable<Contact> {
    public ArrayList<Long> ids = new ArrayList<Long>();
    public ArrayList<Long> rawIds = new ArrayList<Long>();
    public String name;

    @Override
    public int compareTo(Contact another) {
        return name.compareTo(another.name);
    }
}
