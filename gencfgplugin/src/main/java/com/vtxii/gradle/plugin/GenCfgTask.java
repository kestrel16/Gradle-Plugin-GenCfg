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
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Input;

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
        System.out.println("Template Path:  " + this.templatePath);
        System.out.println("Target Path:  " + this.targetPath);
        System.out.println("Properties Path:  " + this.propertiesPath);
        
        // Convert string path to Path objects
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
				tokenValueMap.put("env", environment);
				Path environmentTargetPath = genEnvironmentTargetFilePath(environment, targetPath);
				genTargetFile(templatePath, environmentTargetPath, tokenValueMap);
			}
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Decompose the target file path and put it back together with _environment added
     * @param environment String containing the name of the environment.
     * @param targetPath Path of the target file as originally specified. 
     * @return An environment specific target file path.
     */
    private Path genEnvironmentTargetFilePath(String environment, Path targetPath) {
    	Path filename = targetPath.getFileName();
    	String[] split = filename.toString().split("\\.");
    	String name = split[0];
    	String extension = split[1];
    	Path parent = targetPath.getParent();
    	Path root = targetPath.getRoot();
    	Path environmentTargetPath = root.resolve(parent).resolve(name + "_" + environment + "." + extension);
		return environmentTargetPath;
    }
    
    /**
     * Reads properties per JSON format and puts the results into a map.
     * @param propertiesPath Path to the .json file.
     * @return Resulting JSON in a map.
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
     * @param templatePath  Path to the template file that has tokens needing to be replaced by values.
     * @param targetPath Path to the file that has tokens replaced with values.
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
     * 
     * @param line
     * @param tokenValueMap
     * @return
     * @throws Exception
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