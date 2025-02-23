package com.example.segfaultsquadapplication.db;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 * This is the local copy of the data in the database.
 * @param <T> The type this database is designed to hold.
 */
public class LocalDbCopy<T> {
    // All data should be stored here.
    private final Class<T> classType;
    private HashMap<String, DataInstanceHolder<T>> allData;
    private final String collectionName;
    private FirebaseFirestore db;

    // Constructor
    public LocalDbCopy(String collectionName, Class<T> classType) {
        this.collectionName = collectionName;
        this.classType = classType;
        this.db = FirebaseFirestore.getInstance();
        allData = new HashMap<>();
        synchronizeFromDb();
        listenDbChanges();
    }

    // CLEARS the locally saved data and synchronize from the remote database.
    private void synchronizeFromDb() {
        allData.clear();
        db.collection(collectionName)
                .get().addOnSuccessListener( (result) -> {
                    for (DocumentSnapshot doc : result) {
                        String path = doc.getReference().getPath();
                        allData.put(path, new DataInstanceHolder<>(
                                doc.toObject(classType) , path ));
                    }
                });
    }

    // Listen to database changes
    private void listenDbChanges() {
        db.collection(collectionName).addSnapshotListener((snapshot, e) -> {
            if (snapshot == null)
                return;
            for (DocumentChange change : snapshot.getDocumentChanges()) {
                String path = change.getDocument().getReference().getPath();
                allData.put(path, new DataInstanceHolder<>(change.getDocument().toObject(classType), path));
            }
        });
    }

    // Returns the first data as specified
    @Nullable
    public DataInstanceHolder<T> getFirstData(Predicate<DataInstanceHolder<T>> predicate) {
        for (DataInstanceHolder<T> data : allData.values()) {
            if (data.isActive() && predicate.test(data)) {
                return data;
            }
        }
        return null;
    }

    // Returns the list of filtered data as specified to prevent modification of allDat
    public ArrayList<DataInstanceHolder<T>> getData(Predicate<DataInstanceHolder<T>> predicate) {
        ArrayList<DataInstanceHolder<T>> result = new ArrayList<>();
        for (DataInstanceHolder<T> data : allData.values()) {
            if (data.isActive() && predicate.test(data)) {
                result.add(data);
            }
        }
        return result;
    }

    // Adds a new data to the database.
    public void add(T newData,
                       @Nullable OnSuccessListener<DocumentReference> successCallBack,
                       @Nullable OnFailureListener failureCallBack) {
        Task<DocumentReference> task = db.collection(collectionName)
                .add(newData)
                // Save to local copy on success
                .addOnSuccessListener( documentReference -> {
                    String path = documentReference.getPath();
                    allData.put(path, new DataInstanceHolder<>(newData, path));
                });
        if (successCallBack != null)
            task.addOnSuccessListener(successCallBack);
        if (failureCallBack != null)
            task.addOnFailureListener(failureCallBack);
    }

    // Modifies the data contained in the instance holder.
    public void modify(DataInstanceHolder<T> instance, T newData,
                       @Nullable OnSuccessListener<Void> successCallBack,
                       @Nullable OnFailureListener failureCallBack) {
        Task<Void> task = db.document(instance.getFileName())
                .set(newData)
                // Modify the local copy on success
                .addOnSuccessListener(e -> {
                    instance.setData(newData);
                });
        if (successCallBack != null)
            task.addOnSuccessListener(successCallBack);
        if (failureCallBack != null)
            task.addOnFailureListener(failureCallBack);
    }

    // Deletes the data contained in the instance holder.
    public void delete(DataInstanceHolder<T> instance,
                       @Nullable OnSuccessListener<Void> successCallBack,
                       @Nullable OnFailureListener failureCallBack) {
        Task<Void> task = db.document(instance.getFileName())
                .delete()
                // Mark the local copy for removal
                .addOnSuccessListener(e -> {
                    instance.delete();
                });
        if (successCallBack != null)
            task.addOnSuccessListener(successCallBack);
        if (failureCallBack != null)
            task.addOnFailureListener(failureCallBack);
    }
}
