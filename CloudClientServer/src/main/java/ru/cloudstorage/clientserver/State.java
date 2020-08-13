package ru.cloudstorage.clientserver;

public enum State {
    IDLE,
    AUTH,
    REGISTRATION,
    FILE_LIST,
    FILE_GET,
    FILE_PUT,
    FILE_DELETE
}
