package com.sampullara.jcef.jcefbug

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.network.CefRequest
import org.cef.CefApp
import org.cef.browser.CefMessageRouter

import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import java.awt.BorderLayout

class JCEFBugDialog(private val project: Project) : DialogWrapper(project) {

    private lateinit var webView: JBCefBrowser
    private lateinit var contentPanel: JPanel

    init {
        title = "JCEF Bug Reproduction Dialog"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        contentPanel = JPanel(BorderLayout())
        contentPanel.preferredSize = Dimension(800, 600)

        // Create JCEF browser with message router
        webView = JBCefBrowser()
        
        // Create and configure message router
        val routerConfig = CefMessageRouter.CefMessageRouterConfig("jcefQuery", "jcefQueryCancel")
        val messageRouter = CefMessageRouter.create(routerConfig)
        
        // Add message router handler
        messageRouter.addHandler(object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(
                browser: CefBrowser?,
                frame: CefFrame?,
                queryId: Long,
                request: String?,
                persistent: Boolean,
                callback: CefQueryCallback?
            ): Boolean {
                println("Received query: $request")

                // Simple echo response
                callback?.success("Echo: $request")

                return true
            }
        }, true)
        
        // Add message router to browser
        webView.jbCefClient.cefClient.addMessageRouter(messageRouter)
        
        // Load test HTML with JavaScript communication
        val testHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>JCEF Test</title>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        padding: 20px; 
                        background-color: #f0f0f0;
                    }
                    button { 
                        padding: 10px 20px; 
                        margin: 10px; 
                        font-size: 16px;
                        background-color: #4CAF50;
                        color: white;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                    }
                    button:hover {
                        background-color: #45a049;
                    }
                    #output {
                        margin-top: 20px;
                        padding: 10px;
                        background-color: white;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        min-height: 100px;
                    }
                </style>
            </head>
            <body>
                <h1>JCEF Bug Reproduction Test</h1>
                <p>This dialog contains a JCEF WebView with message router communication.</p>
                <p>Open and close this dialog multiple times quickly to reproduce the bug.</p>
                
                <button onclick="sendTestMessage()">Send Test Message</button>
                <button onclick="sendMultipleMessages()">Send Multiple Messages</button>
                
                <div id="output"></div>
                
                <script>
                    let messageCount = 0;
                    
                    function sendTestMessage() {
                        messageCount++;
                        const message = 'Test message ' + messageCount + ' at ' + new Date().toLocaleTimeString();
                        
                        if (window.jcefQuery) {
                            window.jcefQuery({
                                request: message,
                                onSuccess: function(response) {
                                    addOutput('SUCCESS: ' + response);
                                },
                                onFailure: function(error_code, error_message) {
                                    addOutput('ERROR: ' + error_code + ' - ' + error_message);
                                }
                            });
                        } else {
                            addOutput('ERROR: jcefQuery not available');
                        }
                    }
                    
                    function sendMultipleMessages() {
                        for (let i = 0; i < 5; i++) {
                            setTimeout(() => sendTestMessage(), i * 100);
                        }
                    }
                    
                    function addOutput(message) {
                        const output = document.getElementById('output');
                        const div = document.createElement('div');
                        div.textContent = new Date().toLocaleTimeString() + ': ' + message;
                        output.appendChild(div);
                        output.scrollTop = output.scrollHeight;
                    }
                    
                    // Send initial message when page loads
                    window.addEventListener('load', function() {
                        addOutput('Page loaded, JCEF communication ready');
                        sendTestMessage();
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadHTML(testHtml)
        
        contentPanel.add(webView.component, BorderLayout.CENTER)



        return contentPanel
    }


    
    override fun dispose() {
        println("JCEFBugDialog.dispose() called")
        try {
            webView.dispose()
            println("WebView disposed successfully")
        } catch (e: Exception) {
            println("Error disposing webView: ${e.message}")
            e.printStackTrace()
        }
        super.dispose()
        println("JCEFBugDialog.dispose() completed")
    }

    override fun doCancelAction() {
        println("JCEFBugDialog.doCancelAction() called")
        super.doCancelAction()
    }

    override fun doOKAction() {
        println("JCEFBugDialog.doOKAction() called")
        super.doOKAction()
    }
}
