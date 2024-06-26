package it.adrian.code.util.database;

public class Config {

    public static final String CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String DATABASE_NAME = "aphrodite";
    public static final String USER_COLLECTION_NAME = "userData", MESSAGE_COLLECTION_NAME = "messages", MEDIA_COLLECTION_NAME = "medias";
    public static final String CUSTOM_USER_AGENT = "Aphrodite/1.0.1 (Mozilla/5.0 (Aphrodite OS 1.0; NobusCore; AMD x64)";
    public static final String CORS_ORIGIN_PROTECTION = "*";
}