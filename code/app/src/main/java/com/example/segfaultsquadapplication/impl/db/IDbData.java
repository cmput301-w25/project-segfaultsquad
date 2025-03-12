package com.example.segfaultsquadapplication.impl.db;

/**
 * Data that are expected to be **retrieved** by the database should implement this,
 * So the document ID can be automatically inserted while retrieving the data.
 */
public interface IDbData {
    /**
     * setDbFileId IS ONLY CALLED BY THE DATABASE UTILS WHEN RETRIEVING DATA FROM DOCUMENT </br>
     * If this method is called elsewhere, please double-check the data class's implementation.
     */
    void setDbFileId(String id);

    /**
     * Gets the instance's file name in the database. </br>
     * This is expected to be called outside database utils as well.
     */
    String getDbFileId();
}
