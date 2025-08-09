package com.googlecode.gtalksms.data.phone;

import android.provider.ContactsContract.CommonDataKinds;

public class Phone {
    private final static String cellPhonePattern = "\\+*\\d+";
    
    private String mContactName;
    private final String mNumber;
    private final String mCleanNumber;
    private String mLabel;
    private final int    mType;
    private boolean mIsCellPhoneNumber;
    private boolean mIsDefaultNumber;

    /**
     * 
     * @param contactName
     * @param number
     */
    public Phone(String contactName, String number) {
        this.mContactName = contactName;
        this.mNumber = number;
        this.mCleanNumber = cleanPhoneNumber(number);
        this.mIsCellPhoneNumber = true;
        this.mType = CommonDataKinds.Phone.TYPE_MOBILE;
    }
    
    /**
     * 
     * @param number
     * @param label
     * @param type
     * @param super_primary
     */
    public Phone(String number, String label, int type, int super_primary) {
        this.mNumber = number;
        this.mCleanNumber = cleanPhoneNumber(number);
        this.mLabel = label;
        this.mType = type;
        mIsDefaultNumber = super_primary > 0;
    }
    
    public Boolean phoneMatch(String phone) {
        phone = cleanPhoneNumber(phone);
        if (mCleanNumber.equals(phone)) {
            return true;
        }
        else if (mCleanNumber.length() != phone.length()) {
            if (mCleanNumber.length() > phone.length() && mCleanNumber.startsWith("+")) {
                return mCleanNumber.replaceFirst("\\+\\d\\d", "0").equals(phone);
            }
            else if (phone.length() > mCleanNumber.length() && phone.startsWith("+")) {
                return phone.replaceFirst("\\+\\d\\d", "0").equals(mCleanNumber);
            }
        }
        return false;
    } 
    
    public static boolean isCellPhoneNumber(String number) {
        return Phone.cleanPhoneNumber(number).matches(cellPhonePattern);
    }
    
    public static String cleanPhoneNumber(String number) {
        return number.replace("(", "")
                     .replace(")", "")
                     .replace("-", "")
                     .replace(".", "")
                     .replace(" ", "");
    }

    public String getContactName() {
        return mContactName;
    }

    public String getNumber() {
        return mNumber;
    }
    
    public String getCleanNumber() {
        return mCleanNumber;
    }

    public String getLabel() {
        return mLabel;
    }

    public int getType() {
        return mType;
    }

    public boolean isCellPhoneNumber() {
        return mIsCellPhoneNumber;
    }

    public boolean isDefaultNumber() {
        return mIsDefaultNumber;
    }

    public void setContactName(String name) {
        this.mContactName = name;
    }
}