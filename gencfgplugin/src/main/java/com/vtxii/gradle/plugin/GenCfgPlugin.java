package com.vtxii.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Implements Plugin interface, referencing the Task class GenCfgTask.
 * 
 * @author andyabrams
 *
 */
public class GenCfgPlugin implements Plugin<Project>{

	@Override
	public void apply(Project project) {
        project.getTasks().create("gencfgTask", GenCfgTask.class);
		
	}
}