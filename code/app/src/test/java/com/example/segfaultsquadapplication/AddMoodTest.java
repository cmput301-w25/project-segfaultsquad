package com.example.segfaultsquadapplication;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class AddMoodTest {
    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockMoodsCollection;
    @Mock
    private DocumentReference mockDocRef;

    @Before
    public void setUp() {
        // Start up mocks
        MockitoAnnotations.openMocks(this);
        // Define the behaviour we want during our tests. This part is what avoids the calls to firestore.
        when(mockFirestore.collection("moods")).thenReturn(mockMoodsCollection);
        when(mockMoodsCollection.document()).thenReturn(mockDocRef);
        when(mockMoodsCollection.document(anyString())).thenReturn(mockDocRef);
    }

    @Test
    public void
}
