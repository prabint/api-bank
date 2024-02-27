package api.bank.utils

import api.bank.models.Constants
import api.bank.models.RequestDetail
import api.bank.models.RequestGroup
import com.google.gson.Gson
import com.intellij.ui.treeStructure.Tree
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun TreeNode.getRequestDetail() = (this as DefaultMutableTreeNode).userObject as? RequestDetail

fun TreeNode.getGroupName() = (this as DefaultMutableTreeNode).userObject as? String

fun TreeModel.toRequests() = (this.root as DefaultMutableTreeNode)
    .breadthFirstEnumeration()
    .asSequence()
    .filter { it.isLeaf }
    .mapNotNull { (it as DefaultMutableTreeNode).userObject as? RequestDetail }
    .toMutableList()

fun TreeModel.toGroupList(): List<RequestGroup> {
    val groupList = mutableListOf<RequestGroup>()
    val root = root as DefaultMutableTreeNode

    root.children().asSequence().forEach { treeNode ->
        val defaultNode = treeNode as DefaultMutableTreeNode

        if (defaultNode.userObject is String) {
            val requests = mutableListOf<RequestDetail>()
            val groupName = defaultNode.userObject as String
            val requestGroup = RequestGroup(groupName, requests)

            defaultNode
                .children()
                .asSequence()
                .forEach { childNode ->
                    childNode
                        .getRequestDetail()
                        ?.let { requestDetail -> requests.add(requestDetail) }
                }

            groupList.add(requestGroup)
        }
    }

    return groupList
}

fun TreeModel.saveAsJsonFile(gson: Gson, rootDir: File) {
    File(rootDir, Constants.FILE_API_DETAIL_PERSISTENT).writeText(gson.toJson(toGroupList()))
}

fun Tree.expandTree() {
    val root = model.root as DefaultMutableTreeNode
    val e = root.breadthFirstEnumeration()
    while (e.hasMoreElements()) {
        val node = e.nextElement() as DefaultMutableTreeNode
        if (node.isLeaf) continue
        val row = getRowForPath(TreePath(node.path))
        expandRow(row)
    }
}

fun TreeNode.isGroupNode(): Boolean {
    return (this as? DefaultMutableTreeNode)?.userObject is String
}

fun Tree.getAllGroupNames() = (this.model.root as DefaultMutableTreeNode)
    .children()
    .asSequence()
    .toList()
    .map { (it as DefaultMutableTreeNode).userObject as String }
