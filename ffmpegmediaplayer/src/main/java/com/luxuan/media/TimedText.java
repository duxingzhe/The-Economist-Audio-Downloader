package com.luxuan.media;

import android.os.Parcel;

import java.util.HashMap;

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

    private Parcel mParcel= Parcel.obtain();
    private final HashMap<Integer, Object> mKeyObjectMap=new HashMap<Integer, Object>();

    private int mDisplayFlags=-1;
    private int mBackgroundColorRGBA=-1;
    private int mHightlightColorRGBA=-1;
    private int mScrollDelay=-1;
    private int mWrapText=-1;

    private List<CharPos> mBlinkingPosList=null;
    private List<CharPos> mHighListPosList=null;
    private List<Karaoke> mKaraokeList=null;
    private List<Font> mFontList=null;
    private List<Style> mStyleList=null;
    private List<HyperText> mHyperTextList=null;

    private TExtPos mTextPos;
    private Justification mJustification;
    private TExt mTextStruct;
}
