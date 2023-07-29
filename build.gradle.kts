// uncomment to try plugin project demo

//plugins {
//    id("com.tezov.plugin_project.catalog")
//}
//
//tezovCatalog {
//
////    verboseCatalogBuild = true
////    verbosePluginApply = true
////    verboseReadValue = true
////    verboseCheckDependenciesVersion = true
//    jsonFile = jsonFromFile("F:/android_project/plugin_project_demo/tezov.catalog.json")
////    jsonFile = jsonFromUrl("https://www.tezov.com/tezov.catalog.json")
//    configureProjects()
//
//    val ignore_alpha = false
//    val ignore_beta = false
//    val ignore_rc = false
//    with("projectPath.dependencies"){
//        with("core"){
//            checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
//        }
//        with("compose"){
//            checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
//        }
//    }
//    with("projectPath.dependencies_debug"){
//        checkDependenciesVersion(ignore_alpha, ignore_beta, ignore_rc)
//    }
//
//}