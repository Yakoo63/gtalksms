package com.googlecode.gtalksms.data.contacts;

import java.util.List;

public class ResolvedContact {
    private String mHumanReadableName;
    private String mCanonicalPhoneNumber;
    
    private ResolvedContact[] mPossibleCandidates;
        
    ResolvedContact(String name, String number) {
        mHumanReadableName = name;
        mCanonicalPhoneNumber = number;        
    }
    
    ResolvedContact(List<ResolvedContact> candidates) {
        mPossibleCandidates = new ResolvedContact[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            mPossibleCandidates[i] = candidates.get(i);
        }
    }
    
    public String getName() {        
        return mHumanReadableName;        
    }
    
    public String getNumber() {
        return mCanonicalPhoneNumber;
    }
    
    public ResolvedContact[] getCandidates() {
        return mPossibleCandidates;        
    }
    
    public boolean isDistinct() {
        return (mPossibleCandidates == null);
    }
}
