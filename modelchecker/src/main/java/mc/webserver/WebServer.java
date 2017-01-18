package mc.webserver;

import spark.Spark;

import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.get;


public class WebServer {
  public static void main(String[] args) {
    Spark.externalStaticFileLocation("app");
    Spark.port(5000);
    new WebSocketServer();
    get("/bower_components/*", (req, res) -> String.join("\n",Files.readAllLines(Paths.get(req.pathInfo().substring(1)))));
  }
}
