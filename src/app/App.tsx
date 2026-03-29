import { useState } from 'react';
import './mayuri-styles.css';

// Menu Data
const menuData = [
  // Starters
  { id: 1, name: 'Paneer Tikka', emoji: '🧀', category: 'starters', veg: true, desc: 'Grilled cottage cheese with aromatic spices', price: 220, special: true },
  { id: 2, name: 'Chicken 65', emoji: '🍗', category: 'starters', veg: false, desc: 'Spicy deep-fried chicken with curry leaves', price: 280, special: false },
  { id: 3, name: 'Samosa', emoji: '🥟', category: 'starters', veg: true, desc: 'Crispy pastry filled with spiced potatoes', price: 120, special: false },
  { id: 4, name: 'Seekh Kebab', emoji: '🍢', category: 'starters', veg: false, desc: 'Minced lamb grilled on skewers', price: 320, special: true },
  { id: 5, name: 'Hara Bhara Kabab', emoji: '🥬', category: 'starters', veg: true, desc: 'Spinach and peas patties with herbs', price: 180, special: false },
  { id: 6, name: 'Fish Amritsari', emoji: '🐟', category: 'starters', veg: false, desc: 'Crispy batter-fried fish with tangy spices', price: 340, special: true },
  
  // Main Course
  { id: 7, name: 'Dal Makhani', emoji: '🍲', category: 'main', veg: true, desc: 'Creamy black lentils slow-cooked with butter', price: 260, popular: true },
  { id: 8, name: 'Butter Chicken', emoji: '🍛', category: 'main', veg: false, desc: 'Tender chicken in rich tomato cream sauce', price: 340, popular: true, special: true },
  { id: 9, name: 'Palak Paneer', emoji: '🥬', category: 'main', veg: true, desc: 'Cottage cheese in smooth spinach gravy', price: 280, popular: true },
  { id: 10, name: 'Mutton Rogan Josh', emoji: '🍖', category: 'main', veg: false, desc: 'Kashmiri mutton curry with aromatic spices', price: 420, special: true },
  { id: 11, name: 'Veg Dum Biryani', emoji: '🍚', category: 'main', veg: true, desc: 'Fragrant basmati rice with mixed vegetables', price: 300, popular: true },
  { id: 12, name: 'Hyderabadi Chicken Biryani', emoji: '🍗', category: 'main', veg: false, desc: 'Aromatic rice layered with spiced chicken', price: 380, popular: true, special: true },
  
  // Breads
  { id: 13, name: 'Butter Naan', emoji: '🫓', category: 'breads', veg: true, desc: 'Soft leavened bread brushed with butter', price: 60, popular: true },
  { id: 14, name: 'Garlic Kulcha', emoji: '🧄', category: 'breads', veg: true, desc: 'Fluffy bread stuffed with garlic', price: 70, popular: true },
  { id: 15, name: 'Laccha Paratha', emoji: '🥞', category: 'breads', veg: true, desc: 'Layered whole wheat flatbread', price: 65, popular: false },
  { id: 16, name: 'Tandoori Roti', emoji: '🫓', category: 'breads', veg: true, desc: 'Whole wheat bread from clay oven', price: 45, popular: false },
  
  // Desserts
  { id: 17, name: 'Gulab Jamun', emoji: '🍡', category: 'desserts', veg: true, desc: 'Soft milk dumplings in rose syrup', price: 100, popular: true },
  { id: 18, name: 'Kheer', emoji: '🍚', category: 'desserts', veg: true, desc: 'Creamy rice pudding with cardamom', price: 120, popular: false },
  { id: 19, name: 'Mango Kulfi', emoji: '🥭', category: 'desserts', veg: true, desc: 'Indian ice cream with fresh mango', price: 130, popular: true, special: true },
  { id: 20, name: 'Gajar Ka Halwa', emoji: '🥕', category: 'desserts', veg: true, desc: 'Sweet carrot pudding with nuts', price: 110, popular: false },
  
  // Drinks
  { id: 21, name: 'Mango Lassi', emoji: '🥤', category: 'drinks', veg: true, desc: 'Chilled yogurt drink with mango pulp', price: 90, popular: true },
  { id: 22, name: 'Masala Chai', emoji: '☕', category: 'drinks', veg: true, desc: 'Spiced Indian tea with milk', price: 60, popular: true },
  { id: 23, name: 'Fresh Lime Soda', emoji: '🍋', category: 'drinks', veg: true, desc: 'Refreshing lime with soda water', price: 70, popular: false },
  { id: 24, name: 'Rose Sharbat', emoji: '🌹', category: 'drinks', veg: true, desc: 'Sweet rose-flavored drink', price: 80, popular: false },
];

