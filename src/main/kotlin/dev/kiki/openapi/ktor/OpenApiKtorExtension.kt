package dev.kiki.openapi.ktor

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

abstract class OpenApiKtorExtension @Inject constructor(objects: ObjectFactory) {
    val specs: NamedDomainObjectContainer<OpenApiKtorSpec> =
        objects.domainObjectContainer(OpenApiKtorSpec::class.java) { name -> OpenApiKtorSpec(name, objects) }

    fun spec(name: String, configure: OpenApiKtorSpec.() -> Unit) {
        specs.create(name).configure()
    }
}

open class OpenApiKtorSpec internal constructor(
    private val specName: String,
    objects: ObjectFactory,
) : Named {
    val inputSpec: RegularFileProperty = objects.fileProperty()
    val packageName: Property<String> = objects.property(String::class.java)
    val includeTags: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())
    val validateSpec: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    /** Generates `@Singleton` and `@Inject` API constructors for Hilt/Dagger. */
    val useHilt: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    override fun getName(): String = specName
}
