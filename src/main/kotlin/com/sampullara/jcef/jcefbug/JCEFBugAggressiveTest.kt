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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JCEFBugAggressiveTestAction : AnAction("Start Aggressive JCEF Test") {
    
    private val isRunning = AtomicBoolean(false)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (isRunning.get()) {
            Messages.showInfoMessage(project, "Aggressive test is already running!", "JCEF Aggressive Test")
            return
        }
        
        val choice = Messages.showYesNoDialog(
            project,
            "This will rapidly open/close multiple JCEF dialogs simultaneously.\n" +
            "This is designed to trigger race conditions and disposal issues.\n\n" +
            "Continue with aggressive testing?",
            "JCEF Aggressive Test Warning",
            Messages.getWarningIcon()
        )
        
        if (choice != Messages.YES) return
        
        startAggressiveTest(project)
    }
    
    private fun startAggressiveTest(project: Project) {
        isRunning.set(true)
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "JCEF Aggressive Test", true) {
            override fun run(indicator: ProgressIndicator) {
                val iterationCount = AtomicInteger(0)
                val errorCount = AtomicInteger(0)
                val activeDialogs = AtomicInteger(0)
                val errors = ConcurrentLinkedQueue<String>()
                val startTime = System.currentTimeMillis()
                
                indicator.text = "Starting aggressive JCEF test..."
                indicator.isIndeterminate = true
                
                val executor = Executors.newFixedThreadPool(4)
                
                try {
                    // Strategy 1: Rapid sequential open/close
                    executor.submit {
                        while (!indicator.isCanceled && isRunning.get()) {
                            try {
                                val iteration = iterationCount.incrementAndGet()
                                
                                ApplicationManager.getApplication().invokeAndWait {
                                    try {
                                        val dialog = JCEFBugDialog(project)
                                        activeDialogs.incrementAndGet()
                                        dialog.show()

                                        // Very fast close - simulate rapid clicking
                                        Thread.sleep(5)
                                        dialog.close(0)

                                        activeDialogs.decrementAndGet()
                                        
                                    } catch (e: Exception) {
                                        errorCount.incrementAndGet()
                                        errors.offer("Sequential[$iteration]: ${e.message}")
                                        println("Sequential error [$iteration]: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                                
                                Thread.sleep(5) // Very short delay
                                
                            } catch (e: Exception) {
                                errorCount.incrementAndGet()
                                errors.offer("Sequential thread: ${e.message}")
                                break
                            }
                        }
                    }
                    
                    // Strategy 2: Overlapping dialogs
                    executor.submit {
                        while (!indicator.isCanceled && isRunning.get()) {
                            try {
                                val iteration = iterationCount.incrementAndGet()
                                
                                ApplicationManager.getApplication().invokeLater {
                                    try {
                                        val dialog = JCEFBugDialog(project)
                                        activeDialogs.incrementAndGet()

                                        dialog.show()

                                        // Random quick close timing
                                        Thread {
                                            try {
                                                Thread.sleep((Math.random() * 50 + 10).toLong()) // 10-60ms
                                                ApplicationManager.getApplication().invokeLater {
                                                    dialog.close(0)
                                                }
                                                activeDialogs.decrementAndGet()
                                            } catch (e: Exception) {
                                                errorCount.incrementAndGet()
                                                errors.offer("Overlapping close[$iteration]: ${e.message}")
                                            }
                                        }.start()
                                        
                                    } catch (e: Exception) {
                                        errorCount.incrementAndGet()
                                        errors.offer("Overlapping[$iteration]: ${e.message}")
                                        println("Overlapping error [$iteration]: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                                
                                Thread.sleep(20)
                                
                            } catch (e: Exception) {
                                errorCount.incrementAndGet()
                                errors.offer("Overlapping thread: ${e.message}")
                                break
                            }
                        }
                    }
                    
                    // Strategy 3: Immediate close after show
                    executor.submit {
                        while (!indicator.isCanceled && isRunning.get()) {
                            try {
                                val iteration = iterationCount.incrementAndGet()
                                
                                ApplicationManager.getApplication().invokeAndWait {
                                    try {
                                        val dialog = JCEFBugDialog(project)
                                        activeDialogs.incrementAndGet()

                                        dialog.show()
                                        dialog.close(0) // Immediate close - this creates the race condition

                                        activeDialogs.decrementAndGet()
                                        
                                    } catch (e: Exception) {
                                        errorCount.incrementAndGet()
                                        errors.offer("Immediate[$iteration]: ${e.message}")
                                        println("Immediate error [$iteration]: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                                
                                Thread.sleep(1) // Minimal delay
                                
                            } catch (e: Exception) {
                                errorCount.incrementAndGet()
                                errors.offer("Immediate thread: ${e.message}")
                                break
                            }
                        }
                    }
                    
                    // Monitor and update progress
                    while (!indicator.isCanceled && isRunning.get()) {
                        Thread.sleep(1000)
                        
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val currentIterations = iterationCount.get()
                        val currentErrors = errorCount.get()
                        val currentActive = activeDialogs.get()
                        
                        indicator.text = "Iterations: $currentIterations, Active: $currentActive"
                        indicator.text2 = "Errors: $currentErrors, Elapsed: ${elapsed}s"
                        
                        // Stop if we have too many errors
                        if (currentErrors > 10) {
                            println("Stopping due to too many errors: $currentErrors")
                            break
                        }
                        
                        // Auto-stop after 5 minutes if no errors
                        if (elapsed > 300 && currentErrors == 0) {
                            println("Auto-stopping after 5 minutes with no errors")
                            break
                        }
                    }
                    
                } finally {
                    isRunning.set(false)
                    executor.shutdown()
                    
                    try {
                        executor.awaitTermination(5, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        executor.shutdownNow()
                    }
                    
                    ApplicationManager.getApplication().invokeLater {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val finalIterations = iterationCount.get()
                        val finalErrors = errorCount.get()
                        
                        val errorDetails = if (errors.isNotEmpty()) {
                            "\n\nFirst few errors:\n" + errors.take(5).joinToString("\n")
                        } else ""
                        
                        val message = "Aggressive test completed!\n" +
                                "Total iterations: $finalIterations\n" +
                                "Errors encountered: $finalErrors\n" +
                                "Time elapsed: ${elapsed}s$errorDetails"
                        
                        Messages.showInfoMessage(project, message, "JCEF Aggressive Test Results")
                    }
                }
            }
        })
    }
}