type Dish = typeof menuData[0];
type Cart = Record<number, number>;
type UserRole = 'user' | 'owner' | null;

interface Order {
  id: string;
  date: string;
  status: 'pending' | 'preparing' | 'delivering' | 'delivered';
  items: string[];
  total: number;
  customerName: string;
  address: string;
}

export default function App() {
  const [userRole, setUserRole] = useState<UserRole>(null);
  const [currentScreen, setCurrentScreen] = useState<'home' | 'menu' | 'cart' | 'payment' | 'orders' | 'profile' | 'owner-dashboard' | 'owner-orders' | 'owner-menu'>('home');
  const [cart, setCart] = useState<Cart>({});
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [processingPayment, setProcessingPayment] = useState(false);
  const [allOrders, setAllOrders] = useState<Order[]>([
    { id: 'ORD9876543', date: 'Mar 21, 2026 - 2:30 PM', status: 'pending', items: ['Dal Makhani', 'Butter Naan', 'Gulab Jamun'], total: 420, customerName: 'Priya Patel', address: '45 Brigade Road, Bangalore' },
    { id: 'ORD9876542', date: 'Mar 21, 2026 - 2:15 PM', status: 'preparing', items: ['Butter Chicken', 'Garlic Kulcha', 'Mango Lassi'], total: 500, customerName: 'Rahul Kumar', address: '78 Koramangala, Bangalore' },
    { id: 'ORD9876541', date: 'Mar 21, 2026 - 1:45 PM', status: 'delivering', items: ['Chicken Biryani', 'Raita'], total: 380, customerName: 'Anita Desai', address: '12 Indiranagar, Bangalore' },
    { id: 'ORD9876540', date: 'Mar 21, 2026 - 1:30 PM', status: 'delivered', items: ['Paneer Tikka', 'Laccha Paratha'], total: 285, customerName: 'Vikram Singh', address: '90 MG Road, Bangalore' },
  ]);
  const [orderHistory] = useState<Order[]>([
    { id: 'ORD1234567', date: 'Mar 20, 2026', status: 'delivered', items: ['Butter Chicken', 'Garlic Kulcha', 'Mango Lassi'], total: 490, customerName: 'Arjun Sharma', address: '123 MG Road, Bangalore' },
    { id: 'ORD1234566', date: 'Mar 18, 2026', status: 'delivered', items: ['Paneer Tikka', 'Dal Makhani', 'Butter Naan'], total: 540, customerName: 'Arjun Sharma', address: '123 MG Road, Bangalore' },
    { id: 'ORD1234565', date: 'Mar 15, 2026', status: 'delivered', items: ['Chicken Biryani', 'Gulab Jamun'], total: 480, customerName: 'Arjun Sharma', address: '123 MG Road, Bangalore' }
  ]);

  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');

  // Demo credentials
  const userCredentials = { email: 'user@mayuri.com', password: 'user123' };
  const ownerCredentials = { email: 'owner@mayuri.com', password: 'owner123' };

  const handleLogin = () => {
    if (loginEmail === ownerCredentials.email && loginPassword === ownerCredentials.password) {
      setUserRole('owner');
      setCurrentScreen('owner-dashboard');
    } else if (loginEmail === userCredentials.email && loginPassword === userCredentials.password) {
      setUserRole('user');
      setCurrentScreen('home');
    } else {
      alert('Invalid credentials!\n\nDemo accounts:\nUser: user@mayuri.com / user123\nOwner: owner@mayuri.com / owner123');
    }
  };

  const handleLogout = () => {
    setUserRole(null);
    setLoginEmail('');
    setLoginPassword('');
    setCart({});
    setCurrentScreen('home');
  };

  const updateOrderStatus = (orderId: string, newStatus: Order['status']) => {
    setAllOrders(prev => prev.map(order => 
      order.id === orderId ? { ...order, status: newStatus } : order
    ));
  };

  const addToCart = (dishId: number) => {
    setCart(prev => ({ ...prev, [dishId]: (prev[dishId] || 0) + 1 }));
  };

  const removeFromCart = (dishId: number) => {
    setCart(prev => {
      const newCart = { ...prev };
      if (newCart[dishId] > 1) {
        newCart[dishId]--;
      } else {
        delete newCart[dishId];
      }
      return newCart;
    });
  };

  const getCartCount = () => Object.values(cart).reduce((sum, qty) => sum + qty, 0);

  const getCartItems = () => {
    return Object.entries(cart).map(([id, qty]) => ({
      dish: menuData.find(d => d.id === Number(id))!,
      quantity: qty
    })).filter(item => item.dish);
  };

  const calculateBill = () => {
    const items = getCartItems();
    const subtotal = items.reduce((sum, item) => sum + (item.dish.price * item.quantity), 0);
    const gst = Math.round(subtotal * 0.05);
    const delivery = subtotal > 0 ? 40 : 0;
    const total = subtotal + gst + delivery;
    return { subtotal, gst, delivery, total };
  };

  const placeOrder = () => {
    if (!selectedPaymentMethod) {
      alert('Please select a payment method');
      return;
    }
    setProcessingPayment(true);
    setTimeout(() => {
      setProcessingPayment(false);
      setShowSuccess(true);
    }, 2000);
  };

  const completeOrder = () => {
    setCart({});
    setShowSuccess(false);
    setSelectedPaymentMethod(null);
    setCurrentScreen('orders');
  };

  const filteredDishes = menuData.filter(dish => {
    const categoryMatch = selectedCategory === 'all' || dish.category === selectedCategory;
    const searchMatch = dish.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                       dish.desc.toLowerCase().includes(searchTerm.toLowerCase());
    return categoryMatch && searchMatch;
  });

  const cartCount = getCartCount();
  const { subtotal, gst, delivery, total } = calculateBill();
  const cartItems = getCartItems();

  // Calculate owner dashboard stats
  const todayRevenue = allOrders.reduce((sum, order) => sum + order.total, 0);
  const todayOrders = allOrders.length;
  const pendingOrders = allOrders.filter(o => o.status === 'pending' || o.status === 'preparing').length;

  // LOGIN SCREEN
  if (!userRole) {
    return (
      <div className="mayuri-app">
        <div className="login-screen">
          <div className="login-container">
            <div className="login-header">
              <div className="login-logo">🍛</div>
              <h1 className="login-title">MAYURI</h1>
              <p className="login-subtitle">Authentic Indian Cuisine</p>
            </div>

            <div className="login-form">
              <div className="form-group">
                <label className="form-label-login">Email Address</label>
                <input
                  type="email"
                  className="form-input-login"
                  placeholder="Enter your email"
                  value={loginEmail}
                  onChange={(e) => setLoginEmail(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleLogin()}
                />
              </div>

              <div className="form-group">
                <label className="form-label-login">Password</label>
                <input
                  type="password"
                  className="form-input-login"
                  placeholder="Enter your password"
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleLogin()}
                />
              </div>

              <button className="login-btn" onClick={handleLogin}>
                Sign In
              </button>

              <div className="demo-accounts">
                <p className="demo-title">Demo Accounts:</p>
                <div className="demo-account-card" onClick={() => { setLoginEmail('user@mayuri.com'); setLoginPassword('user123'); }}>
                  <div className="demo-icon">👤</div>
                  <div className="demo-details">
                    <div className="demo-role">Customer Login</div>
                    <div className="demo-email">user@mayuri.com</div>
                  </div>
                </div>
                <div className="demo-account-card" onClick={() => { setLoginEmail('owner@mayuri.com'); setLoginPassword('owner123'); }}>
                  <div className="demo-icon">👨‍💼</div>
                  <div className="demo-details">
                    <div className="demo-role">Owner Dashboard</div>
                    <div className="demo-email">owner@mayuri.com</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // OWNER DASHBOARD
  if (userRole === 'owner') {
    return (
      <div className="mayuri-app owner-mode">
        {/* Owner Dashboard Screen */}
        <div className={`mayuri-screen ${currentScreen === 'owner-dashboard' ? 'active' : 'hidden'}`}>
          <div className="owner-header">
            <div className="owner-header-top">
              <div>
                <h1 className="title">Owner Dashboard</h1>
                <div className="subtitle">Welcome back, Admin</div>
              </div>
              <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
          </div>

          <div className="stats-grid">
            <div className="stat-card-owner">
              <div className="stat-icon">💰</div>
              <div className="stat-value">₹{todayRevenue}</div>
              <div className="stat-label">Today's Revenue</div>
            </div>
            <div className="stat-card-owner">
              <div className="stat-icon">📦</div>
              <div className="stat-value">{todayOrders}</div>
              <div className="stat-label">Total Orders</div>
            </div>
            <div className="stat-card-owner">
              <div className="stat-icon">⏳</div>
              <div className="stat-value">{pendingOrders}</div>
              <div className="stat-label">Pending Orders</div>
            </div>
            <div className="stat-card-owner">
              <div className="stat-icon">📊</div>
              <div className="stat-value">24</div>
              <div className="stat-label">Menu Items</div>
            </div>
          </div>

          <div className="section">
            <div className="section-header">
              <h2 className="section-title">Recent Orders</h2>
              <div className="view-all" onClick={() => setCurrentScreen('owner-orders')}>View All →</div>
            </div>
            <div className="owner-orders-list">
              {allOrders.slice(0, 3).map(order => (
                <div key={order.id} className="owner-order-card">
                  <div className="order-header">
                    <div className="order-info">
                      <h4>{order.id}</h4>
                      <div className="order-date">{order.date}</div>
                      <div className="order-customer">👤 {order.customerName}</div>
                    </div>
                    <div className={`order-status ${order.status}`}>
                      {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
                    </div>
                  </div>
                  
                  <div className="order-items">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="item-chip">{item}</div>
                    ))}
                  </div>
                  
                  <div className="order-footer">
                    <div className="order-total">₹{order.total}</div>
                    <div className="order-actions-quick">
                      {order.status === 'pending' && (
                        <button className="action-btn accept" onClick={() => updateOrderStatus(order.id, 'preparing')}>
                          Accept
                        </button>
                      )}
                      {order.status === 'preparing' && (
                        <button className="action-btn dispatch" onClick={() => updateOrderStatus(order.id, 'delivering')}>
                          Dispatch
                        </button>
                      )}
                      {order.status === 'delivering' && (
                        <button className="action-btn complete" onClick={() => updateOrderStatus(order.id, 'delivered')}>
                          Complete
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="section">
            <div className="section-header">
              <h2 className="section-title">Quick Actions</h2>
            </div>
            <div className="quick-actions">
              <div className="action-card" onClick={() => setCurrentScreen('owner-orders')}>
                <div className="action-icon">📋</div>
                <div className="action-text">Manage Orders</div>
              </div>
              <div className="action-card" onClick={() => setCurrentScreen('owner-menu')}>
                <div className="action-icon">📖</div>
                <div className="action-text">Edit Menu</div>
              </div>
            </div>
          </div>
        </div>

        {/* Owner Orders Management Screen */}
        <div className={`mayuri-screen ${currentScreen === 'owner-orders' ? 'active' : 'hidden'}`}>
          <div className="owner-header">
            <div className="owner-header-top">
              <div>
                <h1 className="title">All Orders</h1>
                <div className="subtitle">Manage incoming orders</div>
              </div>
              <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
          </div>

          <div className="owner-orders-list">
            {allOrders.map(order => (
              <div key={order.id} className="owner-order-card">
                <div className="order-header">
                  <div className="order-info">
                    <h4>{order.id}</h4>
                    <div className="order-date">{order.date}</div>
                    <div className="order-customer">👤 {order.customerName}</div>
                    <div className="order-address">📍 {order.address}</div>
                  </div>
                  <div className={`order-status ${order.status}`}>
                    {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
                  </div>
                </div>
                
                <div className="order-items">
                  {order.items.map((item, idx) => (
                    <div key={idx} className="item-chip">{item}</div>
                  ))}
                </div>
                
                <div className="order-footer">
                  <div className="order-total">₹{order.total}</div>
                  <div className="order-actions-buttons">
                    {order.status === 'pending' && (
                      <button className="action-btn accept" onClick={() => updateOrderStatus(order.id, 'preparing')}>
                        ✓ Accept Order
                      </button>
                    )}
                    {order.status === 'preparing' && (
                      <button className="action-btn dispatch" onClick={() => updateOrderStatus(order.id, 'delivering')}>
                        🚚 Dispatch
                      </button>
                    )}
                    {order.status === 'delivering' && (
                      <button className="action-btn complete" onClick={() => updateOrderStatus(order.id, 'delivered')}>
                        ✓ Mark Delivered
                      </button>
                    )}
                    {order.status === 'delivered' && (
                      <div className="action-btn delivered">✓ Delivered</div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Owner Menu Management Screen */}
        <div className={`mayuri-screen ${currentScreen === 'owner-menu' ? 'active' : 'hidden'}`}>
          <div className="owner-header">
            <div className="owner-header-top">
              <div>
                <h1 className="title">Menu Management</h1>
                <div className="subtitle">Edit dishes and pricing</div>
              </div>
              <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
          </div>

          <div className="menu-list">
            {menuData.map(dish => (
              <div key={dish.id} className="menu-item owner-menu-item">
                <div className="menu-emoji">{dish.emoji}</div>
                <div className="menu-details">
                  <div className="menu-header">
                    <div className="menu-name">{dish.name}</div>
                    <div className={`chef-card-badge ${dish.veg ? '' : 'non-veg'}`}>
                      {dish.veg ? '🟢' : '🔴'}
                    </div>
                  </div>
                  <div className="menu-desc">{dish.desc}</div>
                  <div className="menu-footer">
                    <div className="price">₹{dish.price}</div>
                    <button className="edit-menu-btn">Edit</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Owner Bottom Navigation */}
        <div className="bottom-nav owner-nav">
          <div 
            className={`nav-item ${currentScreen === 'owner-dashboard' ? 'active' : ''}`}
            onClick={() => setCurrentScreen('owner-dashboard')}
          >
            <div className="nav-icon">📊</div>
            <div className="nav-label">Dashboard</div>
          </div>
          <div 
            className={`nav-item ${currentScreen === 'owner-orders' ? 'active' : ''}`}
            onClick={() => setCurrentScreen('owner-orders')}
          >
            <div className="nav-icon">📦</div>
            <div className="nav-label">Orders</div>
            {pendingOrders > 0 && <div className="nav-badge">{pendingOrders}</div>}
          </div>
          <div 
            className={`nav-item ${currentScreen === 'owner-menu' ? 'active' : ''}`}
            onClick={() => setCurrentScreen('owner-menu')}
          >
            <div className="nav-icon">📖</div>
            <div className="nav-label">Menu</div>
          </div>
        </div>
      </div>
    );
  }

  // USER APP (existing code continues)
  return (
    <div className="mayuri-app">
      {/* Home Screen */}
      <div className={`mayuri-screen ${currentScreen === 'home' ? 'active' : 'hidden'}`}>
        <div className="mayuri-header">
          <div className="greeting">Good Evening 👋</div>
          <h1 className="title">MAYURI</h1>
          <div className="subtitle">Authentic Indian Cuisine</div>
        </div>
        
        <div className="promo-banner">
          <h3>🎉 Get 20% OFF</h3>
          <p>On orders above ₹500 • Use code: WELCOME20</p>
        </div>
        
        <div className="category-scroll">
          {['all', 'starters', 'main', 'breads', 'desserts', 'drinks'].map(cat => (
            <div 
              key={cat}
              className={`category-pill ${selectedCategory === cat ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat === 'all' ? 'All' : cat === 'main' ? 'Main Course' : cat.charAt(0).toUpperCase() + cat.slice(1)}
            </div>
          ))}
        </div>
        
        <div className="section">
          <div className="section-header">
            <h2 className="section-title">Chef's Special</h2>
            <div className="view-all" onClick={() => setCurrentScreen('menu')}>View All →</div>
          </div>
          <div className="horizontal-scroll">
            {menuData.filter(d => d.special).map(dish => (
              <div key={dish.id} className="chef-card">
                <div className="chef-card-header">
                  <div className="chef-card-emoji">{dish.emoji}</div>
                  <div className={`chef-card-badge ${dish.veg ? '' : 'non-veg'}`}>
                    {dish.veg ? '🟢 VEG' : '🔴 NON-VEG'}
                  </div>
                </div>
                <h4>{dish.name}</h4>
                <p>{dish.desc}</p>
                <div className="chef-card-footer">
                  <div className="price">₹{dish.price}</div>
                  {cart[dish.id] ? (
                    <div className="qty-control">
                      <button className="qty-btn" onClick={() => removeFromCart(dish.id)}>−</button>
                      <div className="qty-value">{cart[dish.id]}</div>
                      <button className="qty-btn" onClick={() => addToCart(dish.id)}>+</button>
                    </div>
                  ) : (
                    <button className="add-btn" onClick={() => addToCart(dish.id)}>Add</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
        
        <div className="section">
          <div className="section-header">
            <h2 className="section-title">Popular Picks</h2>
          </div>
          <div className="popular-list">
            {menuData.filter(d => d.popular).slice(0, 6).map(dish => (
              <div key={dish.id} className="popular-item">
                <div className="popular-emoji">{dish.emoji}</div>
                <div className="popular-info">
                  <h4>{dish.name}</h4>
                  <div className="price">₹{dish.price}</div>
                </div>
                {cart[dish.id] ? (
                  <div className="qty-control">
                    <button className="qty-btn" onClick={() => removeFromCart(dish.id)}>−</button>
                    <div className="qty-value">{cart[dish.id]}</div>
                    <button className="qty-btn" onClick={() => addToCart(dish.id)}>+</button>
                  </div>
                ) : (
                  <button className="add-btn" onClick={() => addToCart(dish.id)}>Add</button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Menu Screen */}
      <div className={`mayuri-screen ${currentScreen === 'menu' ? 'active' : 'hidden'}`}>
        <div className="mayuri-header">
          <h1 className="title">Menu</h1>
          <div className="subtitle">Explore our delicious dishes</div>
        </div>
        
        <div className="search-bar">
          <input 
            type="text" 
            className="search-input" 
            placeholder="🔍 Search dishes..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        
        <div className="category-scroll">
          {['all', 'starters', 'main', 'breads', 'desserts', 'drinks'].map(cat => (
            <div 
              key={cat}
              className={`category-pill ${selectedCategory === cat ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat === 'all' ? 'All' : cat === 'main' ? 'Main Course' : cat.charAt(0).toUpperCase() + cat.slice(1)}
            </div>
          ))}
        </div>
        
        <div className="menu-list">
          {filteredDishes.map(dish => (
            <div key={dish.id} className="menu-item">
              <div className="menu-emoji">{dish.emoji}</div>
              <div className="menu-details">
                <div className="menu-header">
                  <div className="menu-name">{dish.name}</div>
                  <div className={`chef-card-badge ${dish.veg ? '' : 'non-veg'}`}>
                    {dish.veg ? '🟢' : '🔴'}
                  </div>
                </div>
                <div className="menu-desc">{dish.desc}</div>
                <div className="menu-footer">
                  <div className="price">₹{dish.price}</div>
                  {cart[dish.id] ? (
                    <div className="qty-control">
                      <button className="qty-btn" onClick={() => removeFromCart(dish.id)}>−</button>
                      <div className="qty-value">{cart[dish.id]}</div>
                      <button className="qty-btn" onClick={() => addToCart(dish.id)}>+</button>
                    </div>
                  ) : (
                    <button className="add-btn" onClick={() => addToCart(dish.id)}>Add</button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Cart Screen */}
      <div className={`mayuri-screen ${currentScreen === 'cart' ? 'active' : 'hidden'}`}>
        <div className="mayuri-header">
          <h1 className="title">Your Cart</h1>
          <div className="subtitle">Review your items</div>
        </div>
        
        {cartItems.length === 0 ? (
          <div className="cart-empty">
            <div className="cart-empty-emoji">🛒</div>
            <h3>Your cart is empty</h3>
            <p>Add some delicious items to get started</p>
          </div>
        ) : (
          <>
            <div className="cart-list">
              {cartItems.map(item => (
                <div key={item.dish.id} className="cart-item">
                  <div className="cart-emoji">{item.dish.emoji}</div>
                  <div className="cart-details">
                    <div className="cart-name">{item.dish.name}</div>
                    <div className="cart-price">₹{item.dish.price} × {item.quantity}</div>
                  </div>
                  <div className="cart-actions">
                    <div className="qty-control">
                      <button className="qty-btn" onClick={() => removeFromCart(item.dish.id)}>−</button>
                      <div className="qty-value">{item.quantity}</div>
                      <button className="qty-btn" onClick={() => addToCart(item.dish.id)}>+</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            
            <div className="promo-section">
              <div className="promo-input-wrapper">
                <input type="text" className="promo-input" placeholder="PROMO CODE" />
                <button className="apply-btn">Apply</button>
              </div>
            </div>
            
            <div className="bill-section">
              <div className="bill-card">
                <div className="bill-row">
                  <span>Subtotal</span>
                  <span className="value">₹{subtotal}</span>
                </div>
                <div className="bill-row">
                  <span>GST (5%)</span>
                  <span className="value">₹{gst}</span>
                </div>
                <div className="bill-row">
                  <span>Delivery Fee</span>
                  <span className="value">₹{delivery}</span>
                </div>
                <div className="bill-row total">
                  <span>Grand Total</span>
                  <span className="value">₹{total}</span>
                </div>
              </div>
            </div>
            
            <div className="checkout-bar">
              <button className="checkout-btn" onClick={() => setCurrentScreen('payment')}>
                Proceed to Checkout • ₹{total}
              </button>
            </div>
          </>
        )}
      </div>

      {/* Payment Screen */}
      <div className={`mayuri-screen ${currentScreen === 'payment' ? 'active' : 'hidden'}`}>
        <div className="mayuri-header">
          <h1 className="title">Checkout</h1>
          <div className="subtitle">Complete your order</div>
        </div>
        
        {processingPayment ? (
          <div className="processing">
            <div className="spinner"></div>
            <h3>Processing Payment...</h3>
            <p>Please wait while we confirm your order</p>
          </div>
        ) : showSuccess ? (
          <div className="success-container">
            <div className="success-icon">✓</div>
            <h2>Order Placed Successfully!</h2>
            <p>Thank you for your order</p>
            <div className="order-id">Order ID: ORD{Math.floor(Math.random() * 10000000)}</div>
            
            <div className="tracker">
              <div className="tracker-step active">
                <div className="tracker-dot"></div>
                <div className="tracker-label">Order Placed</div>
              </div>
              <div className="tracker-step active">
                <div className="tracker-dot"></div>
                <div className="tracker-label">Preparing</div>
              </div>
              <div className="tracker-step">
                <div className="tracker-dot"></div>
                <div className="tracker-label">Out for Delivery</div>
              </div>
              <div className="tracker-step">
                <div className="tracker-dot"></div>
                <div className="tracker-label">Delivered</div>
              </div>
            </div>
            
            <p style={{ marginBottom: '24px', opacity: 0.9 }}>Estimated delivery in 30-40 minutes</p>
            
            <button className="home-btn" onClick={completeOrder}>Back to Home</button>
          </div>
        ) : (
          <>
            <div className="summary-card">
              <div className="summary-title">Order Summary</div>
              <div className="summary-row">
                <span>Total Items</span>
                <span className="value">{cartCount}</span>
              </div>
              <div className="summary-row">
                <span>Amount to Pay</span>
                <span className="value">₹{total}</span>
              </div>
            </div>
            
            <div className="section-header" style={{ paddingTop: '8px' }}>
              <h2 className="section-title">Payment Method</h2>
            </div>
            
            <div className="payment-methods">
              {[
                { id: 'card', icon: '💳', label: 'Card' },
                { id: 'upi', icon: '📱', label: 'UPI' },
                { id: 'cod', icon: '💵', label: 'Cash on Delivery' },
                { id: 'wallet', icon: '👛', label: 'Wallet' }
              ].map(method => (
                <div 
                  key={method.id}
                  className={`payment-method ${selectedPaymentMethod === method.id ? 'selected' : ''}`}
                  onClick={() => setSelectedPaymentMethod(method.id)}
                >
                  <div className="payment-icon">{method.icon}</div>
                  <div className="payment-label">{method.label}</div>
                </div>
              ))}
            </div>
            
            {selectedPaymentMethod === 'card' && (
              <div className="payment-form">
                <div className="form-group">
                  <label className="form-label">Card Number</label>
                  <input type="text" className="form-input" placeholder="1234 5678 9012 3456" maxLength={19} />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Expiry Date</label>
                    <input type="text" className="form-input" placeholder="MM/YY" maxLength={5} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">CVV</label>
                    <input type="text" className="form-input" placeholder="123" maxLength={3} />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Cardholder Name</label>
                  <input type="text" className="form-input" placeholder="Enter name on card" />
                </div>
              </div>
            )}
            
            {selectedPaymentMethod === 'upi' && (
              <div className="payment-form">
                <div className="form-group">
                  <label className="form-label">UPI ID</label>
                  <input type="text" className="form-input" placeholder="yourname@upi" />
                </div>
              </div>
            )}
            
            {selectedPaymentMethod === 'wallet' && (
              <div className="payment-form">
                <div className="form-group">
                  <label className="form-label">Select Wallet</label>
                  <select className="form-input">
                    <option>Paytm</option>
                    <option>PhonePe</option>
                    <option>Google Pay</option>
                    <option>Amazon Pay</option>
                  </select>
                </div>
              </div>
            )}
            
            <div className="payment-form">
              <div className="form-group">
                <label className="form-label">Delivery Address</label>
                <input type="text" className="form-input" placeholder="Enter your delivery address" defaultValue="123 MG Road, Bangalore - 560001" />
              </div>
            </div>
            
            <div className="checkout-bar">
              <button className="checkout-btn" onClick={placeOrder}>
                Place Order & Pay ₹{total}
              </button>
            </div>
          </>
        )}
      </div>

      {/* Orders Screen */}
      <div className={`mayuri-screen ${currentScreen === 'orders' ? 'active' : 'hidden'}`}>
        <div className="mayuri-header">
          <h1 className="title">My Orders</h1>
          <div className="subtitle">Track your orders</div>
        </div>
        
        <div className="orders-list">
          {orderHistory.map(order => (
            <div key={order.id} className="order-card">
              <div className="order-header">
                <div className="order-info">
                  <h4>{order.id}</h4>
                  <div className="order-date">{order.date}</div>
                </div>
                <div className={`order-status ${order.status}`}>
                  {order.status === 'delivered' ? 'Delivered' : 'Preparing'}
                </div>
              </div>
              
              <div className="order-items">
                {order.items.map((item, idx) => (
                  <div key={idx} className="item-chip">{item}</div>
                ))}
              </div>
              
              <div className="order-footer">
                <div className="order-total">₹{order.total}</div>
                <button className="reorder-btn">Reorder</button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Profile Screen */}
      <div className={`mayuri-screen ${currentScreen === 'profile' ? 'active' : 'hidden'}`}>
        <div className="profile-header">
          <div className="avatar">AS</div>
          <h2 className="profile-name">Arjun Sharma</h2>
          <div className="profile-email">user@mayuri.com</div>
        </div>
        
        <div className="stats-row">
          <div className="stat-card">
            <div className="stat-value">24</div>
            <div className="stat-label">Orders</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">₹8.4K</div>
            <div className="stat-label">Spent</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">480</div>
            <div className="stat-label">Points</div>
          </div>
        </div>
        
        <div className="settings-menu">
          {[
            { icon: '📍', text: 'Saved Addresses' },
            { icon: '💳', text: 'Payment Methods' },
            { icon: '🎁', text: 'Rewards & Offers' },
            { icon: '⚙️', text: 'Settings' },
            { icon: '❓', text: 'Help & Support' }
          ].map((item, idx) => (
            <div key={idx} className="menu-item-profile">
              <div className="menu-icon">{item.icon}</div>
              <div className="menu-text">{item.text}</div>
              <div className="menu-arrow">›</div>
            </div>
          ))}
          <div className="menu-item-profile" onClick={handleLogout}>
            <div className="menu-icon">🚪</div>
            <div className="menu-text">Logout</div>
            <div className="menu-arrow">›</div>
          </div>
        </div>
      </div>

      {/* Bottom Navigation */}
      <div className="bottom-nav">
        {[
          { screen: 'home' as const, icon: '🏠', label: 'Home' },
          { screen: 'menu' as const, icon: '📖', label: 'Menu' },
          { screen: 'cart' as const, icon: '🛒', label: 'Cart' },
          { screen: 'orders' as const, icon: '📦', label: 'Orders' },
          { screen: 'profile' as const, icon: '👤', label: 'Profile' }
        ].map(nav => (
          <div 
            key={nav.screen}
            className={`nav-item ${currentScreen === nav.screen ? 'active' : ''}`}
            onClick={() => setCurrentScreen(nav.screen)}
          >
            <div className="nav-icon">{nav.icon}</div>
            <div className="nav-label">{nav.label}</div>
            {nav.screen === 'cart' && cartCount > 0 && (
              <div className="nav-badge">{cartCount}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
