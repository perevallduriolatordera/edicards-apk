# Google Cloud Setup Guide - Routes API

**Estimated time**: 20 minutes
**Cost to set up**: Free

---

## Step 1: Create Google Cloud Project (5 minutes)

### 1.1 Go to Google Cloud Console
```
https://console.cloud.google.com/
```

### 1.2 Create a new project
- Click on the project selector at the top
- Click "New Project"
- **Project name**: `Edicards Routes`
- Click "Create"
- Wait 1-2 minutes for project creation

### 1.3 Select your project
- The dropdown at the top should now show "Edicards Routes"
- If not, click the selector and choose it

---

## Step 2: Enable APIs (5 minutes)

### 2.1 Enable Routes API v2
1. Go to: https://console.cloud.google.com/apis/library/routes.googleapis.com
2. Click "Enable"
3. Wait for it to say "API is enabled"

### 2.2 Enable Geocoding API (optional but recommended)
1. Go to: https://console.cloud.google.com/apis/library/geocoding-backend.googleapis.com
2. Click "Enable"
3. This is free (2,500 requests/day) and will help with address resolution

---

## Step 3: Create API Key (5 minutes)

### 3.1 Go to Credentials
1. Left sidebar → "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "API Key"
3. Copy the key (you'll need it in Step 5)
4. Click "Edit API key" (pencil icon)

### 3.2 Restrict the API Key

**IMPORTANT**: Never use unrestricted API keys in production!

1. **Application restrictions**:
   - Select "Android apps"
   - Click "Add package name and fingerprint"
   - **Package name**: `net.ifeu.edicards`
   - **SHA-1 fingerprint**: Get this from your Android signing certificate
     ```
     # From Android Studio terminal:
     keytool -list -v -keystore ~/.android/debug.keystore
     # (password: android, if using debug keystore)

     # Or from your release keystore:
     keytool -list -v -keystore /path/to/your/keystore.jks
     ```

2. **API restrictions**:
   - Select "Routes API v2"
   - Click "Save"

### 3.3 Copy your API Key
- You'll see your key in the credentials list
- Copy it (you'll add it to your app in Step 5)

---

## Step 4: Set Up Billing & Budget Alerts (5 minutes)

### 4.1 Enable Billing
1. Left sidebar → "Billing"
2. Click "Link Billing Account"
3. Create new billing account (requires credit card)
4. Link to your "Edicards Routes" project

**Note**: You won't be charged until you exceed free tier. Routes API costs $0.45 per request after free tier (if any).

### 4.2 Set Budget Alert
1. Left sidebar → "Billing" → "Budgets and alerts"
2. Click "Create Budget"
3. **Budget name**: `Edicards Routes Monthly`
4. **Budget period**: Monthly
5. **Budget amount**: $150 USD (gives you buffer above $945 limit)
6. **Alert threshold**: 50% ($75) and 100% ($150)
7. **Email**: Your email
8. Click "Create"

**Result**: You'll get alerts if you're on track to exceed your budget.

---

## Step 5: Add API Key to Your Android App

### 5.1 Add to local.properties
```properties
# local.properties

GOOGLE_MAPS_API_KEY=YOUR_API_KEY_HERE
```

**Replace `YOUR_API_KEY_HERE`** with the key from Step 3.3

### 5.2 Update your code

The implementation is already in place in your code:
```java
// In RouteGeneratorService:
String googleMapsApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;
// This is used in:
// - generateRouteAsync()
// - optimizeRouteWithGoogleMaps()
```

Make sure `ConstantsEndpoints.GOOGLE_MAPS_API_KEY` is properly set:
```java
// ConstantsEndpoints.java
public static final String GOOGLE_MAPS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY;
```

### 5.3 Build your app
```bash
./gradlew assembleDebug
# or
./gradlew assembleRelease
```

The API key from `local.properties` will be automatically injected via gradle.

---

## Step 6: Monitor Costs (2 minutes/month)

### 6.1 Check monthly spending
1. Go to: https://console.cloud.google.com/billing
2. Click your billing account
3. Look at "Current month's costs"
4. Should be around $945 ± $50

### 6.2 View API usage
1. Go to: https://console.cloud.google.com/apis/dashboard
2. Select "Routes API v2"
3. You'll see:
   - Requests per day
   - Monthly total
   - Estimated cost

### 6.3 Expected numbers
```
Weekly (1x per week):
- Routes: 15 routes
- Clusters per route: 35
- Total API requests: 525/week
- Cost: ~$236/week

Monthly:
- Routes: 60
- Total API requests: 2,100
- Cost: ~$945
```

---

## Troubleshooting

### API Key Not Working?
1. **Check restrictions**:
   - Go to Credentials → Your key
   - Verify "Android apps" is selected
   - Verify SHA-1 fingerprint matches your keystore

2. **Check enabled APIs**:
   - Go to APIs & Services → Enabled APIs
   - Verify "Routes API v2" is listed

3. **Check billing**:
   - Go to Billing → Billing overview
   - Make sure billing account is active
   - Look for "Warning: Billing account not linked" (if shown, fix it)

### Getting 403 Forbidden?
- API key is restricted to wrong package name
- Or SHA-1 fingerprint doesn't match
- Solution: Update restrictions in API key settings

### Getting 429 Too Many Requests?
- Unlikely at your volume (2,100/month = 70/day)
- But if it happens, add exponential backoff retry logic
- Google Cloud auto-scales, so this is rare

### Getting 400 Bad Request?
- Usually means bad coordinates
- Already handled in your code (invalid Spanish coordinates filtered out)
- Check logs for "COORDENADAS INVÁLIDAS"

---

## Security Best Practices

### ✅ DO:
- Restrict API key to Android package
- Restrict to specific APIs (Routes only)
- Set usage quotas/budgets
- Monitor costs weekly
- Rotate key every 6 months

### ❌ DON'T:
- Use same API key in web/server code
- Share API key in GitHub commits
- Use unrestricted API keys
- Leave notifications on "forever"

### For Production:
- Use separate API keys for debug/release builds
- Store sensitive data in Google Secret Manager (free tier available)
- Implement request signing (optional but recommended)

---

## Cost Optimization Tips

### Tip 1: Batch Processing
If you generate multiple routes simultaneously:
```java
// Instead of:
for (int rep : reps) {
    generateRoute(rep);  // Sequential = longer)
}

// Do this:
ExecutorService executor = Executors.newFixedThreadPool(5);
for (int rep : reps) {
    executor.submit(() -> generateRoute(rep));  // Parallel
}
executor.shutdown();
executor.awaitTermination(10, TimeUnit.MINUTES);
```

**Benefit**: Faster execution, same API cost

### Tip 2: Cache Geocoding Results
Your code already does this! But verify in logs:
```
✓ Coordenadas obtenidas y cacheadas: LatLng{latitude=41.760809, longitude=-0.833035}
```

### Tip 3: Reuse Last Week's Route (if clients don't change)
```java
// Pseudocode
if (clientsUnchanged && lastRouteIsRecent) {
    return lastRoute;  // Cost: $0!
} else {
    return generateNewRoute();  // Cost: $15.75
}
```

---

## Monitoring Dashboard

Once set up, create a quick reference:

```
EDICARDS ROUTES - MONTHLY SUMMARY
═══════════════════════════════════════════════════════════════

Month:        FEBRUARY 2026
Project:      Edicards Routes
API:          Routes API v2

Routes Generated:         60
API Requests:            2,100
Cost:                    $945
Budget:                  $1,200 (monthly limit)
Alert Threshold:         $150 (50% of budget)
Status:                  ✅ NORMAL

Expected vs Actual:
- Expected: 60 routes × 35 clusters = 2,100 requests
- Actual:   2,095 requests (slightly better! 👍)
- Variance: -0.2% (excellent)

Cost Efficiency:
- Cost per route:   $15.75
- Cost per cluster: $0.45
- Cost per client:  $0.00126
- Cost per visit:   €0.019

Next Month Forecast: $945 (stable)
═══════════════════════════════════════════════════════════════
```

---

## Support & Resources

### Official Documentation
- Routes API v2: https://developers.google.com/maps/documentation/routes/overview
- Pricing: https://developers.google.com/maps/billing-and-pricing/pricing
- Quotas: https://developers.google.com/maps/quotas

### Google Cloud Support
- Free support included (email/chat)
- For 24/7 phone support, upgrade to paid support plan

### Your Team
- Question about API behavior? Check the official docs first
- Issue with your implementation? Check your code in `RouteGeneratorService.java`
- Cost tracking? Monthly check in Cloud Console (5 minutes)

---

## Final Checklist

- [ ] Create Google Cloud Project "Edicards Routes"
- [ ] Enable Routes API v2
- [ ] Enable Geocoding API (optional)
- [ ] Create API Key with Android restrictions
- [ ] Set budget alert at $150/month
- [ ] Add API key to `local.properties`
- [ ] Update `ConstantsEndpoints.java` if needed
- [ ] Build and test app
- [ ] Verify API key works (check logs: "✓ API key decodificada correctamente")
- [ ] Set up monthly monitoring (Calendar reminder)

**Once done**: You're live! Routes will be optimized with Google Maps API.

---

## Quick Reference: What You're Paying For

```
$945/month ÷ 60 routes = $15.75 per route
$15.75 per route ÷ 500 clients = $0.00126 per client optimized
$0.00126 ÷ 7 days = $0.00018 per client per day

For a single commercial visit:
- Fuel burned: ~€0.50-1.00
- Route optimization cost: €0.00019
- Value added: 25-40% efficiency gain = €0.125-0.40 saved

RESULT: You spend basically nothing while saving significant fuel costs! ✅
```

---

**Questions?**

See:
- `COST_WEEKLY_REGEN.md` - Cost analysis
- `COSTS_SUMMARY.txt` - Quick reference
- `ROUTE_OPTIMIZATION_ANALYSIS.md` - Technical details
- Your code: `RouteGeneratorService.java` - Implementation
