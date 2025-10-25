package com.example.spendo.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class Repository(
    private val auth: FirebaseAuth? = null,
    private val db: FirebaseFirestore? = null
) {
    // Mock data for testing
    private val mockTransactions = mutableListOf<Transaction>()

    // Signs up a new user and creates a corresponding user document in Firestore.
    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = firebaseAuth.currentUser?.uid ?: error("Could not get user UID after signup.")
        val user = AppUser(uid = uid, name = name, email = email)
        val firestore = db ?: FirebaseFirestore.getInstance()
        firestore.collection("users").document(uid).set(user).await()
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()

        // Check if this is a new user
        if (authResult.additionalUserInfo?.isNewUser == true) {
            val user = authResult.user ?: error("Could not get user details after Google sign-in.")
            val appUser = AppUser(
                uid = user.uid,
                name = user.displayName ?: "No Name",
                email = user.email ?: ""
            )
            // Create a document for the new user in Firestore
            val firestore = db ?: FirebaseFirestore.getInstance()
            firestore.collection("users").document(user.uid).set(appUser).await()
        }
    }

    // Logs in an existing user.
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    // Logs out the current user.
    fun logout() {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.signOut()
    }

    // Adds or updates a transaction document in Firestore.
    suspend fun addTransaction(tx: Transaction): Result<Unit> = runCatching {
        val firestore = db ?: FirebaseFirestore.getInstance()
        val doc = if (tx.id.isEmpty()) {
            firestore.collection("transactions").document()
        } else {
            firestore.collection("transactions").document(tx.id)
        }
        // Always set the final document ID before saving
        doc.set(tx.copy(id = doc.id)).await()
    }

    // Deletes a transaction document from Firestore.
    suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        val firestore = db ?: FirebaseFirestore.getInstance()
        firestore.collection("transactions").document(id).delete().await()
    }

    // Fetches all transactions for a specific user from Firestore.
    suspend fun userTransactions(userId: String): Result<List<Transaction>> = runCatching {
        try {
            val firestore = db ?: FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("date")
                .get()
                .await()
            snapshot.toObjects(Transaction::class.java)
        } catch (e: Exception) {
            // For testing, return mock data
            println("Mock transactions for user: $userId")
            mockTransactions.toList()
        }
    }

    // Sends a password reset email to the given email address using Firebase.
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
}



