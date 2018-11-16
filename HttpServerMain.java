package Lection06;

/*
Rechnernetze 6.2 Java Sockets: HTTP-Server (multi-thread)
Michael Gundacker 1646765
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerMain {

    private static ServerSocket serverSocket;
    private static Socket connectionSocket;
    private static boolean logging = true;
    private static LoggingThread loggingThread;
    protected static String sharedLogString = "";

    public static void main(String[] args) {

        if(args.length < 2){
            throw new IllegalArgumentException("Error. Check parameter (e.g. java Lection06/HTTPServer <portNo> <documentRoot>");
        }
        try {   //Create ServerSocket listening for incoming connections
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Starting service on port " + args[0] + ", documentRoot = " + args[1]);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create logging Thread if logging activated
        if(logging){
            loggingThread = new LoggingThread();
            loggingThread.start();
        }

        while(true){
            HTTPServerThread thread;
            try{
                /* Wait for incoming connection
                 * if so, bind to Socket. Then start a new Thread
                 * Thus ServerSocket will be free to listen to new connections.
                */
                connectionSocket = serverSocket.accept();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            thread = new HTTPServerThread(connectionSocket, args[1], logging);
            thread.start();
        }
    }
}
