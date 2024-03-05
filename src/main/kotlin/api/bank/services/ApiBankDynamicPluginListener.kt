package api.bank.services

import api.bank.models.Constants
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Paths

class ApiBankDynamicPluginListener(private val project: Project) : DynamicPluginListener {
    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        File(Paths.get(project.basePath!!, ".idea", Constants.FILE_SETTINGS).toString()).delete()
        super.beforePluginUnload(pluginDescriptor, isUpdate)
    }
}
