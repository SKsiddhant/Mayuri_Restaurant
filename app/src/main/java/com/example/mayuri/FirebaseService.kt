package com.example.mayuri

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthResult(val role: String? = null, val error: String? = null)

object FirebaseService {
    private const val TAG = "FirebaseService"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- Authentication ---
    fun getCurrentUser() = auth.currentUser

    /**
     * Reads the role stored for [uid] in Firestore. Returns null if it cannot be read.
     *
     * The role is ONLY ever read here, never decided by the app. It used to be granted to anyone whose e-mail
     * was "owner@mayuri.com" (and written back to Firestore by the client), so whoever registered that address
     * - or used the demo button - became owner. Owners must now be promoted from the Firebase console
     * (see README) and firestore.rules refuse client writes to the role field.
     */
    suspend fun fetchRole(uid: String): String? = try {
        db.collection("users").document(uid).get().await().getString("role")
    } catch (e: Exception) {
        Log.w(TAG, "Could not read role", e)
        null
    }

    suspend fun signIn(email: String, pass: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: return AuthResult(error = "User not found")
            var role = fetchRole(user.uid)
            if (role == null) {
                // First sign-in of an account without a profile document: always a customer
                role = "user"
                db.collection("users").document(user.uid).set(mapOf(
                    "uid" to user.uid,
                    "email" to email.lowercase().trim(),
                    "role" to role
                )).await()
            }
            AuthResult(role = role)
        } catch (e: Exception) {
            AuthResult(error = e.localizedMessage)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: return AuthResult(error = "Registration failed")
            val userData = mapOf(
                "uid" to user.uid,
                "name" to name.trim(),
                "email" to email.lowercase().trim(),
                "role" to "user"
            )
            db.collection("users").document(user.uid).set(userData).await()
            AuthResult(role = "user")
        } catch (e: Exception) {
            AuthResult(error = e.localizedMessage)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // --- Menu Management (Real-time) ---
    fun getMenuFlow(): Flow<List<Dish>> = callbackFlow {
        val subscription = db.collection("menu").addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Errors (e.g. permission denied) were silently dropped; at least leave a trace
                Log.w(TAG, "Menu listener failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject<Dish>() })
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addDish(dish: Dish) {
        val docRef = if (dish.id.isEmpty()) db.collection("menu").document() else db.collection("menu").document(dish.id)
        val newDish = dish.copy(id = docRef.id)
        docRef.set(newDish).await()
    }

    suspend fun updateDish(dish: Dish) {
        db.collection("menu").document(dish.id).set(dish).await()
    }

    suspend fun deleteDish(dishId: String) {
        db.collection("menu").document(dishId).delete().await()
    }

    // --- Orders (Real-time) ---
    /** Owners see every order; customers only their own (this is also what firestore.rules enforce). */
    fun getOrdersFlow(allOrders: Boolean): Flow<List<Order>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val query: Query = if (allOrders) db.collection("orders") else db.collection("orders").whereEqualTo("userId", uid ?: "")
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Orders listener failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject<Order>() })
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun placeOrder(order: Order) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Please sign in again.")
        val id = order.id.ifEmpty { db.collection("orders").document().id }
        db.collection("orders").document(id).set(order.copy(id = id, userId = uid)).await()
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        db.collection("orders").document(orderId).update("status", status).await()
    }

    // --- Seed Demo Data ---
    /** Idempotent: fixed ids mean pressing the button twice no longer duplicates the whole menu. */
    suspend fun seedDemoData() {
        val demoMenu = listOf(
            Dish("butter-chicken", "Butter Chicken", "🍗", "Main", false, "Creamy tomato gravy with grilled chicken", 350, true, true),
            Dish("paneer-tikka", "Paneer Tikka", "🍢", "Starter", true, "Grilled cottage cheese with spices", 280, false, true),
            Dish("dal-makhani", "Dal Makhani", "🥣", "Main", true, "Slow-cooked black lentils", 220, false, false),
            Dish("garlic-naan", "Garlic Naan", "🫓", "Bread", true, "Freshly baked bread with garlic", 60, false, true),
            Dish("gulab-jamun", "Gulab Jamun", "🍩", "Dessert", true, "Sweet milk solids in syrup", 120, true, false)
        )
        for (dish in demoMenu) {
            addDish(dish)
        }
    }
}
