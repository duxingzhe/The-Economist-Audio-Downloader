package com.luxuan.media;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class Metadata {

    /**
     * {@hide}
     */
    public static final int STRING_VAL=1;

    /**
     * {@hide}
     */
    public static final int INTEGER_VAL=2;

    /**
     * {@hide}
     */
    public static final int BOOLEAN_VAL=3;

    /**
     * {@hide}
     */
    public static final int LONG_VAL=4;

    /**
     * {@hide}
     */
    public static final int DOUBLE_VAL=5;

    /**
     * {@hide}
     */
    public static final int DATE_VAL=6;

    /**
     * {@hide}
     */
    public static final int BYTE_ARRAY_VAL=7;

    private HashMap<String, String> mParcel;

    /**
     * Check a parcel containing metadata is well formed. The header
     * is checked as well as the individual records format. However, the
     * data inside the record is not checked because we do lazy access
     * (we check/unmarshall only data the user asks for.)
     *
     * Format of a metadata parcel:
     <pre>
     1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                     metadata total size                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     'M'       |     'E'       |     'T'       |     'A'       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     |                .... metadata records ....                     |
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     </pre>
     *
     * @param parcel With the serialized data. Metadata keeps a
     *               reference on it to access it later on. The caller
     *               should not modify the parcel after this call (and
     *               not call recycle on it.)
     * @return false if an error occurred.
     * {@hide}
     */
    public boolean parse(HashMap<String, String> metadata){
        if(metadata==null){
            return false;
        }else{
            mParcel=metadata;
            return true;
        }
    }

    public boolean has(final String metadataId){
        if(!checkMetadataId(metadataId)){
            throw new IllegalArgumentException("invalid key: "+ metadataId);
        }
        return mParcel.containsKey(metadataId);
    }

    public HashMap<String, String> getAll(){
        return mParcel;
    }

    /**
     * {@hide}
     */
    public String getString(final String key){
        checkType(key, STRING_VAL);
        return String.valueOf(mParcel.get(key));
    }

    /**
     * {@hide}
     */
    public int getInt(final String key){
        checkType(key, INTEGER_VAL);
        return Integer.valueOf(mParcel.get(key));
    }

    public boolean getBoolean(final String key){
        checkType(key, BOOLEAN_VAL);
        return Integer.valueOf(mParcel.get(key))==1;
    }

    /**
     * {@hide}
     */
    public long getLong(final String key){
        checkType(key, LONG_VAL);
        return Long.valueOf(mParcel.get(key));
    }

    /**
     * {@hide}
     */
    public double getDouble(final String key){
        checkType(key, DOUBLE_VAL);
        return Double.valueOf(mParcel.get(key));
    }

    /**
     * {@hide}
     */
    public byte[] getByteArray(final String key){
        checkType(key, BYTE_ARRAY_VAL);
        return mParcel.get(key).getBytes();
    }

    /**
     * {@hide}
     */
    public Date getDate(final String key){
        checkType(key, DATE_VAL);
        final long timeSinceEpoch=Long.valueOf(mParcel.get(key));
        final String timeZone=mParcel.get(key);

        if(timeZone.length()==0){
            return new Date(timeSinceEpoch);
        }else{
            TimeZone tz= TimeZone.getTimeZone(timeZone);
            Calendar cal= Calendar.getInstance(tz);

            cal.setTimeInMillis(timeSinceEpoch);
            return cal.getTime();
        }
    }

    private boolean checkMetadataId(final String val){
        return true;
    }

    private void checkType(final String key, final int expectedType){
        String type=mParcel.get(key);

        if(type==null){
            throw new IllegalStateException("Wrong type "+expectedType+" but not "+type);
        }
    }
}
