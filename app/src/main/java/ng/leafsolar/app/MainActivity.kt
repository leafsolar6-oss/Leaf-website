package ng.leafsolar.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Brand = Color(0xFF3CA506)
private const val HOME = "https://leafsolar.ng/"
private const val WHATSAPP = "2347037561216"
private val TABS = listOf(
  Triple("Home", Icons.Default.Home, HOME),
  Triple("Shop", Icons.Default.Search, HOME+"?post_type=product"),
  Triple("Cart", Icons.Default.ShoppingCart, HOME+"cart/"),
  Triple("Account", Icons.Default.Person, HOME+"my-account/"),
  Triple("Track", Icons.Default.Place, HOME+"my-account/orders/")
)

class MainActivity : ComponentActivity() {
  private lateinit var web: WebView
  var cart = mutableIntStateOf(0)
  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    web = WebView(this).apply {
      layoutParams = ViewGroup.LayoutParams(-1,-1)
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.databaseEnabled = true
      settings.loadWithOverviewMode = true
      settings.useWideViewPort = true
      settings.mediaPlaybackRequiresUserGesture = false
      settings.setAppCacheEnabled(true)
      settings.cacheMode = WebSettings.LOAD_DEFAULT
      settings.databaseEnabled = true
      CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
      CookieManager.getInstance().setAcceptCookie(true)
      webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
          val u = r.url.toString()
          if (u.startsWith("tel:")||u.startsWith("mailto:")||u.startsWith("whatsapp:")||u.contains("wa.me")||u.contains("instagram.com")||u.contains("facebook.com")||u.contains("tiktok.com")||u.contains("youtube.com"))
            return try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))); true } catch(_:Exception){ true }
          return false
        }
        override fun onPageFinished(view: WebView?, url: String?) {
          (context as? MainActivity)?._offline?.value = false
          view?.evaluateJavascript(CART_JS, null)
        }
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
          if (request?.isForMainFrame == true) (context as? MainActivity)?.showOffline()
        }
      }
      addJavascriptInterface(WebAppInterface(), "Android")
      loadUrl(HOME)
    }
    setContent { App(web, offline, ::goOnline, ::isOnline) }
  }
  override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }

  private val _offline = mutableStateOf(false)
  val offline: State<Boolean> = _offline
  fun isOnline(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val n = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(n) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
  fun showOffline() { _offline.value = true }
  fun goOnline() { _offline.value = false; web.reload() }

  inner class WebAppInterface {
    @android.webkit.JavascriptInterface
    fun onCartCount(n: Int) { runOnUiThread { cart.intValue = n } }
  }
  companion object {
    val CART_JS = """(function(){
      function read(){var el=document.querySelector('.cart-count, .wc-block-mini-cart__badge, [data-cart-count], .badge, .count');
        if(el){var n=parseInt((el.textContent||'').replace(/[^0-9]/g,''));if(!isNaN(n))return n;}
        var links=document.querySelectorAll('a[href*="cart"], a[href*="checkout"]');
        for(var i=0;i<links.length;i++){var m=(links[i].textContent||'').match(/(\d+)/);if(m)return parseInt(m[1]);}
        try{var data=document.cookie.match(/woocommerce_items_in_cart=(\d+)/);if(data)return parseInt(data[1]);}catch(e){}
        return 0;}
      try{ Android.onCartCount(read()); }catch(e){}
    })();"""
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(web: WebView, offline: State<Boolean>, onRetry: () -> Unit, isOnline: () -> Boolean) {
  var loading by remember { mutableStateOf(true) }
  var progress by remember { mutableIntStateOf(0) }
  var tab by remember { mutableIntStateOf(0) }
  var pull by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }
  val cartCount = (web.context as MainActivity).cart.intValue
  web.webChromeClient = object : WebChromeClient() { override fun onProgressChanged(v: WebView?, p: Int) { progress = p; if (p >= 100) loading = false } }
  MaterialTheme(colorScheme = lightColorScheme(primary = Brand)) {
    Scaffold(
      topBar = {
        Surface(color = Brand, shadowElevation = 4.dp) {
          Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
              Text("🍃 Leaf Solar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Spacer(Modifier.weight(1f))
              IconButton(onClick = { tab=3; web.loadUrl(TABS[3].third) }) { Icon(Icons.Default.Person, "Account", tint = Color.White) }
            }
            Row(Modifier.padding(horizontal = 10.dp).padding(bottom = 8.dp).fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)).background(Color.White)
                .clickable { web.loadUrl(HOME+"?post_type=product&s=") },
                verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Search, "Search", tint = Brand, modifier = Modifier.padding(10.dp))
              Text("Search products…", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
          }
        }
      },
      bottomBar = {
        Surface(color = Color.White, tonalElevation = 8.dp) {
          Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            TABS.forEachIndexed { i,(label,icon,url) ->
              val sel=tab==i
              Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier=Modifier.weight(1f).clickable { tab=i; web.loadUrl(url) }.padding(vertical = 4.dp)) {
                BadgedBox(badge = { if (label=="Cart" && cartCount>0) Badge(containerColor=Brand){Text("$cartCount",color=Color.White,fontSize=8.sp)} }) {
                  Icon(icon,label,tint=if(sel)Brand else Color.Gray, modifier=Modifier.size(22.dp))
                }
                Text(label, fontSize=10.sp, color=if(sel)Brand else Color.Gray, fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal)
              }
            }
          }
        }
      }
    ) { pad ->
      Box(Modifier.padding(pad).fillMaxSize()) {
        PullRefresh(pull, { pull=false; web.reload() }) {
          AndroidView(factory = { web }, modifier = Modifier.fillMaxSize(),
            update = { if (pull) web.reload() })
        }
        if (loading && !offline.value) LinearProgressIndicator(progress = { progress/100f }, modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter), color = Color.White, trackColor = Color(0x33FFFFFF))
        if (offline.value) OfflineScreen(onRetry)
      }
    }
    if (showMenu) MenuDialog(onDismiss={showMenu=false}, onLink={ url -> showMenu=false; web.loadUrl(url) },
      onWhatsApp={ showMenu=false; val i=Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/$WHATSAPP?text=${java.net.URLEncoder.encode("Hello Leaf Solar","UTF-8")}")); web.context.startActivity(i) })
  }
}

