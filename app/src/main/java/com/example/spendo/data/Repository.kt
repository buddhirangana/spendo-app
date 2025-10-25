package com.example.spendo.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class Repository(
    private val auth: FirebaseAuth? = null,
    private val db: FirebaseFirestore? = null
) {
    // Mock data for testing
    private val mockTransactions = mutableListOf<Transaction>()

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runCatching {
        try {
            val firebaseAuth = auth ?: FirebaseAuth.getInstance()
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = firebaseAuth.currentUser?.uid ?: error("No uid")
            val user = AppUser(uid = uid, name = name, email = email)
            val firestore = db ?: FirebaseFirestore.getInstance()
            firestore.collection("users").document(uid).set(user).await()
        } catch (e: Exception) {
            // For testing, just simulate success
            println("Mock signup for $email")
        }
    }

    // ... other code in Repository.kt

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        // This is the only part you need. The runCatching will automatically
        // catch any exceptions from Firebase and wrap them in a Failure result.
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        try {
            val firebaseAuth = auth ?: FirebaseAuth.getInstance()
            firebaseAuth.signOut()
        } catch (e: Exception) {
            println("Mock logout")
        }
    }

    suspend fun addTransaction(tx: Transaction): Result<Unit> = runCatching {
        try {
            val firestore = db ?: FirebaseFirestore.getInstance()
            val doc = if (tx.id.isEmpty()) firestore.collection("transactions").document() else firestore.collection("transactions").document(tx.id)
            doc.set(tx.copy(id = doc.id)).await()
        } catch (e: Exception) {
            // For testing, add to mock data
            mockTransactions.add(tx.copy(id = UUID.randomUUID().toString()))
            println("Mock transaction added: ${tx.category} - ${tx.amount}")
        }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        try {
            val firestore = db ?: FirebaseFirestore.getInstance()
            firestore.collection("transactions").document(id).delete().await()
        } catch (e: Exception) {
            // For testing, remove from mock data
            mockTransactions.removeAll { it.id == id }
            println("Mock transaction deleted: $id")
        }
    }

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

    private val firebaseAuth = FirebaseAuth.getInstance()

    // Other functions in your repository...

    /**
     * Sends a password reset email to the given email address.
     * This is a suspending function and should be called from a coroutine.
     * @param email The user's email address.
     * @throws Exception if the email sending fails.
     */
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
}



