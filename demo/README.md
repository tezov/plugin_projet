# Tezov plugin project - Demo

This folder is the application demo to show how to use the Tezov plugin project and Tezov plugin catalog.

There are 3 bases catalog named tezov.catalog (json, toml and yaml). You can switch from one to another one by changing systemProp.catalog in properties.gradle file.
- These 3 bases catalog have one place holder inclusion of another catalog "tezov.catalog.dependencies.yaml"
- These 3 bases catalog have plugin defined by module (app and lib) to be auto applied.

In each build.gradle.kts of each module, you can see the use of the catalog and the android application/library config.

