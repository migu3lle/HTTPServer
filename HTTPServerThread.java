package Lection06;

/*
Rechnernetze 6.2 Java Sockets: HTTP-Server (multi-thread)
Michael Gundacker 1646765
 */

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;

public class HTTPServerThread extends Thread {

    private Socket socket;
    private String documentRoot;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private DataOutputStream dataWriter;
    private boolean connected = true;
    private boolean logging;
    private String[] commandArr;

    public HTTPServerThread(Socket socket, String documentRoot, boolean logging) {
        this.socket = socket;
        this.documentRoot = documentRoot;
        this.logging = logging;
    }

    /**
     * Thread run() method with endless loop
     */
    public void run() {
        System.out.println("Client " + socket.getInetAddress() + " connected.");
        initialize();
        //Doing the request/response mechanism
        do {
            readCommand();
            if (connected) {  //readCommand() might close the connection
                processGET();
            }
        } while (connected);    //do until connection is closed
    }

    /**
     * Initializes some Writer and Reader objects
     */
    private void initialize() {
        try {
            bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //BufferedWriter for sending HTML files (String)
            bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //DataOutputStream for sending binary files (Byte[])
            dataWriter = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processing source path and check whether a file or a directory is requested
     * Then build the respective file path
     */
    private void processGET() {
        String srcPath;
        srcPath = commandArr[1];
        Path filePath;


        //Since it is no longer just one Thread, several connections will be accepted
        //even if one is blocked
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Identifier to check if substring after last '/' contains a '.'
        String identifier = srcPath.substring(srcPath.lastIndexOf('/'));
        System.out.println("Identifier: " + identifier);

        //Either it is a folder (ending with '/')
        if (srcPath.charAt(srcPath.length() - 1) == '/') {
            filePath = Paths.get(documentRoot + srcPath + "index.html");
        }
        //Or it is a folder (no '.' within identifier)
        else if (identifier.indexOf('.') == -1) {
            filePath = Paths.get(documentRoot + srcPath + "/index.html");
        }
        //Or it is a file
        else {
            filePath = Paths.get(documentRoot + srcPath);
        }
        Object fileObject = getFile(filePath);
        sendFile(fileObject);
    }

    /**
     * @param path to the requested file (at this point a directory was converted to /index.html)
     * @return Object
     * Keep return value generic for sendFile() method, return value either be
     * * String - for HTML file
     * * Byte[] - for binary file
     */
    private Object getFile(Path path) {
        System.out.println("Path: " + path.toString());

        //File to be transmitted as clear text (HTML files only)
        if (path.toString().substring(path.toString().lastIndexOf('.')).equalsIgnoreCase(".html")) {
            System.out.println("HTML File requested");
            try {
                //Store HTML File in Byte Array then convert to String and return
                log(path.toString().substring(56));
                byte[] encoded = Files.readAllBytes(path);
                System.out.println("Sending file: " + path.toString().substring(56));
                return new String(encoded);
            } catch (IOException e) {
                //In case file does not exist close the connection and return null (to be checked in sendFile() )
                System.out.println("Requested file does not exist on server - Closing connection.");
                closeConnection();
                return null;
            }
        }
        //File to be transmitted as binary File
        else {
            System.out.println("Other File requested");
            File file = new File(path.toString());
            byte[] byteArray = new byte[(int) file.length()];

            BufferedInputStream bInputStream;
            try {
                //Read from file with BufferedInputStream and read into Byte Array to return
                log(path.toString().substring(56));
                bInputStream = new BufferedInputStream(new FileInputStream(file));
                bInputStream.read(byteArray, 0, byteArray.length);
                return byteArray;
            } catch (FileNotFoundException e) {
                //In case file does not exist close the connection and return null (to be checked in sendFile() )
                System.out.println("Requested file does not exist on server - Closing connection.");
                closeConnection();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Reading HTTP request and check for syntax (only GET allowed)
     */
    private void readCommand() {
        String command = null;
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
        if (!commandMethod.equalsIgnoreCase("GET")) {
            closeConnection();
        }
    }

    /**
     * @param fileObject - either String (for HTML) or Byte[] (for binary file)
     */
    private void sendFile(Object fileObject) {
        if (fileObject == null) {
            return; //fileObject (String) might be null if file does not exist. Then skip send process
        }
        //Case String (HTML)
        if (fileObject instanceof String) {
            String fileString = (String) fileObject;
            try {
                bWriter.write(fileString);
                bWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }
        //Case Byte[] (binary File)
        else if (fileObject instanceof byte[]) {
            byte[] byteArray = (byte[]) fileObject;
            try {
                dataWriter.write(byteArray, 0, byteArray.length);
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
            bWriter.close();
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
            String writeString = timestamp + " GET " + logString;
            synchronized (MainThread.sharedLogString) {
                MainThread.sharedLogString = MainThread.sharedLogString + "\n" + writeString;
            }
        }
    }
}
