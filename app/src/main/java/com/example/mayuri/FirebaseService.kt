package com.example.mayuri

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthResult(val role: String? = null, val error: String? = null)

object FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- Authentication ---
    fun getCurrentUser() = auth.currentUser
    
    suspend fun signIn(email: String, pass: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                // Fetch user role from Firestore
                val doc = db.collection("users").document(user.uid).get().await()
                var role = doc.getString("role")
                
                val cleanEmail = email.lowercase().trim()
                
                // Ensure owner@mayuri.com ALWAYS gets the owner role
                if (cleanEmail == "owner@mayuri.com") {
                    if (role != "owner") {
                        role = "owner"
                        db.collection("users").document(user.uid).set(mapOf(
                            "uid" to user.uid,
                            "email" to cleanEmail,
                            "role" to "owner"
                        ), com.google.firebase.firestore.SetOptions.merge()).await()
                    }
                } else if (role == null) {
                    role = "user"
                    db.collection("users").document(user.uid).set(mapOf(
                        "uid" to user.uid,
                        "email" to cleanEmail,
                        "role" to role
                    )).await()
                }
                
                AuthResult(role = role)
            } else AuthResult(error = "User not found")
        } catch (e: Exception) {
            AuthResult(error = e.localizedMessage)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                val cleanEmail = email.lowercase().trim()
                val role = if (cleanEmail == "owner@mayuri.com") "owner" else "user"
                val userData = mapOf(
                    "uid" to user.uid,
                    "name" to name,
                    "email" to cleanEmail,
                    "role" to role
                )
                db.collection("users").document(user.uid).set(userData).await()
                AuthResult(role = role)
            } else AuthResult(error = "Registration failed")
        } catch (e: Exception) {
            AuthResult(error = e.localizedMessage)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // --- Menu Management (Real-time) ---
    fun getMenuFlow(): Flow<List<Dish>> = callbackFlow {
        val subscription = db.collection("menu").addSnapshotListener { snapshot, _ ->
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
    fun getOrdersFlow(): Flow<List<Order>> = callbackFlow {
        val subscription = db.collection("orders").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject<Order>() })
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun placeOrder(order: Order) {
        db.collection("orders").document(order.id).set(order).await()
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        db.collection("orders").document(orderId).update("status", status).await()
    }

    // --- Seed Demo Data ---
    suspend fun seedDemoData() {
        val demoMenu = listOf(
            Dish("", "Butter Chicken", "🍗", "Main", false, "Creamy tomato gravy with grilled chicken", 350, true, true),
            Dish("", "Paneer Tikka", "🍢", "Starter", true, "Grilled cottage cheese with spices", 280, false, true),
            Dish("", "Dal Makhani", "🥣", "Main", true, "Slow-cooked black lentils", 220, false, false),
            Dish("", "Garlic Naan", "🫓", "Bread", true, "Freshly baked bread with garlic", 60, false, true),
            Dish("", "Gulab Jamun", "🍩", "Dessert", true, "Sweet milk solids in syrup", 120, true, false)
        )
        for (dish in demoMenu) {
            addDish(dish)
        }
    }
}
