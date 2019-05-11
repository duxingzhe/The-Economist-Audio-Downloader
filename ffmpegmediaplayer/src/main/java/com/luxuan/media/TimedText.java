package com.luxuan.media;

import android.os.Parcel;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class to hold the timed text's metadata.
 *
 * {@hide}
 */
public class TimedText {

    private static final int FIRST_PUBLIC_KEY=1;

    public static final int KEY_DISPLAY_FLAGS=1;
    public static final int KEY_STYLE_FLAGS=2;
    public static final int KEY_BACKGROUND_COLOR_RGBA=3;
    public static final int KEY_HIGHLIGHT_COLOR_RGBA=4;
    public static final int KEY_SCROLL_DELAY=5;
    public static final int KEY_WRAP_TEXT=6;
    public static final int KEY_START_TIME=7;
    public static final int KEY_STRUCT_BLINKING_TEXT_LIST=8;
    public static final int KEY_STRUCT_FONT_LIST=9;
    public static final int KEY_STRUCT_HIGHLIGHT_LIST=10;
    public static final int KEY_STRUCT_HYPER_TEXT_LIST=11;
    public static final int KEY_STRUCT_KARAOKE_LIST=12;
    public static final int KEY_STRUCT_STYLE_LIST=13;
    public static final int KEY_STRUCT_TEXT_POS=14;
    public static final int KEY_STRUCT_JUSTIFICATION=15;
    public static final int KEY_STRUCT_TEXT=16;

    private static final int LST_PUBLIC_KEY=16;
    private static final int FIRST_PRIVATE_KEY=101;

    private static final int KEY_GLOBAL_SETTING=101;
    private static final int KEY_LOCAL_SETTING=102;
    private static final int KEY_START_CHAR =103;
    private static final int KEY_END_CHAR =104;
    private static final int KEY_FONT_ID =105;
    private static final int KEY_FONT_SIZE=106;
    private static final int KEY_TEXT_COLOR_RGBA=107;

    private static final int LAST_PRIVATE_KEY=107;

    private static final String TAG = "TimedText";
    private Parcel mParcel= Parcel.obtain();
    private final HashMap<Integer, Object> mKeyObjectMap=new HashMap<Integer, Object>();

    private int mDisplayFlags=-1;
    private int mBackgroundColorRGBA=-1;
    private int mHightlightColorRGBA=-1;
    private int mScrollDelay=-1;
    private int mWrapText=-1;

    private List<CharPos> mBlinkingPosList=null;
    private List<CharPos> mHighlightPosList=null;
    private List<Karaoke> mKaraokeList=null;
    private List<Font> mFontList=null;
    private List<Style> mStyleList=null;
    private List<HyperText> mHyperTextList=null;

    private TextPos mTextPos;
    private Justification mJustification;
    private Text mTextStruct;

    public class Text{
        public int textLen;
        public byte[] text;
        public Text(){

        }
    }

    public class CharPos{
        public int startChar=-1;
        public int endChar=-1;
        public CharPos(){

        }
    }

    public class TextPos{
        public int top=-1;
        public int left=-1;
        public int bottom=-1;
        public int right=-1;
        public TextPos(){

        }
    }

    public class Justification{
        public int horizontalJustification=-1;
        public int verticalJustification=-1;
        public Justification(){

        }
    }

    public class Style{
        public int startChar=-1;
        public int endChar=-1;
        public int fontID=-1;
        public boolean isBold=false;
        public boolean isItalic=false;
        public boolean isUnderlined=false;
        public int fontSize=-1;
        public int colorRGBA=-1;
        public Style(){

        }
    }

    public class Font{
        public int ID=-1;
        public String name;
        public Font(){

        }
    }

    public class Karaoke{
        public int startTimeMs=-1;
        public int endTimeMs=-1;
        public int endChar=-1;
        public Karaoke(){

        }
    }

    public class HyperText{
        public int startChar=-1;
        public int endChar=-1;
        public String URL;
        public String altString;
        public HyperText(){

        }
    }

    public TimedText(byte[] obj){
        mParcel.unmarshall(obj,0,obj.length);

        if(!parseParcel()){
            mKeyObjectMap.clear();
            throw new IllegalArgumentException("parseParcel() fails");
        }
    }

