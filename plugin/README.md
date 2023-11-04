# Tezov plugin project - Plugin

This folder is the plugin source code.


Roadmap :
 - find a way to not be forced to set the catalog source in gradle.properties but in setting.gradle.kts
 - make my own domain variables to not vampirize the gradle catalog
   - If I can find a way to make my own domain variable, it should be possible to have all 
custom variables works too. Right now, we are forced to used them with tezovCatalog.string("xxx")
because the gradle catalog do not allow custom any variables. We can just have libraries with 
perfect coordinates.
 - last improvement, will be to find a way to apply a plugin in gradle with the version coming from
the catalog yaml/json/toml. Right now, that the version of the classpath define in setting.gradle.kts