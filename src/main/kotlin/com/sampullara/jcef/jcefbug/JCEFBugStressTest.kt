package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

class JCEFBugStressTestAction : AnAction("Start JCEF Stress Test") {
    
    private val isRunning = AtomicBoolean(false)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (isRunning.get()) {
            Messages.showInfoMessage(project, "Stress test is already running!", "JCEF Stress Test")
            return
        }
        
        val iterations = Messages.showInputDialog(
            project,
            "How many iterations to run? (0 = run until failure)",
            "JCEF Stress Test Configuration",
            Messages.getQuestionIcon(),
            "100",
            null
        )?.toIntOrNull() ?: return
        
        val delayMs = Messages.showInputDialog(
            project,
            "Delay between iterations (milliseconds):",
            "JCEF Stress Test Configuration", 
            Messages.getQuestionIcon(),
            "100",
            null
        )?.toLongOrNull() ?: return
        
        startStressTest(project, iterations, delayMs)
    }
    
    private fun startStressTest(project: Project, maxIterations: Int, delayMs: Long) {
        isRunning.set(true)
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "JCEF Stress Test", true) {
            override fun run(indicator: ProgressIndicator) {
                val iterationCount = AtomicInteger(0)
                val errorCount = AtomicInteger(0)
                val startTime = System.currentTimeMillis()
                
                indicator.text = "Starting JCEF stress test..."
                indicator.isIndeterminate = maxIterations == 0
                
                try {
                    while (!indicator.isCanceled && isRunning.get()) {
                        val currentIteration = iterationCount.incrementAndGet()
                        
                        if (maxIterations > 0) {
                            if (currentIteration > maxIterations) break
                            indicator.fraction = currentIteration.toDouble() / maxIterations
                        }
                        
                        indicator.text = "Iteration $currentIteration - Opening dialog..."
                        
                        try {
                            // Run dialog operations on EDT
                            var dialog: JCEFBugDialog? = null
                            ApplicationManager.getApplication().invokeAndWait {
                                try {
                                    dialog = JCEFBugDialog(project, autoCloseOnEcho = true)
                                    dialog!!.show()
                                } catch (e: Exception) {
                                    errorCount.incrementAndGet()
                                    println("ERROR creating dialog in iteration $currentIteration: ${e.message}")
                                    e.printStackTrace()
                                }
                            }

                            // Wait for the dialog to auto-close after receiving echo message
                            // or timeout after reasonable time (outside EDT)
                            if (dialog != null) {
                                try {
                                    var waitTime = 0
                                    while (dialog!!.isShowing && waitTime < 3000) { // Max 3 seconds wait
                                        Thread.sleep(10)
                                        waitTime += 10
                                    }

                                    // Force close if still showing (fallback)
                                    if (dialog!!.isShowing) {
                                        ApplicationManager.getApplication().invokeAndWait {
                                            dialog!!.close(0)
                                        }
                                    }

                                } catch (e: Exception) {
                                    errorCount.incrementAndGet()
                                    println("ERROR in iteration $currentIteration: ${e.message}")
                                    e.printStackTrace()
                                    
                                    // Show error and ask if we should continue
                                    val choice = Messages.showYesNoDialog(
                                        project,
                                        "Error occurred in iteration $currentIteration:\n${e.message}\n\nContinue testing?",
                                        "JCEF Stress Test Error",
                                        Messages.getErrorIcon()
                                    )
                                    
                                    if (choice != Messages.YES) {
                                        throw e // This will stop the test
                                    }
                                }
                            }
                            
                            // Delay between iterations
                            if (delayMs > 0) {
                                Thread.sleep(delayMs)
                            }
                            
                        } catch (e: Exception) {
                            println("FATAL ERROR in iteration $currentIteration: ${e.message}")
                            e.printStackTrace()
                            break
                        }
                        
                        // Update progress text
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        indicator.text2 = "Errors: ${errorCount.get()}, Elapsed: ${elapsed}s"
                    }
                    
                } finally {
                    isRunning.set(false)
                    
                    ApplicationManager.getApplication().invokeLater {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val finalIteration = iterationCount.get()
                        val finalErrors = errorCount.get()
                        
                        val message = if (indicator.isCanceled) {
                            "Stress test cancelled after $finalIteration iterations\n" +
                            "Errors encountered: $finalErrors\n" +
                            "Time elapsed: ${elapsed}s"
                        } else {
                            "Stress test completed!\n" +
                            "Total iterations: $finalIteration\n" +
                            "Errors encountered: $finalErrors\n" +
                            "Time elapsed: ${elapsed}s"
                        }
                        
                        Messages.showInfoMessage(project, message, "JCEF Stress Test Results")
                    }
                }
            }
        })
    }
}
