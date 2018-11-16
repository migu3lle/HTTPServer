package Lection06;

/*
Rechnernetze 6.2 Java Sockets: HTTP-Server (multi-thread)
Michael Gundacker 1646765
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class LoggingThread extends Thread {

    public void run(){
        //Create Logfile if not exists
        if(!Files.exists(Paths.get("logfile.txt"))){
            try {
                Files.createFile(Paths.get("logfile.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Write to file all 5 seconds
        while(true){
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (HttpServerMain.sharedLogString){
                writeLogToFile(HttpServerMain.sharedLogString);
                HttpServerMain.sharedLogString = "";
            }
        }
    }

    /**
     * @param logString
     * Appends logString to LogFile
     */
    private void writeLogToFile(String logString){
        try {
            Files.write(Paths.get("logfile.txt"), logString.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
