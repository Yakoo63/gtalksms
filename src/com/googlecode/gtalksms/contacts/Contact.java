package com.googlecode.gtalksms.contacts;

public class Contact implements Comparable<Contact> {
    public Long id;
    public String name;

    @Override
    public int compareTo(Contact another) {
        return name.compareTo(another.name);
    }
}
