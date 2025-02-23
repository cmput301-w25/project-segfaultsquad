package com.example.segfaultsquadapplication.db;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This holder is used to store data locally in LocalDbCopy.
 * In the future, information on the instance (whether it is already on the server etc.) will also be stored here.
 * NOTE: synchronization between local and remote database is handled in LocalDbCopy!
 * @param <T> The type of the contained data.
 */
public class DataInstanceHolder<T> {
    // The data contained within this holder
    private T data;
    // The file name used in the database
    private final String fileName;
    // Whether this record has been deleted;
    // deletion is expected to be an occasional operation; simply use a flag for overall less overhead.
    private boolean isActive;

    public DataInstanceHolder(T data, String fileName) {
        this.data = data;
        this.fileName = fileName;
        this.isActive = true;
    }

    // Gets the data within this instance
    public T getData() {
        return data;
    }

    // Updates the data within this instance
    public void setData(T newData) {
        this.data = newData;
    }

    // Notifies this data instance holder for deletion
    public void delete() {
        isActive = false;
    }

    public boolean isActive() {
        return isActive;
    }

    // Gets the file name used to store the instance
    public String getFileName() {
        return fileName;
    }
}
