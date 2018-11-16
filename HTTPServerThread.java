package Lection06;

/*
Rechnernetze 6.2 Java Sockets: HTTP-Server (multi-thread)
Michael Gundacker 1646765
 */

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;

public class HTTPServerThread extends Thread {

    private Socket socket;
    private Path documentRoot;
    private BufferedReader bReader;
    private DataOutputStream dataWriter;
    private boolean connected = true;
    private boolean logging;

    public HTTPServerThread(Socket socket, String documentRoot, boolean logging) {
        this.socket = socket;
        this.documentRoot = Paths.get(documentRoot);
        this.logging = logging;
    }

    /**
     * Thread run() method with endless loop
     */
    public void run() {
        System.out.println("Client " + socket.getInetAddress() + " on port " + socket.getPort() + " connected.");
        initialize();
        //Doing the request/response mechanism
        do {
            readCommand();
        } while (connected);    //do until connection is closed
    }

    /**
     * Initializes some Writer and Reader objects
     */
    private void initialize() {
        try {
            //BufferedReader for reading the incoming command
            bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //DataOutputStream for sending binary files (Byte[])
            dataWriter = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processing source path and check whether a file or a directory is requested
     * Then request the respective file via getFile()
     */
    private void GET(String[] commandArr) {
        //Local working directory
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        //Relative file path from incoming command
        Path relativeFilePath = Paths.get(commandArr[1]);
        //File name from incoming command - might be null if folder is requested
        Path fileName = relativeFilePath.getFileName();
        //Finally absolute file path will be built
        Path absoluteFilePath;
        //Status code to be returned from getFile()
        int statusCode;

        //Since it is no longer just one Thread, several connections will be accepted
        //even if one is blocked
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("File name: " + fileName);

        //Folder access
        if (fileName == null || fileName.toString().indexOf('.') == -1) {
            fileName = Paths.get("index.html");
            absoluteFilePath = Paths.get(workingDir.toString(), "src", documentRoot.toString(), relativeFilePath.toString(), fileName.toString());
            System.out.println("1 Absolute File Path: " + absoluteFilePath.toString());
            //Get index.html and send
            send(getFile(absoluteFilePath, relativeFilePath));
        }
        //File access
        else {
            absoluteFilePath = Paths.get(workingDir.toString(), "src", documentRoot.toString(), relativeFilePath.toString());
            System.out.println("2 Absolute File Path: " + absoluteFilePath.toString());
            send(getFile(absoluteFilePath, relativeFilePath));
        }
    }


    /**
     * @param absoluteFilePath  to the requested file (at this point a directory was already converted to /index.html)
     * @param realativeFilePath just for logging reasons (logging relative Path only)
     * @return - byte[] array containing the requested file
     * - null if the file was not found
     */
    private byte[] getFile(Path absoluteFilePath, Path realativeFilePath) {

        File file = absoluteFilePath.toFile();
        byte[] byteArray = new byte[(int) file.length()];
        BufferedInputStream bInputStream;

        try {
            //Read from file with BufferedInputStream and read into Byte Array to return
            log("GET " + realativeFilePath.toString());
            bInputStream = new BufferedInputStream(new FileInputStream(file));
            bInputStream.read(byteArray, 0, byteArray.length);
            return byteArray;
        } catch (FileNotFoundException e) {
            //In case file does not exist close the connection and return null
            System.out.println("Requested file does not exist on server.");
            closeConnection();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reading HTTP request and check for syntax
     */
    private void readCommand() {
        String command = null;
        String[] commandArr;
        String commandMethod;
        try {
            command = bReader.readLine();
            System.out.println("Received command: " + command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        commandArr = command.split(" ");

        commandMethod = commandArr[0];
        //Only GET is supported, otherwise close connection
        if (commandMethod.equalsIgnoreCase("GET")) {
            GET(commandArr);
        }
        //TODO handling for unknown command
        else {
            closeConnection();
        }
    }

    /**
     * @param fileToSend - file to be sent to client
     */
    private void send(byte[] fileToSend) {
        if (fileToSend != null) {
            System.out.println("Sending file with length of " + fileToSend.length);
            try {
                dataWriter.write(fileToSend, 0, fileToSend.length);
                dataWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }
    }

    /**
     * Method to close Readers, Writers and Connection Socket
     */
    private void closeConnection() {
        try {
            socket.close();
            bReader.close();
            dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
    }

    /**
     * @param logString Synchronized method to write String into logfile
     */
    private void log(String logString) {
        if (logging) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            //Logging timestamp, incoming request, InetAdress, Port
            String writeString = timestamp + " " + logString + " " + socket.getInetAddress() + " " + socket.getPort();
            //Synchronized since sharedLogString is used by all HTTPServerThread Threads
            synchronized (HttpServerMain.sharedLogString) {
                //Append new line to sharedLogString
                HttpServerMain.sharedLogString = HttpServerMain.sharedLogString + "\n" + writeString;
            }
        }
    }

}
