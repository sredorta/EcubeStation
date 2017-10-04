package com.ecube_solutions.ecubestation.DAO;

import android.util.Log;

import com.ecube_solutions.ecubestation.Singleton.Locker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by sredorta on 12/1/2016.
 */
/* USAGE EXAMPLE
        GPIO.setDebugMode(true);
        Boolean isEnabled = GPIO.isSuperUserAvailable();
        if (isEnabled) {
        Toast.makeText(getApplicationContext(),"SuperUser on",Toast.LENGTH_SHORT).show();
        } else {
        Toast.makeText(getApplicationContext(),"SuperUser off",Toast.LENGTH_SHORT).show();
        }

        MyLed = new GPIO(36);


        MyLed.activationPin();
        MyLed.setInOut("out");
        MyLed.setState(1);
*/

public class GPIO {
    private String port;
    private int pin;
    private static String TAG_SHORT = "GPIO";
    private String TAG;
    private static Boolean DEBUG_MODE = true;
    private static final String NO_EXCEPTION = "no_exception";
    private Locker mLocker = Locker.getLocker();

    //Sets debug mode
    public static void setDebugMode(Boolean mode) {
        DEBUG_MODE = mode;
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Debug mode enabled !");
    }

    //Constructor
    public GPIO(int pin){
        //Add dragonboard offset
        int pin2 = pin + 902;
        this.port = "gpio"+pin2;
        this.pin = pin2;
        //Add pin name in the TAG of debugging
        TAG = TAG_SHORT + "::" + pin + "::";
        if (DEBUG_MODE) Log.i(TAG,"Created GPIO: " + pin);
        if (DEBUG_MODE) Log.i(TAG,"Created GPIO file: " + this.port);
    }

    public static ArrayList<GPIO> getArray() {
        ArrayList<GPIO> myList = new ArrayList<>();
        myList.add(new GPIO(2));   //[0] : GPIO-2    Pin:3
        myList.add(new GPIO(0));   //[1] : GPIO-0    Pin:5
        myList.add(new GPIO(1));   //[2] : GPIO-1    Pin:7
        myList.add(new GPIO(3));   //[3] : GPIO-3    Pin:9
        myList.add(new GPIO(4));   //[4] : GPIO-4    Pin:11
        myList.add(new GPIO(5));   //[5] : GPIO-5    Pin:13
        myList.add(new GPIO(7));   //[6] : GPIO-7    Pin:15
        myList.add(new GPIO(9));   //[7] : GPIO-6    Pin:17
        myList.add(new GPIO(23));  //[8] : GPIO-23   Pin:19
        myList.add(new GPIO(22));  //[9] : GPIO-22   Pin:21
        myList.add(new GPIO(36));  //[10] : GPIO-36  Pin:23
        myList.add(new GPIO(13));  //[11] : GPIO-13  Pin:25
        myList.add(new GPIO(115)); //[12] : GPIO-115 Pin:27
        myList.add(new GPIO(24));  //[13] : GPIO-24  Pin:29
        myList.add(new GPIO(35));  //[14] : GPIO-35  Pin:31
        myList.add(new GPIO(28));  //[15] : GPIO-28  Pin:33


        return myList;
    }

