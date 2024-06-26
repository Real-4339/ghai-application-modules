package fiit.vava.client.controllers;

import fiit.vava.client.Router;
import fiit.vava.client.StubsManager;
import fiit.vava.server.Empty;
import fiit.vava.server.User;
import fiit.vava.server.UserServiceGrpc;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AppController {

    private static final Logger logger = LoggerFactory.getLogger("client." + AppController.class);

    @FXML
    private BorderPane borderPane;

    @FXML
    void initialize() throws IOException {
        handleAuthPage();
    }

    public void handleAuthPage() throws IOException {
        Router router = Router.getInstance();
        router.setAppController(this);

        String startingScenePath = "auth/login";

        // todo uncomment String[] credentials = CredentialsManager.retrieveCredentials();
        String[] credentials = null;

        if (credentials != null) {
            logger.info("Using credentials: " + credentials[0] + " " + credentials[1]);

            UserServiceGrpc.UserServiceBlockingStub stub = StubsManager.getInstance().getUserServiceBlockingStub();

            try {
                User user = stub.me(Empty.newBuilder().build());
                startingScenePath = user.getRole().name().toLowerCase();
            } catch (Exception ex) {
                logger.warn("Unable to call me" + ex);
            }
        }

        router.navigateTo(startingScenePath);
    }

    public void setCenter(Node page) {
        borderPane.setCenter(page);
    }

    public void setSidebar(Node node) {
        borderPane.setLeft(node);
    }

    public void setRightSidebar(Node node) {
        borderPane.setRight(node);
    }

    public Node getCenter() {
        return borderPane.getCenter();
    }

    public Node getSidebar() {
        return borderPane.getLeft();
    }
}
