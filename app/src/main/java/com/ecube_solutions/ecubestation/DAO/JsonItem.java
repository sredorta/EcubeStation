package com.ecube_solutions.ecubestation.DAO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * JsonItem class:
 *    This class is used to parse Json answers from the Cloud
 */
public class JsonItem {


        @SerializedName("message")
        private String mMessage = "Could not connect to cloud !";

        @SerializedName("result")
        private String mResult = "false";

        @SerializedName("products")
        private String mProducts = null;

        @SerializedName("action")
        private String mAction = null;

        public boolean getResult() {
            if (mResult.equals("success")) {
                mResult = "true";
            } else {
                mResult = "false";
            }
            return Boolean.parseBoolean(mResult);
        }
        public void setResult(boolean result) {
            mResult = String.valueOf(result);
        }

        public String getMessage() {
            return mMessage;
        }

        public void setMessage(String message) {
            mMessage = message;
        }

        public String getProducts() { return mProducts; }


       public String getAction() {
         return mAction;
       }

        public static JsonItem parseJSON(String response) {
            Gson gson = new GsonBuilder().create();
            JsonItem answer = gson.fromJson(response, JsonItem.class);
            return(answer);
        }

        public String encodeJSON() {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(this);
        }
}