    private boolean parseParcel(){
        mParcel.setDataPosition(0);
        if(mParcel.dataAvail()==0){
            return false;
        }

        int type=mParcel.readInt();
        if(type==KEY_LOCAL_SETTING){
            type=mParcel.readInt();
            if(type!=KEY_START_TIME){
                return false;
            }
            int mStartTimeMs=mParcel.readInt();
            mKeyObjectMap.put(type, mStartTimeMs);

            type=mParcel.readInt();
            if(type!=KEY_STRUCT_TEXT){
                return false;
            }

            mTextStruct=new Text();
            mTextStruct.textLen=mParcel.readInt();

            mTextStruct.text=mParcel.createByteArray();
            mKeyObjectMap.put(type, mTextStruct);
        }
        else if(type!=KEY_GLOBAL_SETTING) {
            Log.w(TAG, "Invalid timed text key found: " + type);
            return false;
        }
        while(mParcel.dataAvail()>0){
            int key=mParcel.readInt();
            if(!isValidKey(key)){
                Log.w(TAG,"INvalid timed key found:"+key);
                return false;
            }

            Object object=null;

            switch(key){
                case KEY_STRUCT_STYLE_LIST:
                    readStyle();
                    object=mStyleList;
                    break;
                case KEY_STRUCT_FONT_LIST:
                    readFont();
                    object=mFontList;
                    break;
                case KEY_STRUCT_HIGHLIGHT_LIST:
                    readHighlight();
                    object=mHighlightPosList;
                    break;
                case KEY_STRUCT_KARAOKE_LIST:
                    readKaraoke();
                    object=mKaraokeList;
                    break;
                case KEY_STRUCT_HYPER_TEXT_LIST:
                    readHyperText();
                    object=mHyperTextList;
                    break;
                case KEY_STRUCT_BLINKING_TEXT_LIST:
                    readBlinkingText();
                    object=mBlinkingPosList;
                    break;
                case KEY_WRAP_TEXT:
                    mWrapText=mParcel.readInt();
                    object=mWrapText;
                    break;
                case KEY_HIGHLIGHT_COLOR_RGBA:
                    mHightlightColorRGBA=mParcel.readInt();
                    object=mHighlightPosList;
                    break;
                case KEY_DISPLAY_FLAGS:
                    mDisplayFlags=mParcel.readInt();
                    object=mDisplayFlags;
                    break;
                case KEY_STRUCT_JUSTIFICATION:
                    mJustification=new Justification();

                    mJustification.horizontalJustification=mParcel.readInt();
                    mJustification.verticalJustification=mParcel.readInt();

                    object=mJustification;
                    break;
                case KEY_BACKGROUND_COLOR_RGBA:
                    mBackgroundColorRGBA=mParcel.readInt();
                    object=mBackgroundColorRGBA;
                    break;
                case KEY_STRUCT_TEXT_POS:
                    mTextPos=new TextPos();

                    mTextPos.top=mParcel.readInt();
                    mTextPos.left=mParcel.readInt();
                    mTextPos.bottom=mParcel.readInt();
                    mTextPos.right=mParcel.readInt();

                    object=mTextPos;
                    break;
                case KEY_SCROLL_DELAY:
                    mScrollDelay=mParcel.readInt();
                    object=mScrollDelay;
                    break;
                default:
                    break;
            }

            if(object!=null){
                if(mKeyObjectMap.containsKey(key)){
                    mKeyObjectMap.remove(key);
                }
                mKeyObjectMap.put(key, object);
            }
        }

        mParcel.recycle();
        return true;
    }

    private void readStyle(){
        Style style=new Style();
        boolean endOfStyle=false;

        while(!endOfStyle&&(mParcel.dataAvail()>0)){
            int key=mParcel.readInt();
            switch(key){
                case KEY_START_CHAR:
                    style.startChar=mParcel.readInt();
                    break;
                case KEY_END_CHAR:
                    style.endChar=mParcel.readInt();
                    break;
                case KEY_FONT_ID:
                    style.fontID=mParcel.readInt();
                    break;
                case KEY_STYLE_FLAGS:
                    int flags=mParcel.readInt();

                    style.isBold=((flags%2)==1);
                    style.isItalic=((flags%4)>=2);
                    style.isUnderlined=((flags/4)==1);
                    break;
                case KEY_FONT_SIZE:
                    style.fontSize=mParcel.readInt();
                    break;
                case KEY_TEXT_COLOR_RGBA:
                    style.colorRGBA=mParcel.readInt();
                    break;
                default:
                    mParcel.setDataPosition(mParcel.dataPosition());
                    endOfStyle=true;
                    break;
            }

            if(mStyleList==null){
                mStyleList=new ArrayList<Style>();
            }

            mStyleList.add(style);
        }

    }

    private void readFont(){
        int entryCount=mParcel.readInt();

        for(int i=0;i<entryCount;i++){
            Font font=new Font();
            font.ID=mParcel.readInt();
            int nameLen=mParcel.readInt();

            byte[] text=mParcel.createByteArray();
            font.name=new String(text, 0, nameLen);

            if(mFontList==null){
                mFontList=new ArrayList<Font>();
            }

            mFontList.add(font);
        }
    }

}
