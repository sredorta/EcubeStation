package com.ecube_solutions.ecubestation.DAO;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sredorta on 1/9/2017.
 */
public class ImageItem {
    private static Boolean DEBUG_MODE = false;
    private static final String TAG = "ImageItem::";
    private String mExtension;                          //Contains the extension of the file transferred
    private String mPath;                               //Local path of the downloaded file
    private Bitmap mBitmap;                             //Stores the bitmap of the image once reloaded from disk

    //Map JSON strings to our variables
    @SerializedName("product_id")
    private String mId;

    @SerializedName("name")
    private String mName;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("picture")
    private String mStream;

    @SerializedName("price")
    private String mPrice;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getPrice() {
        return mPrice + " \u20ac";
    }

    //Static method (only one per class) that parses JSon string to object
    public static ImageItem parseJSON(String response) {
        Gson gson = new GsonBuilder().create();
        ImageItem itemResponse = gson.fromJson(response, ImageItem.class);
        //JSONArray
        return itemResponse;
    }

    //Returns the base64 decoded stream
    public byte[] getStream() {return Base64.decode(this.parseBase64(),Base64.DEFAULT);}

    // Parses the mStream and returns the stream without header
    //     it extracts all info from the header
    private String parseBase64() {
        String streamHeaderFree;
        String streamHeaderOnly;
        String streamExtension;
        String pattern;
        Pattern r;
        Matcher m;

        //Remove the header first
        pattern = "^.*base64,";
        r = Pattern.compile(pattern);
        m = r.matcher(mStream);
        streamHeaderFree = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Stream without header : " + streamHeaderFree);
        //Get the header
        pattern = ";base64,.*$";
        r = Pattern.compile(pattern);
        m = r.matcher(mStream);
        streamHeaderOnly = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Header : " + streamHeaderOnly);

        //Get the file extension and save it into the object
        pattern = "^.*/";
        r = Pattern.compile(pattern);
        m = r.matcher(streamHeaderOnly);
        streamExtension = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Extension : " + streamExtension);

        this.mExtension = streamExtension;
        //Returns now the stream without header
        return streamHeaderFree;
    }

}
