package fr.cea.ig.io;


import java.io.*;
import java.nio.charset.Charset;

public abstract class Reader {
    private final static int    PAGE_SIZE           = 4_096;
    private final static int    DEFAULT_NUMBER_PAGE = 10;
    private final static String DEFAULT_CHARSET     = "US-ASCII";

    private     final   int                 mNumberPageSize;
    protected   final   String              mPath;
    protected           InputStreamReader   mIsr;
    protected           BufferedReader      mBuffer;
    

    public Reader( InputStream is, final int number_page_size, final String charset ){
        mNumberPageSize = number_page_size;
        mPath           = null;
        mIsr            = new InputStreamReader( is, Charset.forName( charset ) );
        mBuffer         = new BufferedReader( mIsr, PAGE_SIZE * mNumberPageSize );
    }

    public Reader( InputStream is, final int number_page_size ){
        this(is, number_page_size, DEFAULT_CHARSET);
    }

    public Reader( InputStream is ){
        this(is, DEFAULT_NUMBER_PAGE, DEFAULT_CHARSET);
    }



    public Reader( final String path, final int number_page_size, final String charset ){
        mNumberPageSize = number_page_size;
        mPath           = path;
        try {

            mIsr        = new InputStreamReader( new FileInputStream(path), Charset.forName( charset ) );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBuffer         = new BufferedReader( mIsr, PAGE_SIZE * mNumberPageSize );
    }

    public Reader( final String path, final int number_page_size ){
        this(path, number_page_size, DEFAULT_CHARSET);
    }

    public Reader( final String path ){
        this(path, DEFAULT_NUMBER_PAGE, DEFAULT_CHARSET);
    }
}
