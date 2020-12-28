package ru.cloudstorage.client.controllers;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import ru.cloudstorage.clientserver.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class LeftPanelController extends PanelController {


    @Override
    public void updateList(Path path) {
        try {
            disksBox.getSelectionModel().select(path.toAbsolutePath().getRoot().toString());
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).filter(Files::isReadable).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
