package com.vtxii.gradle.plugin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Input;

/**
 * Implements the configuration file generation - creating environment specific file
 * by reading a template file, replacing tokens with desired values, writing the results
 * to a file with the name extended to indicate the environment associated with the file.
 * This Task class is also referenced by the plugin class, as well as the class that will be 
 * used when registering tasks, i.e., it can be reused for defining similar tasks.
 * 
 * @author andyabrams
 *
 */
public class GenCfgTask extends DefaultTask {
	private String templatePath;
	private String targetPath;
	private String propertiesPath;
	
    @Input
    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }
    
    @Input
    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    @Input
    public String getPropertiesPath() {
        return propertiesPath;
    }

    public void setPropertiesPath(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    @TaskAction
    public void myTask() {
    	
    	// Validate parameters Convert string path to Path objects
    	validateParameters(this.templatePath, this.targetPath, this.propertiesPath);
        Path templatePath = Paths.get(this.templatePath);
        Path targetPath = Paths.get(this.targetPath);
        Path propertiesPath = Paths.get(this.propertiesPath);
        
        try { 
        	// Get list of environments and token-to-value map from JSON
			HashMap<String,Object> jsonMap = getJsonMap(propertiesPath);
			@SuppressWarnings("unchecked")
			ArrayList<String> environments = (ArrayList<String>)jsonMap.get("environments");
			@SuppressWarnings("unchecked")
			HashMap<String, String> tokenValueMap = (HashMap<String, String>)jsonMap.get("tokenValueMap");
			
			// Loop over environments creating environment target files
			for(String environment : environments)
			{
				// Add the environment to the map 
				tokenValueMap.put("env", environment);
				
				// Do the work
				Path environmentTargetPath = genEnvironmentTargetFilePath(environment, targetPath);
				genTargetFile(templatePath, environmentTargetPath, tokenValueMap);
			}
			
		} catch (JsonParseException e) {
			System.out.println(e.getMessage());
			throw new InvalidUserDataException(e.getMessage());
		} catch (JsonMappingException e) {
			System.out.println(e.getMessage());
			throw new InvalidUserDataException(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			throw new InvalidUserDataException(e.getMessage());
		} catch (NoSuchFieldException e) {
			System.out.println(e.getMessage());
			throw new InvalidUserDataException(e.getMessage());
		}
    }

    /**
     * Make sure we have all of the parameters, that they are not null, 0 length,
     * or are not absolute paths
     * @param templatePath Absolute path to the template file that has tokens needing to be replaced by values.
     * @param targetPath Absolute path to the file that has tokens replaced with values.
     * @param propertiesPath Absolute path to the .json file.
     * @throws InvalidUserDataException Exception if any of the values is invalid.
     */
    private void validateParameters(String templatePath, String targetPath, 
    		String propertiesPath) throws InvalidUserDataException {
    	if (templatePath == null || templatePath.length() == 0 || !Paths.get(templatePath).isAbsolute()) {
    		throw new InvalidUserDataException("invalid template path");
    	}
        if (targetPath == null || targetPath.length() == 0 || !Paths.get(targetPath).isAbsolute()) {
        		throw new InvalidUserDataException("invalid target path");
    	}
        if (propertiesPath == null || propertiesPath.length() == 0 || !Paths.get(propertiesPath).isAbsolute()) {
    		throw new InvalidUserDataException("invalid properties path");
        }
	}
    
    /**
     * Decompose the target file path and put it back together with _environment added
     * @param environment String containing the name of the environment.
     * @param targetPath Absolute path of the target file as originally specified. 
     * @return An environment specific target file path.
     */
    private Path genEnvironmentTargetFilePath(String environment, Path targetPath) {
    	
    	// Take into account that the environment can be empty, which is equivalent to prod
    	if (environment.equals(""))
    		return targetPath;
    	
    	// Break the path into component parts
    	Path filename = targetPath.getFileName();
    	String[] split = filename.toString().split("\\.");
    	String name = split[0];
    	String extension = split[1];
    	Path parent = targetPath.getParent();
    	Path root = targetPath.getRoot();
    	
    	// Put the path back together, adding _ and environment
    	Path environmentTargetPath = root.resolve(parent).resolve(name + "_" + environment + "." + extension);
		return environmentTargetPath;
    }
    
    /**
     * Reads properties per JSON format and puts the results into a map.
     * @param propertiesPath  Absolute path to the .json file.
     * @return Resulting JSON as a map.
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    private HashMap<String,Object> getJsonMap(Path propertiesPath) throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper objectMapper = new ObjectMapper();
    	InputStream input = new FileInputStream(propertiesPath.toString());
    	HashMap<String, Object> jsonMap = objectMapper.readValue(input,
    		    new TypeReference<HashMap<String,Object>>(){});
		return jsonMap;
    }
    
    /**
     * Takes a template file and token/value pairs and generates a target file with tokens replaced with values.
     * @param templatePath  Absolute path to the template file that has tokens needing to be replaced by values.
     * @param targetPath Absolute path to the file that has tokens replaced with values.
     * @param tokenValueMap Map of tokens with corresponding values.
     * @throws IOException
     * @throws NoSuchFieldException
     */
    private void genTargetFile(Path templatePath, Path targetPath, HashMap<String, String> tokenValueMap) throws IOException, NoSuchFieldException {
        try (
        		BufferedReader templateStream = new BufferedReader(new FileReader(templatePath.toString()));
        		PrintWriter targetStream = new PrintWriter(new FileWriter(targetPath.toString()));
         ){
            String line;
            String outputLine;
            while ((line = templateStream.readLine()) != null) {
            	outputLine = genTargetFileLine(line, tokenValueMap);
                targetStream.println(outputLine);
            }
        } 
    }
    
    /**
     * Creates a new string with tokens replaced by values.
     * @param line String containing tokens that will be replaced
     * @param tokenValueMap Map of tokens to values.
     * @return String where tokens have been replaced.
     * @throws NoSuchFieldExceptionon Exception will be generated if a token is identified 
     * and there is no corresponding value in the map
     */
    private String genTargetFileLine(String line, HashMap<String,String> tokenValueMap) throws NoSuchFieldException {
        
    	// Parse the line looking for tokens
    	Pattern pattern = Pattern.compile("\\[(.+?)\\]");
        Matcher matcher = pattern.matcher(line);
        
        // Loop through all of the tokens; replacing tokens in the line with values, leveraging an index, 
        // start, end; building a new output string
        StringBuilder builder = new StringBuilder();
        int idx = 0;
        String value;
        String token;
        while (matcher.find()) {
        	token = matcher.group(1);
        	value = tokenValueMap.get(token);
        	
        	// Cannot have a token without a value
        	if (value == null) {
        		throw new NoSuchFieldException("no corresponding value for token " + token);
        	}
        	
        	builder.append(line.substring(idx, matcher.start()));
            builder.append(value);
            idx = matcher.end();
        }
        
        // Finish building the output string
        builder.append(line.substring(idx, line.length()));
        
        return builder.toString();
    }
}