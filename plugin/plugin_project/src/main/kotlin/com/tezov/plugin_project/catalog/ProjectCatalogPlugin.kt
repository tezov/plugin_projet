package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.PLUGIN_CATALOG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.CatalogMap.Companion.KEY_SEPARATOR
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.util.concurrent.atomic.AtomicBoolean

fun Settings.tezovCatalogSource(value: String) {
    extensions.findByType(SettingsExtension::class.java)?.buildCatalog(value)
        ?: PLUGIN_CATALOG.throwException(
            this,
            "you need to apply the plugin $CATALOG_PLUGIN_ID before to use this extension"
        )
}

abstract class SettingsExtension constructor(
    private val settings: Settings
) {
    internal var catalog: CatalogMap? = null

    internal fun buildCatalog(sourceCatalog: String) {
        val catalog = CatalogMap(
            settings = settings,
            catalogPointer = CatalogPointer.build(
                settings = settings,
                from = sourceCatalog
            )
        )

        val domainLibrary = settings
            .dependencyResolutionManagement
            .versionCatalogs
            .let {
                it.findByName(ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES) ?: it.create(
                    ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES
                )
            }

        val versionsKey = ProjectCatalogPlugin.CATALOG_KEY_VERSIONS + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(versionsKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogVersions ->
            with(domainLibrary) {
                catalogVersions.forEach { entry ->
                    version(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }

        val librariesKey = ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(librariesKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogLibraries ->
            with(domainLibrary) {
                catalogLibraries.forEach { entry ->
                    library(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }
        this.catalog = catalog
    }
}

class ProjectCatalogPlugin : Plugin<Any> {

    companion object {
        internal const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_EXTENSION_NAME = "tezovCatalog"

        private val hasBeenApplyToRootProject = AtomicBoolean(false)

        internal const val CATALOG_KEY_LIBRARIES = "libraries"
        internal const val CATALOG_KEY_VERSIONS = "versions"
        internal const val CATALOG_KEY_PLUGINS_APPLY_TO = "plugins_apply_to"
    }

    override fun apply(any: Any) {
        when (any) {
            is Settings -> {
                VersionCheck.gradle(any, PLUGIN_CATALOG, CATALOG_PLUGIN_ID)
                val settingsExtension = any.extensions.create(
                    CATALOG_EXTENSION_NAME,
                    SettingsExtension::class.java,
                    any
                )
                val source = runCatching { System.getProperty(SYSTEM_PROP_CATALOG_SOURCE) }
                    .getOrNull() ?: run {
                    PLUGIN_CATALOG.throwException("catalog property not found. Add a valid property 'systemProp.catalog' in gradle.properties")
                }
                val catalog = buildCatalog(any, source)

                dsl(any, catalog)

                any.gradle.rootProject {
                    settingsExtension.catalog?.let { catalog ->
                        hasBeenApplyToRootProject.set(true)
                        extensions.create(
                            CATALOG_EXTENSION_NAME,
                            CatalogProjectExtension::class.java,
                            catalog
                        )
                    } ?: run {
                        PLUGIN_CATALOG.throwException(
                            this,
                            "catalog not found. Maybe you forgot the call tezovCatalogSource(value:String) in settings.gradle after having applied the plugin ?"
                        )
                    }
                    hasBeenApplyToRootProject.set(true)
                    extensions.create(
                        CATALOG_EXTENSION_NAME,
                        CatalogProjectExtension::class.java,
                        catalog
                    )
                }
            }

            is Project -> {
                if (!hasBeenApplyToRootProject.get()) {
                    PLUGIN_CATALOG.throwException(
                        any,
                        "$CATALOG_PLUGIN_ID must be applied in settings.gradle.kts of ${any.rootProject.name}"
                    )
                }
                any.extensions.create(CATALOG_EXTENSION_NAME, CatalogModuleExtension::class.java)
            }

            else -> {
                PLUGIN_CATALOG.throwException("unknown class ${any::class.java}. You must apply the plugin to the root project settings.gradle.kts")
            }
        }

    }

    private fun buildCatalog(settings: Settings, sourceCatalog: String): CatalogMap {
        val catalog = CatalogMap(
            settings = settings,
            catalogPointer = CatalogPointer.build(
                settings = settings,
                from = sourceCatalog
            )
        )

        val domainLibrary = settings
            .dependencyResolutionManagement
            .versionCatalogs
            .let { it.findByName(CATALOG_KEY_LIBRARIES) ?: it.create(CATALOG_KEY_LIBRARIES) }

        val versionsKey = CATALOG_KEY_VERSIONS + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(versionsKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogVersions ->
            with(domainLibrary) {
                catalogVersions.forEach { entry ->
                    version(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }

        val librariesKey = CATALOG_KEY_LIBRARIES + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(librariesKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogLibraries ->
            with(domainLibrary) {
                catalogLibraries.forEach { entry ->
                    library(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }
        return catalog
    }

    interface MyPolymorphicTypeContainer {
        fun getConn(): ExtensiblePolymorphicDomainObjectContainer<MyPolymorphicType>
    }

    interface MyPolymorphicType : Named {
        abstract class AlphaType @Inject constructor(
            private val name: String
        ) : MyPolymorphicType {
            abstract val stringProperty: Property<String>

            override fun getName() = name
        }

        abstract class BetaType @Inject constructor(
            private val name: String
        ) : MyPolymorphicType {
            abstract val intProperty: Property<Int>
            override fun getName() = name
        }

        abstract class ContainerType @Inject constructor(
            private val name: String,
            factory: ObjectFactory
        ) : MyPolymorphicType, ExtensionAware, MyPolymorphicTypeContainer {
            override fun getName() = name

            val con: ExtensiblePolymorphicDomainObjectContainer<MyPolymorphicType> =
                factory.polymorphicDomainObjectContainer(MyPolymorphicType::class.java)

            init {
                con.registerBinding(AlphaType::class.java, AlphaType::class.java)
                con.registerBinding(BetaType::class.java, BetaType::class.java)
                con.registerBinding(ContainerType::class.java, ContainerType::class.java)
                con.whenObjectAdded {
                    (con as ExtensionAware).extensions.add(name, this)
                }
            }

            override fun getConn() = con
        }
    }

    abstract class MyExtension @Inject constructor(
        val name:String,
        factory: ObjectFactory
    ) : ExtensionAware {
        val myContainer: ExtensiblePolymorphicDomainObjectContainer<MyPolymorphicType> =
            factory.polymorphicDomainObjectContainer(MyPolymorphicType::class.java)

        init {
            myContainer.registerBinding(MyPolymorphicType.AlphaType::class.java, MyPolymorphicType.AlphaType::class.java)
            myContainer.registerBinding(MyPolymorphicType.BetaType::class.java, MyPolymorphicType.BetaType::class.java)
            myContainer.registerBinding(MyPolymorphicType.ContainerType::class.java, MyPolymorphicType.ContainerType::class.java)
            myContainer.whenObjectAdded {
                (this as? MyPolymorphicTypeContainer)?.let {
                    (myContainer as ExtensionAware).extensions.add(name, getConn())
                } ?: kotlin.run {
                    (myContainer as ExtensionAware).extensions.add(name, this)
                }


            }
        }
    }


    private fun dsl(settings: Settings, catalog: CatalogMap) {
        settings.gradle.rootProject {
            val myExtension = objects.newInstance(MyExtension::class.java, "myExtension")
            extensions.add(myExtension.name, myExtension.myContainer)

            myExtension.myContainer.create("alphaValue", MyPolymorphicType.AlphaType::class.java) {
                stringProperty.set("I'm a property in AlphaType")
            }

            myExtension.myContainer.create("betaValue", MyPolymorphicType.BetaType::class.java) {
                intProperty.set(11)
            }

            val o = myExtension.myContainer.create("innerContainer", MyPolymorphicType.ContainerType::class.java) {



            }

            o.con.create("innerContainer2", MyPolymorphicType.ContainerType::class.java) {



            }




//            catalog.forEach { key, value ->

//                println("***")
//                println("$key : $value")

//                val keyChunk = key.split(KEY_SEPARATOR)
//                var parent = container
//                for(i in 0 until keyChunk.lastIndex){
//                    val poly =  parent.findByName(keyChunk[i])
//                    when (poly) {
//                        is PolymorphicType.Container -> {
//                            println("Container ${keyChunk[i]}")
//
//                            poly.container.register<PolymorphicType.Container>(keyChunk[i]) {
//                                parent = container
//                            }
//                        }
//                        is PolymorphicType.EndPoint -> {
//                            println("EndPoint ${keyChunk[i]}")
//
//                            //todo
//                        }
//                        else -> {
//                            println("New ${keyChunk[i]}")
//
//                            parent.register<PolymorphicType.Container>(keyChunk[i]) {
//                                parent = container
//                            }
//                        }
//                    }
//                }
//                if(parent.findByName(keyChunk[keyChunk.lastIndex]) == null){
//                    parent.register<PolymorphicType.EndPoint>(keyChunk[keyChunk.lastIndex]) {
//                        string = value
//                    }
//                }
//
//            }


        }

    }

}
