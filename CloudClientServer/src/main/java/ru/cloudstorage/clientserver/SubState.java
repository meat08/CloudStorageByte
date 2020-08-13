package ru.cloudstorage.clientserver;

public enum SubState {
    IDLE,
    LOGIN_SIZE,
    LOGIN_STRING,
    PASS_SIZE,
    PASS_STRING,
    PATH_SIZE,
    PATH_STRING,
    FILE_SIZE,
    FILE
}
