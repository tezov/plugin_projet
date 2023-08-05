//import kotlin.io.path.Path
//
//// uncomment to try plugin project demo
//
//plugins {
//    id("com.tezov.plugin_project.catalog")
//}
//
//tezovCatalog {
//
//    catalogFile = catalogFromUrl(Path("${project.projectDir}", "/tezov.catalog.yaml").toString())
//    catalogFile = catalogFromUrl(Path("${project.projectDir}", "/tezov.catalog.json").toString())
//    catalogFile = catalogFromUrl(Path("${project.projectDir}", "/tezov.catalog.toml").toString())

////    catalogFile = catalogFromUrl("https://www.tezov.com/tezov.catalog.json")

//
//    configureProjects()
//
//    val ignore_alpha = false
//    val ignore_beta = false
//    val ignore_rc = false
////    with("projectPath.dependencies"){
////        with("core"){
////            checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
////        }
////        with("compose"){
////            checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
////        }
////    }
////    with("projectPath.dependencies_debug"){
////        checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
////    }
//
//}