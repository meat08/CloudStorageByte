package ru.cloudstorage.client.controllers;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import ru.cloudstorage.client.util.FileUtilClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class RightPanelController extends PanelController {

    private String serverRootPath;
    private String serverClientPath;

    @Override
    public void globalUpdateList() {
        updateList(Paths.get(serverClientPath));
    }

    @Override
    public void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toString());
            FileUtilClient.setFileList(filesTable, path.toString());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.getDialogPane().setHeaderText(null);
            alert.showAndWait();
        }
    }

    public void btnPathUpActionR() {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null & !Objects.equals(upperPath, Paths.get(serverRootPath))) {
            updateList(upperPath);
        }
    }

    public void setServerPaths(String serverRootPath, String serverClientPath) {
        this.serverRootPath = serverRootPath;
        this.serverClientPath = serverClientPath;
    }
}
