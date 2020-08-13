package ru.cloudstorage.clientserver;

public class ByteCommand {
    public static final byte AUTHORISE_COMMAND = 10;
    public static final byte LIST_COMMAND = 11;
    public static final byte FROM_SERVER_COMMAND = 12;
    public static final byte TO_SERVER_COMMAND = 13;
    public static final byte DELETE_FILE_COMMAND = 14;
    public static final byte SUCCESS_AUTH_COMMAND = 15;
    public static final byte FAIL_AUTH_COMMAND = 16;
    public static final byte ALREADY_AUTH_COMMAND = 17;
    public static final byte GET_ROOT_PATH_COMMAND = 18;
    public static final byte FINISH_COMMAND = 19;
    public static final byte REGISTRATION_COMMAND = 20;
    public static final byte LOGIN_EXIST_COMMAND = 21;
    public static final byte LOGIN_REGISTERED_COMMAND = 22;
}