@Composable private fun PullRefresh(refreshing:Boolean, onRefresh:()->Unit, content:@Composable ()->Unit) {
  Box(Modifier.fillMaxSize()) {
    content()
    if (refreshing) CircularProgressIndicator(strokeWidth=2.dp,modifier=Modifier.align(Alignment.TopCenter).padding(8.dp), color=Brand)
  }
}

@Composable private fun MenuDialog(onDismiss:()->Unit, onLink:(String)->Unit, onWhatsApp:()->Unit) {
  val cats = listOf(
    "Electronics" to "electronics","Fridges & Freezers" to "fridges-freezers","Air Conditioners" to "air-conditioners",
    "TVs" to "tvs","Kitchen & Cooking" to "kitchen-cooking","Fans & Coolers" to "fans-coolers",
    "Audio & Sound" to "audio-sound","Solar & Inverters" to "solar-inverters","Generators & Power" to "generators-power",
    "Washers & Dryers" to "washers-dryers","Accessories" to "accessories","Solar Panels" to "solar-panels")
  AlertDialog(onDismissRequest=onDismiss, confirmButton={TextButton(onDismiss){Text("Close")}},
    title={Text("Menu", fontWeight=FontWeight.ExtraBold, color=Brand)},
    text={ Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      MItem("All Products"){onLink(HOME+"?post_type=product")}
      Text("CATEGORIES", color=Brand, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.padding(top=6.dp))
      cats.forEach { (n,s) -> MItem(n){ onLink(HOME+"product-category/$s/") } }
      Text("ACCOUNT", color=Brand, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.padding(top=6.dp))
      MItem("My Account"){onLink(HOME+"my-account/")}
      MItem("Track My Order"){onLink(HOME+"my-account/orders/")}
      MItem("Cart"){onLink(HOME+"cart/")}
      Text("CONTACT", color=Brand, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.padding(top=6.dp))
      MItem("Chat on WhatsApp", onWhatsApp)
    }})
}
@Composable private fun MItem(label:String, onClick:()->Unit)=Text(label, fontSize=13.sp, modifier=Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=8.dp), color=Color(0xFF14201A))


@Composable private fun OfflineScreen(onRetry: () -> Unit) {
  Column(Modifier.fillMaxSize().background(Color(0xFFF7FAF6)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("📵", fontSize = 56.sp)
    Spacer(Modifier.height(12.dp))
    Text("You're offline", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF14201A))
    Spacer(Modifier.height(6.dp))
    Text("Check your internet connection and try again.", color = Color.Gray, fontSize = 13.sp)
    Spacer(Modifier.height(18.dp))
    Button(onClick = onRetry, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
      Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Retry")
    }
  }
}
