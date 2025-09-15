# JCEF Bug Reproduction Plugin

This IntelliJ IDEA plugin is designed to reproduce a bug related to JCEF (Java Chromium Embedded Framework) WebView and CefMessageRouter.

## What This Plugin Does

The plugin creates:

1. **Tool Window**: A "JCEF Bug" tool window on the right side with a button to open the dialog
2. **Menu Action**: An action in the Tools menu and main toolbar to open the dialog
3. **Dialog with JCEF**: A dialog window that contains:
   - A JPanel as content holder
   - A JCEFWebView (JBCefBrowser) 
   - A registered CefMessageRouter for JavaScript ↔ Java communication
   - Test HTML page with JavaScript that sends messages to Java

## How to Reproduce the Bug

### Manual Testing
1. **Build and Run**: Build the plugin and run it in a development instance of IntelliJ IDEA
2. **Open Dialog**: Use either:
   - The "JCEF Bug" tool window button (right side panel)
   - Tools menu → "Open JCEF Dialog"
   - Main toolbar button
3. **Trigger Bug**: **Click OK/Cancel buttons as fast as possible** to rapidly open/close dialogs
4. **Monitor Console**: Watch for error messages and stack traces

### Automated Testing (Recommended)
The plugin includes automated stress testing to systematically reproduce the bug:

#### 1. **Simple Fast Stress Test**
- **Access**: Tools menu → "Simple Fast Stress Test" OR tool window "Simple Fast Test" button
- **Features**:
  - Configurable number of iterations (default: 1000)
  - Configurable delay between iterations (default: 10ms)
  - Simple open → wait → close cycle
  - Stops immediately on any error
- **Use Case**: Automated rapid dialog cycling to trigger race conditions

#### 2. **Aggressive Test** (Most Effective)
- **Access**: Tools menu → "Start Aggressive JCEF Test"
- **Features**:
  - Multiple concurrent testing strategies:
    - **Rapid sequential**: Very fast open/close cycles (5ms delay)
    - **Overlapping lifecycles**: Random timing between open/close (10-60ms)
    - **Immediate close**: Manual close immediately after show (race condition)
  - Multi-threaded execution to trigger race conditions
  - Real-time monitoring of active dialogs and errors
  - Auto-stops after 10 errors or 5 minutes
- **Use Case**: Maximum pressure testing with concurrent dialog lifecycles

## Technical Details

### Components Created

- **JCEFBugAction**: Action class that opens the dialog
- **JCEFBugDialog**: Modal DialogWrapper containing the JCEF WebView with manual OK/Cancel buttons
- **JCEFBugSimpleStressTest**: Simple automated stress testing with configurable parameters
- **JCEFBugAggressiveTest**: Multi-threaded concurrent testing for maximum race condition pressure
- **JCEFBugTargetedTest**: Isolates the CefMessageRouter creation race condition
- **JCEFBugToolWindow**: Tool window factory and content
- **CefMessageRouter**: Configured with test communication handlers

### JCEF Setup

The dialog creates:
- JBCefBrowser instance
- CefMessageRouter with "jcefQuery" and "jcefQueryCancel" 
- Message handler that echoes received messages
- Test HTML with JavaScript that sends messages via `window.jcefQuery()`

### Test Communication

The HTML page includes:
- Buttons to send test messages
- JavaScript that communicates with Java via CefMessageRouter
- Output area showing communication results
- Automatic message sending on page load

## Building and Running

```bash
# Build the plugin
./gradlew buildPlugin

# Run in development IDE
./gradlew runIde
```

## Expected Behavior for Bug Reproduction

When opening and closing the dialog rapidly multiple times, the CefMessageRouter and JCEF WebView disposal/cleanup should eventually trigger the bug you're investigating. The combination of:

- Quick dialog open/close cycles
- Active JavaScript ↔ Java communication
- CefMessageRouter cleanup during disposal

Should create the conditions needed to reproduce the issue.

## 🐛 **Confirmed Bug Reproduction**

**Bug**: `java.lang.NullPointerException: Cannot read field "objId" because "robj" is null`

**Location**: `com.jetbrains.cef.remote.router.RemoteMessageRouterImpl.create(RemoteMessageRouterImpl.java:38)`

**Root Cause**: Race condition in JCEF's remote message router initialization where the remote object (`robj`) is accessed before being properly initialized.

**Trigger Conditions**:
- Rapid CefMessageRouter creation/disposal cycles
- Multiple concurrent JCEF browser instances
- Remote CEF server connection timing issues

**Reproduction Success**: ✅ Confirmed with the stress testing tools in this plugin.

### **Additional Testing Tools**
- **Targeted RemoteMessageRouter Race Test**: Isolates the race condition to just CefMessageRouter creation
- **Test Dialog Close Mechanism**: Tests basic dialog closing without JCEF complexity
- **Manual Test**: Single dialog for manual verification
