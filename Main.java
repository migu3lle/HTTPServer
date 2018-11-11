package Lection06;

public class Main {

    public static void main(String[] args) {
        if(args.length < 2){
            throw new IllegalArgumentException("Error. Check parameter (e.g. java Lection06/HTTPServer <portNo> <documentRoot>");
        }

        HTTPServer server = new HTTPServer(Integer.parseInt(args[0]), args[1]);
        server.runServer();
    }
}
