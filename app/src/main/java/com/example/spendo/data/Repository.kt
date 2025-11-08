package com.example.spendo.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class Repository(
    private val auth: FirebaseAuth? = null,
    private val db: FirebaseFirestore? = null
) {

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
        try {
            val firebaseAuth = auth ?: FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            println("Google Sign-In successful. New user: ${authResult.additionalUserInfo?.isNewUser}")

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
                println("New user document created in Firestore")
            } else {
                println("Existing user signed in")
            }
            
            // Explicitly return Unit to indicate success
            Unit
        } catch (e: Exception) {
            println("Error in signInWithGoogle: ${e.message}")
            e.printStackTrace()
            throw e
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
            // Creates a new document with a unique ID
            firestore.collection("transactions").document()
        } else {
            // Uses existing ID if provided
            firestore.collection("transactions").document(tx.id)
        }
        // Set the document ID on the object itself and save it
        doc.set(tx.copy(id = doc.id)).await()
    }

    // Deletes a transaction document from Firestore.
    suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        val firestore = db ?: FirebaseFirestore.getInstance()
        firestore.collection("transactions").document(id).delete().await()
    }

    // Fetches all transactions for a specific user from Firestore with timeout and retry.
    suspend fun userTransactions(userId: String, retryCount: Int = 2): Result<List<Transaction>> = runCatching {
        val firestore = db ?: FirebaseFirestore.getInstance()
        var lastException: Exception? = null
        
        // Try with timeout, fallback to cache if network fails
        repeat(retryCount) { attempt ->
            try {
                val query: Query = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .orderBy("date")
                
                // First try: Use server (network) with timeout
                val snapshot = if (attempt == 0) {
                    withTimeout(10000) { // 10 second timeout
                        query.get(Source.SERVER).await()
                    }
                } else {
                    // Retry: Try cache first, then server
                    try {
                        withTimeout(5000) {
                            query.get(Source.CACHE).await()
                        }
                    } catch (e: Exception) {
                        // If cache fails or not available, try server one more time
                        withTimeout(10000) {
                            query.get(Source.SERVER).await()
                        }
                    }
                }
                
                return@runCatching snapshot.toObjects(Transaction::class.java)
            } catch (e: TimeoutCancellationException) {
                lastException = e
                if (attempt < retryCount - 1) {
                    delay(1000) // Wait 1 second before retry
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < retryCount - 1) {
                    delay(1000)
                }
            }
        }
        
        // If all retries failed, try cache as last resort (if available)
        try {
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("date")
                .get(Source.CACHE)
                .await()
            snapshot.toObjects(Transaction::class.java)
        } catch (e: Exception) {
            // If cache is not available or fails, throw the original exception
            throw lastException ?: Exception("Failed to load transactions: ${e.message}")
        }
    }

    // Sends a password reset email to the given email address using Firebase.
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance()
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
}



