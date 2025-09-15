package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

class JCEFBugSimpleStressTestAction : AnAction("Simple Fast Stress Test") {
    
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
            "JCEF Simple Stress Test",
            Messages.getQuestionIcon(),
            "1000",
            null
        )?.toIntOrNull() ?: return
        
        val delayMs = Messages.showInputDialog(
            project,
            "Delay between iterations (milliseconds):",
            "JCEF Simple Stress Test", 
            Messages.getQuestionIcon(),
            "10",
            null
        )?.toLongOrNull() ?: return
        
        startStressTest(project, iterations, delayMs)
    }
    
    private fun startStressTest(project: Project, maxIterations: Int, delayMs: Long) {
        isRunning.set(true)
        
        Thread {
            val iterationCount = AtomicInteger(0)
            val errorCount = AtomicInteger(0)
            val startTime = System.currentTimeMillis()
            
            try {
                while (isRunning.get()) {
                    val currentIteration = iterationCount.incrementAndGet()
                    
                    // Stop if we've reached max iterations (unless 0 = infinite)
                    if (maxIterations > 0 && currentIteration > maxIterations) {
                        break
                    }
                    
                    println("Iteration $currentIteration - Opening dialog...")
                    
                    try {
                        // Run dialog operations on EDT
                        ApplicationManager.getApplication().invokeAndWait {
                            try {
                                val dialog = JCEFBugDialog(project)
                                dialog.show()
                                
                                // Wait a brief moment then close manually for fast clicking simulation
                                Thread.sleep(delayMs)
                                dialog.close(0)
                            } catch (e: Exception) {
                                errorCount.incrementAndGet()
                                println("ERROR in iteration $currentIteration: ${e.message}")
                                e.printStackTrace()
                                
                                // Stop on error
                                isRunning.set(false)
                            }
                        }
                        
                    } catch (e: Exception) {
                        errorCount.incrementAndGet()
                        println("OUTER ERROR in iteration $currentIteration: ${e.message}")
                        e.printStackTrace()
                        
                        // Stop on error
                        isRunning.set(false)
                    }
                    
                    // Brief delay between iterations
                    if (delayMs > 0) {
                        Thread.sleep(delayMs)
                    }
                }
                
            } finally {
                isRunning.set(false)
                
                ApplicationManager.getApplication().invokeLater {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val finalIteration = iterationCount.get()
                    val finalErrors = errorCount.get()
                    
                    val message = "Simple stress test completed!\n" +
                        "Total iterations: $finalIteration\n" +
                        "Errors encountered: $finalErrors\n" +
                        "Time elapsed: ${elapsed}s"
                    
                    Messages.showInfoMessage(project, message, "JCEF Simple Stress Test Results")
                }
            }
        }.start()
    }
}
