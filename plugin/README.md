# Tezov plugin project - Plugin

This folder is the plugin source code.

Roadmap :
 - make my own domain variables to not vampirize the gradle catalog
   - gradle catalog is limited to valid dependencies coordinates. Hence it can't be use for anything else. By moving with my own domain variables of type safe accessor, I could allow any thing (aka: projectVersion, ect...)
 - last improvement, will be to find a way to apply a plugin in gradle with the version coming from
the catalog yaml/json/toml. Right now, that the version of the classpath define in setting.gradle.kts. Same though to auto apply the plugin is setting with friendly accessor.