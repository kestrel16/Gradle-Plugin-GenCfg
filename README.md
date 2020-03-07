# Gradle-Plugin-GenCfg

This project addresses the need to manage configuration files (.yaml, .json, .properties, etc.) across multiple environments.  The approach is to leverage a template for a given type of configuration file, the template file includes tokens that would be replaced as the environment specific files are generated, the template file and the .json file that specifies environments and maps tokens/values are kept under version control.  The implementation is a Gradle plugin (**gencfgplugin**) which takes three parameters:  absolute path of the template file, the absolute path to a properties (.json) file that enumerates the environments and maps tokens to values, and the absolute path/name of the generated file.     

####Sample build.gradle:

	buildscript {
	     repositories {
	         mavenLocal()
	     }
	 dependencies {
	    classpath group: 'com.vtxii.gradle.plugin',    // Defined in the build.gradle of the plugin
	              name: 'gencfgplugin',       // Defined by the rootProject.name 
	              version: '1.0'
	    }
	 }
	
	import com.vtxii.gradle.plugin.GenCfgTask
	
	tasks.register("yaml", GenCfgTask) {
	    templatePath = '/Users/andyabrams/workspace/testproject/template.yaml'
	    propertiesPath = '/Users/andyabrams/workspace/testproject/tokenvalue.json'
	    targetPath = '/Users/andyabrams/workspace/testproject/my.yaml'
	}
	
Assumes plugin is in a Maven repo.

####Sample Properties File (.json):

	{
		"environments": ["dev", "sit", ""],
		"tokenValueMap" : {
			"podName": "dapod"
		}
	}
	
Environment value of "" is used for production.