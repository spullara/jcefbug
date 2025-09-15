package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

class JCEFBugTargetedTestAction : AnAction("Targeted RemoteMessageRouter Race Test") {
    
    private val isRunning = AtomicBoolean(false)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (isRunning.get()) {
            Messages.showInfoMessage(project, "Targeted test is already running!", "JCEF Targeted Test")
            return
        }
        
        val choice = Messages.showYesNoDialog(
            project,
            "This test specifically targets the RemoteMessageRouterImpl race condition.\n" +
            "It rapidly creates/destroys CefMessageRouter instances to trigger the NPE.\n\n" +
            "Expected error: 'Cannot read field \"objId\" because \"robj\" is null'\n\n" +
            "Continue?",
            "JCEF RemoteMessageRouter Race Test",
            Messages.getWarningIcon()
        )
        
        if (choice != Messages.YES) return
        
        startTargetedTest(project)
    }
    
    private fun startTargetedTest(project: Project) {
        isRunning.set(true)
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "JCEF RemoteMessageRouter Race Test", true) {
            override fun run(indicator: ProgressIndicator) {
                val iterationCount = AtomicInteger(0)
                val errorCount = AtomicInteger(0)
                val startTime = System.currentTimeMillis()
                
                indicator.text = "Starting targeted RemoteMessageRouter race test..."
                indicator.isIndeterminate = true
                
                try {
                    while (!indicator.isCanceled && isRunning.get() && errorCount.get() < 5) {
                        val currentIteration = iterationCount.incrementAndGet()
                        indicator.text = "Iteration $currentIteration - Creating CefMessageRouter..."
                        
                        try {
                            // Strategy: Rapid CefMessageRouter creation without full dialog lifecycle
                            // This isolates the race condition to just the message router creation
                            ApplicationManager.getApplication().invokeAndWait {
                                try {
                                    println("Iteration $currentIteration: Creating CefMessageRouter...")
                                    
                                    // This is the exact line that fails in your stack trace
                                    val routerConfig = org.cef.browser.CefMessageRouter.CefMessageRouterConfig("jcefQuery", "jcefQueryCancel")
                                    val messageRouter = org.cef.browser.CefMessageRouter.create(routerConfig)
                                    
                                    println("Iteration $currentIteration: CefMessageRouter created successfully")
                                    
                                    // Immediately dispose to create cleanup race conditions
                                    messageRouter?.dispose()
                                    
                                } catch (e: Exception) {
                                    errorCount.incrementAndGet()
                                    println("ERROR in iteration $currentIteration: ${e.javaClass.simpleName}: ${e.message}")
                                    
                                    // Check if this is the specific NPE we're looking for
                                    if (e is NullPointerException && 
                                        e.stackTrace.any { it.className.contains("RemoteMessageRouterImpl") }) {
                                        println("SUCCESS! Reproduced the RemoteMessageRouterImpl NPE bug!")
                                        
                                        ApplicationManager.getApplication().invokeLater {
                                            Messages.showInfoMessage(
                                                project,
                                                "Successfully reproduced the bug!\n\n" +
                                                "Error: ${e.javaClass.simpleName}\n" +
                                                "Message: ${e.message}\n" +
                                                "Iteration: $currentIteration\n\n" +
                                                "This confirms the race condition in RemoteMessageRouterImpl.create()",
                                                "Bug Reproduced!"
                                            )
                                        }
                                        isRunning.set(false) // Stop the test
                                    }
                                    
                                    e.printStackTrace()
                                }
                            }
                            
                            // Very short delay to maximize race condition opportunity
                            Thread.sleep(1)
                            
                        } catch (e: Exception) {
                            errorCount.incrementAndGet()
                            println("OUTER ERROR in iteration $currentIteration: ${e.message}")
                            e.printStackTrace()
                        }
                        
                        // Update progress
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        indicator.text2 = "Errors: ${errorCount.get()}, Elapsed: ${elapsed}s"
                        
                        // Auto-stop after 1000 iterations if no errors
                        if (currentIteration >= 1000) {
                            println("Completed 1000 iterations without reproducing the bug")
                            break
                        }
                    }
                    
                } finally {
                    isRunning.set(false)
                    
                    ApplicationManager.getApplication().invokeLater {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val finalIterations = iterationCount.get()
                        val finalErrors = errorCount.get()
                        
                        if (finalErrors == 0) {
                            Messages.showInfoMessage(
                                project,
                                "Test completed without reproducing the bug.\n\n" +
                                "Total iterations: $finalIterations\n" +
                                "Time elapsed: ${elapsed}s\n\n" +
                                "The race condition may require different timing or conditions.",
                                "Test Complete"
                            )
                        }
                    }
                }
            }
        })
    }
}
