package inspector

import org.gradle.api.artifacts.component.ComponentIdentifier

data class Requester(
    val id: ComponentIdentifier,
    val root: ComponentIdentifier,
    val isDirect: Boolean
)
