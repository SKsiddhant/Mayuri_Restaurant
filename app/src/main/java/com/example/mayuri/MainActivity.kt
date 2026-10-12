package com.example.mayuri

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Runs a Firestore write and shows the error instead of crashing the app (e.g. permission denied, offline). */
suspend fun runSafely(context: android.content.Context, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Toast.makeText(context, e.localizedMessage ?: "Something went wrong", Toast.LENGTH_LONG).show()
    }
}

// --- COLORS ---
val Saffron = Color(0xFFF4C430)
val Gold = Color(0xFFFFD700)
val DeepBg = Color(0xFF1A1A1A)
val CardBg = Color(0xFF2D2D2D)
val Cream = Color(0xFFFFFDD0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayuriTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen { showSplash = false }
                } else {
                    RestaurantApp()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1.2f, tween(1000, easing = FastOutSlowInEasing))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(800)
        onFinish()
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value, alpha = alpha.value)) {
            Text("🍛", fontSize = 100.sp)
            Spacer(Modifier.height(16.dp))
            Text("MAYURI", fontSize = 52.sp, color = Saffron, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
            Text("Authentic Indian Cuisine", color = Gold, fontSize = 18.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun RestaurantApp() {
    val navController = rememberNavController()
    var userRole by remember { mutableStateOf<String?>(null) }
    val cart = remember { mutableStateMapOf<String, Int>() }
    val menuItems = remember { mutableStateListOf<Dish>() }
    val orders = remember { mutableStateListOf<Order>() }
    val scope = rememberCoroutineScope()
    var isCheckingAuth by remember { mutableStateOf(true) }
    val appContext = LocalContext.current

    // Auto-login and Data Sync
    LaunchedEffect(Unit) {
        val currentUser = FirebaseService.getCurrentUser()
        if (currentUser != null) {
            // Role comes from Firestore only. (This used to call signIn() with an empty password - which can
            // never succeed - and then decide the role from the e-mail address.) Least privilege if unreadable.
            userRole = FirebaseService.fetchRole(currentUser.uid) ?: "user"
        }
        isCheckingAuth = false
    }

    LaunchedEffect(userRole) {
        if (userRole != null) {
            launch {
                FirebaseService.getMenuFlow().collectLatest { items ->
                    menuItems.clear()
                    menuItems.addAll(items)
                }
            }
            launch {
                FirebaseService.getOrdersFlow(allOrders = userRole == "owner").collectLatest { items ->
                    orders.clear()
                    orders.addAll(items)
                }
            }
        }
    }

    if (isCheckingAuth) {
        Box(Modifier.fillMaxSize().background(DeepBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Saffron)
        }
    } else {
        AnimatedContent(targetState = userRole == null, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) }, label = "") { isLogin ->
            if (isLogin) {
                AuthScreen { role -> userRole = role }
            } else {
                Scaffold(
                    bottomBar = {
                        if (userRole == "owner") OwnerBottomNav(navController)
                        else CustomerBottomNav(navController, cart.values.sum())
                    },
                    containerColor = DeepBg
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        NavHost(navController, startDestination = if (userRole == "owner") "dashboard" else "home") {
                            composable("home") { HomeScreen(navController, cart, menuItems) }
                            composable("menu") { MenuScreen(cart, menuItems) }
                            composable("cart") { CartScreen(navController, cart, menuItems) }
                            composable("payment") { PaymentScreen(navController, cart, menuItems) }
                            composable("orders") { CustomerOrdersScreen(orders) }
                            composable("profile") { ProfileScreen(userRole == "owner") { 
                                FirebaseService.signOut()
                                userRole = null
                                cart.clear() 
                            } }
                            
                            composable("dashboard") { OwnerDashboard(navController, orders, menuItems) }
                            composable("owner_orders") { OwnerOrdersScreen(orders) { id, status ->
                                scope.launch {
                                    runSafely(appContext) { FirebaseService.updateOrderStatus(id, status) }
                                }
                            }}
                            composable("owner_menu") { OwnerMenuScreen(menuItems) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onLogin: (String) -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(DeepBg).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MAYURI", fontSize = 48.sp, color = Saffron, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(Modifier.padding(24.dp)) {
                Text(if(isSignUp) "Create Account" else "Welcome Back", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
                Spacer(Modifier.height(16.dp))
                if(isSignUp) {
                    OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { 
                        scope.launch {
                            loading = true
                            val result = if(isSignUp) FirebaseService.signUp(email, pass, name) else FirebaseService.signIn(email, pass)
                            loading = false
                            if (result.role != null) onLogin(result.role!!)
                            else Toast.makeText(context, result.error ?: "Authentication Failed", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                    enabled = !loading
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text(if(isSignUp) "Sign Up" else "Sign In")
                }
                TextButton(onClick = { isSignUp = !isSignUp }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if(isSignUp) "Already have an account? Sign In" else "New here? Create account", fontSize = 12.sp, color = Saffron)
                }
                
                if (BuildConfig.DEBUG) { // demo logins (with a published password) must not ship in release builds
                Divider(Modifier.padding(vertical = 16.dp), color = Color.Gray)
                
                Text("Demo Accounts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold, modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { email = "user@mayuri.com"; pass = "123456" },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Saffron)
                    ) { Text("User Demo", fontSize = 11.sp, color = Saffron) }
                    
                    OutlinedButton(
                        onClick = { email = "owner@mayuri.com"; pass = "123456" },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Gold)
                    ) { Text("Owner Demo", fontSize = 11.sp, color = Gold) }
                }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController, cart: MutableMap<String, Int>, menuItems: List<Dish>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Good Evening 👋", color = Cream, modifier = Modifier.alpha(0.8f))
            Text("MAYURI", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Saffron)) {
                Column(Modifier.padding(16.dp)) {
                    Text("🎉 Get 20% OFF", fontWeight = FontWeight.Bold, color = DeepBg)
                    Text("Code: WELCOME20", fontSize = 12.sp, color = DeepBg)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Chef's Special", fontSize = 22.sp, color = Gold, fontWeight = FontWeight.Bold)
        }
        item {
            LazyRow(Modifier.padding(vertical = 12.dp)) {
                items(menuItems.filter { it.special }) { dish ->
                    DishCard(dish, cart.getOrDefault(dish.id, 0),
                        onAdd = { cart[dish.id] = cart.getOrDefault(dish.id, 0) + 1 },
                        onRemove = { if ((cart[dish.id] ?: 0) > 1) cart[dish.id] = (cart[dish.id] ?: 0) - 1 else cart.remove(dish.id) }
                    )
                }
            }
        }
        item { Text("Popular Picks", fontSize = 22.sp, color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
        items(menuItems.filter { it.popular }) { dish ->
            HorizontalDishItem(dish, cart.getOrDefault(dish.id, 0),
                onAdd = { cart[dish.id] = cart.getOrDefault(dish.id, 0) + 1 },
                onRemove = { if ((cart[dish.id] ?: 0) > 1) cart[dish.id] = (cart[dish.id] ?: 0) - 1 else cart.remove(dish.id) }
            )
        }
    }
}

@Composable
fun MenuScreen(cart: MutableMap<String, Int>, menuItems: List<Dish>) {
    var searchTerm by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Menu", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = searchTerm, onValueChange = { searchTerm = it }, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(menuItems.filter { it.name.contains(searchTerm, ignoreCase = true) }) { dish ->
                HorizontalDishItem(dish, cart.getOrDefault(dish.id, 0),
                    onAdd = { cart[dish.id] = cart.getOrDefault(dish.id, 0) + 1 },
                    onRemove = { if ((cart[dish.id] ?: 0) > 1) cart[dish.id] = (cart[dish.id] ?: 0) - 1 else cart.remove(dish.id) }
                )
            }
        }
    }
}

@Composable
fun CartScreen(navController: NavHostController, cart: MutableMap<String, Int>, menuItems: List<Dish>) {
    val cartItems = cart.mapNotNull { entry -> menuItems.find { it.id == entry.key }?.let { it to entry.value } }
    val subtotal = cartItems.sumOf { it.first.price * it.second }
    val total = (subtotal * 1.05 + if(subtotal > 0) 40 else 0).toInt()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Cart", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
        if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your cart is empty 🛒", color = Cream) }
        } else {
            LazyColumn(Modifier.weight(1f).padding(vertical = 16.dp)) {
                items(cartItems) { (dish, qty) ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(dish.emoji, fontSize = 32.sp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(dish.name, fontWeight = FontWeight.Bold, color = Cream)
                                Text("₹${dish.price}", color = Saffron)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if(qty > 1) cart[dish.id] = qty - 1 else cart.remove(dish.id) }) { Text("-", color = Saffron) }
                                Text("$qty", color = Cream, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { cart[dish.id] = qty + 1 }) { Text("+", color = Saffron) }
                            }
                        }
                    }
                }
            }
            Button(onClick = { navController.navigate("payment") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
                Text("Proceed to Checkout • ₹$total", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavHostController, cart: MutableMap<String, Int>, menuItems: List<Dish>) {
    var step by remember { mutableStateOf(1) }
    val context = LocalContext.current
    val cartItemsList = cart.mapNotNull { entry -> menuItems.find { it.id == entry.key }?.let { "${it.name} x${entry.value}" } }
    val subtotal = cart.entries.sumOf { entry -> menuItems.find { it.id == entry.key }?.price?.times(entry.value) ?: 0 }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        when(step) {
            1 -> {
                Column {
                    Text("Checkout", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
                    listOf("UPI", "Card", "Cash on Delivery").forEach { method ->
                        Card(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payment, null, tint = Saffron)
                                Spacer(Modifier.width(16.dp))
                                Text(method, fontWeight = FontWeight.Bold, color = Cream)
                            }
                        }
                    }
                }
            }
            2 -> {
                LaunchedEffect(Unit) {
                    val user = FirebaseService.getCurrentUser()
                    val order = Order(
                        id = "ORD${System.currentTimeMillis()}",
                        date = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                        status = "pending",
                        items = cartItemsList,
                        total = (subtotal * 1.05 + 40).toInt(),
                        customerName = user?.email ?: "Guest",
                        address = "Saved Address",
                        userId = user?.uid ?: ""
                    )
                    try {
                        FirebaseService.placeOrder(order)
                        cart.clear() // otherwise pressing Back from the success screen let the same cart be ordered again
                        step = 3
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // This used to crash the app (uncaught in LaunchedEffect) and showed "Successful!" regardless
                        Toast.makeText(context, "Could not place the order: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        step = 1
                    }
                }
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Saffron)
                    Text("Processing...", color = Cream)
                }
            }
            3 -> {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(100.dp), tint = Color.Green)
                    Text("Successful!", fontSize = 28.sp, color = Gold, fontWeight = FontWeight.Bold)
                    Button(onClick = { cart.clear(); navController.navigate("home") { popUpTo("home") { inclusive = true } } }) { Text("Back to Home") }
                }
            }
        }
    }
}

@Composable
fun CustomerOrdersScreen(orders: List<Order>) {
    val uid = FirebaseService.getCurrentUser()?.uid
    val myOrders = orders.filter { it.userId == uid }.reversed()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Orders", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            items(myOrders) { order ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.id, fontWeight = FontWeight.Bold, color = Cream)
                            Text(order.status.uppercase(), color = Saffron, fontWeight = FontWeight.Bold)
                        }
                        Text(order.items.joinToString(", "), fontSize = 14.sp, color = Cream)
                        Text("Total: ₹${order.total}", fontWeight = FontWeight.Bold, color = Gold)
                        Text(order.date, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(isOwner: Boolean = false, onLogout: () -> Unit) {
    var activeDialog by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (activeDialog != null) {
        AlertDialog(onDismissRequest = { activeDialog = null }, confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close") } },
            title = { Text(activeDialog!!, color = Saffron) }, text = { Text("Details for $activeDialog", color = Cream) }, containerColor = CardBg)
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(100.dp), shape = CircleShape, color = Saffron) { Box(contentAlignment = Alignment.Center) { Text(if(isOwner) "MA" else "CU", color = Color.White, fontSize = 32.sp) } }
        Text(FirebaseService.getCurrentUser()?.email ?: "Unknown", fontSize = 16.sp, color = Gold)
        Text(if(isOwner) "Manager Role" else "Customer Role", fontSize = 14.sp, color = Cream, modifier = Modifier.alpha(0.7f))
        Spacer(Modifier.height(40.dp))
        
        if (isOwner) {
            Button(onClick = { scope.launch { runSafely(context) { FirebaseService.seedDemoData(); Toast.makeText(context, "Menu Seeded!", Toast.LENGTH_SHORT).show() } } }, modifier = Modifier.fillMaxWidth()) {
                Text("Seed Demo Menu")
            }
            Spacer(Modifier.height(16.dp))
        }

        listOf("Payment Methods", "Saved Addresses", "Settings", "Help & Support").forEach { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { activeDialog = item }, colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Row(Modifier.padding(16.dp)) { Text(item, color = Cream, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Cream) }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}

// --- OWNER SCREENS ---

@Composable
fun OwnerDashboard(navController: NavHostController, orders: List<Order>, menuItems: List<Dish>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Dashboard", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Revenue", "₹${orders.filter { it.status == "delivered" }.sumOf { it.total }}", "💰", Modifier.weight(1f))
                StatCard("Total Orders", "${orders.size}", "📦", Modifier.weight(1f))
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard("Orders", "📋", { navController.navigate("owner_orders") }, Modifier.weight(1f))
                ActionCard("Menu", "📖", { navController.navigate("owner_menu") }, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOrdersScreen(orders: List<Order>, onUpdateStatus: (String, String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Active Orders", fontSize = 32.sp, color = Saffron, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            items(orders.filter { it.status != "delivered" }.reversed()) { order ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.id, fontWeight = FontWeight.Bold, color = Cream)
                            Text(order.status.uppercase(), color = Saffron, fontWeight = FontWeight.Bold)
                        }
                        Text(order.customerName, fontSize = 12.sp, color = Color.Gray)
                        Text(order.items.joinToString(", "), modifier = Modifier.padding(vertical = 8.dp), color = Cream)
                        Row {
                            if(order.status == "pending") {
                                Button(onClick = { onUpdateStatus(order.id, "preparing") }, modifier = Modifier.weight(1f)) { Text("Accept") }
                            } else if(order.status == "preparing") {
                                Button(onClick = { onUpdateStatus(order.id, "delivered") }, modifier = Modifier.weight(1f)) { Text("Mark Delivered") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerMenuScreen(menuData: SnapshotStateList<Dish>) {
    var editingDish by remember { mutableStateOf<Dish?>(null) }
    var showingAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (editingDish != null) EditDishDialog(editingDish!!, { editingDish = null }, { updated ->
        scope.launch { runSafely(context) { FirebaseService.updateDish(updated) }; editingDish = null }
    })
    if (showingAddDialog) AddDishDialog(menuData) { showingAddDialog = false }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Menu Editor", fontSize = 32.sp, color = Saffron, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            IconButton(onClick = { showingAddDialog = true }) { Icon(Icons.Default.AddCircle, null, tint = Saffron, modifier = Modifier.size(32.dp)) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            items(menuData) { dish ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(dish.emoji, fontSize = 24.sp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(dish.name, fontWeight = FontWeight.Bold, color = Cream)
                            Text("₹${dish.price}", color = Saffron, fontSize = 14.sp)
                        }
                        IconButton(onClick = { editingDish = dish }) { Icon(Icons.Default.Edit, null, tint = Gold) }
                        IconButton(onClick = { scope.launch { runSafely(context) { FirebaseService.deleteDish(dish.id) } } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun AddDishDialog(menuData: SnapshotStateList<Dish>, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🍛") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {
        Button(onClick = {
            scope.launch {
                val new = Dish("", name.trim(), emoji, "main", true, "", price.toIntOrNull() ?: 0)
                runSafely(context) { FirebaseService.addDish(new) }; onDismiss()
            }
        }, enabled = name.isNotBlank() && price.isNotBlank()) { Text("Add Dish") }
    }, title = { Text("New Dish") }, text = {
        Column { 
            OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, modifier = Modifier.width(80.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Dish Name") })
            OutlinedTextField(price, { price = it }, label = { Text("Price (₹)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)) 
        }
    }, containerColor = CardBg)
}

@Composable
fun EditDishDialog(dish: Dish, onDismiss: () -> Unit, onSave: (Dish) -> Unit) {
    var name by remember { mutableStateOf(dish.name) }
    var price by remember { mutableStateOf(dish.price.toString()) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {
        Button(onClick = { onSave(dish.copy(name = name, price = price.toIntOrNull() ?: dish.price)) }) { Text("Save Changes") }
    }, title = { Text("Edit Dish") }, text = {
        Column { OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(price, { price = it }, label = { Text("Price") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)) }
    }, containerColor = CardBg)
}

@Composable
fun StatCard(title: String, value: String, icon: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 32.sp); Text(value, fontWeight = FontWeight.Bold, color = Saffron, fontSize = 18.sp); Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionCard(title: String, icon: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp); Text(title, fontWeight = FontWeight.Bold, color = Cream)
        }
    }
}

@Composable
fun DishCard(dish: Dish, qty: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.width(200.dp).padding(end = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dish.emoji, fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(dish.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, color = Cream)
            Text("₹${dish.price}", color = Saffron, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAdd, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { 
                Text(if(qty > 0) "Add More ($qty)" else "Add to Cart", fontSize = 12.sp) 
            }
        }
    }
}

@Composable
fun HorizontalDishItem(dish: Dish, qty: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dish.emoji, fontSize = 32.sp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(dish.name, fontWeight = FontWeight.Bold, color = Cream)
                Text("₹${dish.price}", color = Saffron, fontSize = 14.sp)
            }
            if(qty > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Remove, null, tint = Saffron) }
                    Text("$qty", fontWeight = FontWeight.Bold, color = Cream)
                    IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = Saffron) }
                }
            } else {
                Button(onClick = onAdd, shape = RoundedCornerShape(8.dp)) { Text("Add") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBottomNav(navController: NavHostController, cartCount: Int) {
    NavigationBar(containerColor = DeepBg) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val route = navBackStackEntry?.destination?.route
        listOf(Triple("home", "Home", Icons.Default.Home), Triple("menu", "Menu", Icons.Default.MenuBook), Triple("cart", "Cart", Icons.Default.ShoppingCart), Triple("orders", "Orders", Icons.Default.List), Triple("profile", "Profile", Icons.Default.Person)).forEach { (r, l, i) ->
            NavigationBarItem(
                selected = route == r, 
                onClick = { navController.navigate(r) { popUpTo("home"); launchSingleTop = true; restoreState = true } },
                icon = { 
                    BadgedBox(badge = { if(r == "cart" && cartCount > 0) Badge { Text("$cartCount") } }) { 
                        Icon(i, l) 
                    } 
                }, 
                label = { Text(l, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Saffron, unselectedIconColor = Color.Gray, selectedTextColor = Saffron)
            )
        }
    }
}

@Composable
fun OwnerBottomNav(navController: NavHostController) {
    NavigationBar(containerColor = DeepBg) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val route = navBackStackEntry?.destination?.route
        listOf(Triple("dashboard", "Dashboard", Icons.Default.Dashboard), Triple("owner_orders", "Orders", Icons.Default.Receipt), Triple("owner_menu", "Menu", Icons.Default.RestaurantMenu), Triple("profile", "Profile", Icons.Default.Person)).forEach { (r, l, i) ->
            NavigationBarItem(
                selected = route == r, 
                onClick = { navController.navigate(r) { launchSingleTop = true } }, 
                icon = { Icon(i, l) }, 
                label = { Text(l) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Saffron, unselectedIconColor = Color.Gray, selectedTextColor = Saffron)
            )
        }
    }
}

@Composable
fun MayuriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Saffron, secondary = Gold, background = DeepBg, surface = CardBg), content = content)
}
