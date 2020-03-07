# Gradle-Plugin-GenCfg

This project addresses the need to manage configuration files (.yaml, .json, .properties, etc.) across multiple environments.  The approach is to leverage a template for a given type of configuration file, the template file includes tokens that would be replaced as the environment specific files are generated, the template file and the .json file that specifies environments and maps tokens/values are kept under version control.  The implementation is a Gradle plugin (**gencfgplugin**) which takes three parameters:  absolute path of the template file, the absolute path to a properties (.json) file that enumerates the environments and maps tokens to values, and the absolute path/name of the generated file.     

#### Sample build.gradle:

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
#### Sample Template File (.yaml):
	apiVersion: apps/v1
	kind: Deployment
	metadata:
	  name: [name]-deployment
	  labels:
	    app: [name]
	spec:
	  selector:
	    matchLabels:
	      app: [name]
	  template:
	    metadata:
	      labels:
	        app: [name]
	    spec:
	      containers:
	      - name: [name]-container
	        image: [name]:[version]
	        ports:
	        - containerPort: [port]
	---
	apiVersion: v1
	kind: Service
	metadata:
	  name: [name]-service
	spec:
	  type: NodePort
	  selector:
	    app: [name]
	  ports:
	  - protocol: TCP
	    port: [port]
	    targetPort: [port]
	    nodePort: [node-port]
#### Sample Properties File (.json):
	{
		"environments": ["dev", "sit", ""],
		"tokenValueMap" : {
			"name": "system",
			"version": "1.0-SNAPSHOT",
			"port": "9080",
			"node-port": "3100"
		}
	}
Environment value of "" is used for production.