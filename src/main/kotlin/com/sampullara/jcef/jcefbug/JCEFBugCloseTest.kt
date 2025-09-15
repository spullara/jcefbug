package com.sampullara.jcef.jcefbug

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages

class JCEFBugCloseTestAction : AnAction("Test Dialog Close Mechanism") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        Messages.showInfoMessage(
            project,
            "This will test the basic dialog close mechanism.\n" +
            "The dialog will open and then automatically close after 1 second.\n\n" +
            "Watch the console for debug messages.",
            "Dialog Close Test"
        )
        
        val dialog = JCEFBugDialog(project)
        dialog.show()
        
        println("Dialog opened, isShowing: ${dialog.isShowing}")
        
        // Try to close it after 1 second
        Thread {
            Thread.sleep(1000)
            println("Attempting to close dialog after 1 second...")
            
            ApplicationManager.getApplication().invokeLater {
                println("On EDT: dialog.isShowing = ${dialog.isShowing}")
                
                try {
                    if (dialog.isShowing) {
                        println("Calling doCancelAction()...")
                        dialog.doCancelAction()
                    }
                    
                    Thread.sleep(100) // Brief pause
                    
                    if (dialog.isShowing) {
                        println("Still showing, calling close(0)...")
                        dialog.close(0)
                    }
                    
                    Thread.sleep(100) // Brief pause
                    
                    // Skip dispose() call since it's protected
                    
                    println("Final state: dialog.isShowing = ${dialog.isShowing}")
                    
                } catch (e: Exception) {
                    println("Error during close test: ${e.message}")
                    e.printStackTrace()
                }
            }
        }.start()
    }
}
