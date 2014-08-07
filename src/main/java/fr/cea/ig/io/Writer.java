package fr.cea.ig.io;


import java.io.*;
import java.nio.charset.Charset;

public abstract class Writer {
    private final static int    PAGE_SIZE           = 4_096;
    private final static int    DEFAULT_NUMBER_PAGE = 10;
    private final static String DEFAULT_CHARSET     = "US-ASCII";

    private     final   int                 mNumberPageSize;
    protected   final   String              mPath;
    protected           OutputStreamWriter  mOsw;
    protected           BufferedWriter      mBuffer;
    

    public Writer( OutputStream os, final int number_page_size, final String charset ){
        mNumberPageSize = number_page_size;
        mPath           = null;
        mOsw            = new OutputStreamWriter( os, Charset.forName( charset ) );
        mBuffer         = new BufferedWriter( mOsw, PAGE_SIZE * mNumberPageSize );
    }

    public Writer( OutputStream os, final int number_page_size ){
        this( os, number_page_size, DEFAULT_CHARSET);
    }

    public Writer( OutputStream os ){
        this( os, DEFAULT_NUMBER_PAGE, DEFAULT_CHARSET);
    }



    public Writer( final String path, final int number_page_size, final String charset ){
        mNumberPageSize = number_page_size;
        mPath           = path;
        try {
            mOsw        = new OutputStreamWriter( new FileOutputStream(path), Charset.forName(charset) );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBuffer         = new BufferedWriter( mOsw, PAGE_SIZE * mNumberPageSize );
    }

    public Writer( final String path, final int number_page_size ){
        this(path, number_page_size, DEFAULT_CHARSET);
    }

    public Writer( final String path ){
        this(path, DEFAULT_NUMBER_PAGE, DEFAULT_CHARSET);
    }
}
