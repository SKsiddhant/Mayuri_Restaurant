package com.example.mayuri

data class Dish(
    val id: String = "",
    val name: String = "", 
    val emoji: String = "", 
    val category: String = "", 
    val veg: Boolean = true,
    val desc: String = "", 
    val price: Int = 0, 
    val special: Boolean = false, 
    val popular: Boolean = false
)

data class Order(
    val id: String = "", 
    val date: String = "", 
    var status: String = "", 
    val items: List<String> = emptyList(), 
    val total: Int = 0, 
    val customerName: String = "",
    val address: String = ""
)
