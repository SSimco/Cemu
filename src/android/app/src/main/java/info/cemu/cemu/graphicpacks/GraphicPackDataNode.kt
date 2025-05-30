package info.cemu.cemu.graphicpacks

class GraphicPackDataNode(
    val id: Long,
    name: String,
    val path: String,
    enabled: Boolean,
    val parent: GraphicPackSectionNode?,
) :
    GraphicPackNode(name) {
    constructor(
        id: Long,
        name: String,
        path: String,
        enabled: Boolean,
        titleIdInstalled: Boolean,
        parentNode: GraphicPackSectionNode,
    ) : this(id, name, path, enabled, parentNode) {
        this.titleIdInstalled = titleIdInstalled
    }

    var enabled: Boolean = enabled
        set(value) {
            if (field == value) {
                return
            }
            field = value
            parent?.updateEnabledCount(value)
        }
}