    //This function checks if SuperUser is available
    public static boolean isSuperUserAvailable() {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running isSuperUserAvailable");
        Result result = new Result();
        Boolean firstResult = false;
        //Check that we can export without having an issue
        result = runLinuxSUCommand("echo 938 > /sys/class/gpio/export");
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::success:: " + result.success.toString());
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stdout:: " + result.stdout);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stderr:: " + result.stderr);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::except:: " + result.exception);
        if ( result.exception.equals(NO_EXCEPTION)) {
            firstResult= true;
        }
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable:: firstResult = " + firstResult);
        // Now check that the exported file exists
        result = runLinuxSUCommand("ls -l /sys/class/gpio/gpio938");
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::success:: " + result.success);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stdout:: " + result.stdout);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stderr:: " + result.stderr);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::except:: " + result.exception);
        if ( result.stdout.length()>10 && firstResult) {
            return true;
        }
        return false;
    }

    // Runs SU shell (root access is required)
    private static Result runLinuxSUCommand(String command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxSUCommand: " + command);
        String[] commandFinal = {"su", "-c", command};
        return runLinuxCommand(commandFinal);
    }
    // Runs SH shell (no root access is required)
    private static Result runLinuxSHCommand(String command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxSHCommand: " + command);
        String[] commandFinal = {"sh", "-c", command};
        return runLinuxCommand(commandFinal);
    }

    // Runs SH shell (no root access is required)
    private static Result runLinuxCommand(String[] command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxCommand: ");
        Result result = new Result();
        String output = "";
        String outputErr = "";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output = output.concat(line + "\n");
                if (DEBUG_MODE) Log.i(TAG_SHORT, "shell:>" + line);
            }
            reader.close();
            BufferedReader readerErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            line = "";
            while ((line = readerErr.readLine()) != null) {
                outputErr = outputErr.concat(line + "\n");
                if (DEBUG_MODE) Log.i(TAG_SHORT, "error:>" + line + "\n");
            }
            readerErr.close();
            process.waitFor();
            result.success =true;
            result.stdout = output;
            result.stderr = outputErr;
            result.exception = NO_EXCEPTION;
            if (outputErr.length() > 0 ) result.success = false;
        } catch (IOException e) {
            result.exception = e.toString();
            if (DEBUG_MODE) Log.i(TAG_SHORT, "runLinuxSUCommand got exception:" + e);
        } catch (InterruptedException e) {
            result.exception =e.toString();
            if (DEBUG_MODE) Log.i(TAG_SHORT, "runLinuxSUCommand got exception:" + e);
        }
        return result;
    }


    //set value of the output
    public boolean setState(int value) {
        String command = String.format("echo %d > /sys/class/gpio/%s/value\n", value,this.port);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    // set direction
    public boolean setInOut(String direction){
        Result result = new Result();
        String command = String.format("echo %s > /sys/class/gpio/%s/direction\n", direction,this.port);
        result = runLinuxSUCommand(command);
        return result.success;
    }

    //export gpio
    public boolean activationPin(){
        String command = String.format("echo %d > /sys/class/gpio/export\n", this.pin);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    // unexport gpio
    public boolean desactivationPin(){
        String command = String.format("echo %d > /sys/class/gpio/unexport", this.pin);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    //get direction of gpio
    public String getInOut() {
        String command = String.format("cat /sys/class/gpio/%s/direction", this.port);
        Result result = runLinuxSUCommand(command);
        if (result.exception.equals(NO_EXCEPTION)) {
            return result.stdout;
        } else {
            return "";
        }

    }

    // get state of gpio for input and output
    //test if gpio is configurate
    public int getState()
    {
        String command = String.format("cat /sys/class/gpio/%s/value",this.port);
        Result result = runLinuxSUCommand(command);
        if (result.exception.equals(NO_EXCEPTION)) {
            if (result.stdout.equals("")) {
                return -1;
            } else {
                return Integer.parseInt(result.stdout.substring(0, 1));
            }
        }
        return -1;
    }



    //init the pin
    public int initPin(String direction){
        int retour=0;
        boolean ret=true;

        // see if gpio is already set
        retour=getState();
        if (retour==-1) {
            // unexport the gpio
            ret=desactivationPin();
            if(ret==false){ retour=-1; }

            //export the gpio
            ret=activationPin();
            if(ret==false){ retour=-2; }
        }

        // get If gpio direction is define
        String ret2 = getInOut();
        if (!ret2.contains(direction))
        {
            // set the direction (in or out)
            ret=setInOut(direction);
            if(ret==false){ retour=-3; }
        }
        return retour;
    }

    private static class Result {
        Boolean success = false;
        String  stdout = "";
        String  stderr = "";
        String  exception = NO_EXCEPTION;
    }
}