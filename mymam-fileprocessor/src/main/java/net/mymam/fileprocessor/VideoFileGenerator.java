/* MyMAM - Open Source Digital Media Asset Management.
 * http://www.mymam.net
 *
 * Copyright 2013, MyMAM contributors as indicated by the @author tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mymam.fileprocessor;

import net.mymam.data.json.FileProcessorTaskDataKeys;
import net.mymam.fileprocessor.exceptions.FileProcessingFailedException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fstab
 */
public class VideoFileGenerator {

    private final Config config;

    public VideoFileGenerator(Config config) {
        this.config = config;
    }

    public Map<String, String> generateProxyVideos(String rootDir, String origFile) throws FileProcessingFailedException {
        Path origPath = getOrigPath(rootDir, origFile);
        Path generatedDir = makeGeneratedDir(origPath);
        // The paremeters width and height are currently ignored when generating videos.
        Path mp4 = generateFile(config.get(Config.Var.CLIENT_CMD_GENERATE_LOWRES_MP4), origPath, generatedDir, "lowRes.mp4", 0, 0);
        Path webm = generateFile(config.get(Config.Var.CLIENT_CMD_GENERATE_LOWRES_WEBM), origPath, generatedDir, "lowRes.webm", 0, 0);
        Map<String, String> result = new HashMap<>();
        result.put(FileProcessorTaskDataKeys.LOW_RES_MP4, relPath(config, mp4, rootDir));
        result.put(FileProcessorTaskDataKeys.LOW_RES_WEMB, relPath(config, webm, rootDir));
        return result;
    }

    public Map<String, String> generateThumbnails(String rootDir, String origFile) throws FileProcessingFailedException {
        Path origPath = getOrigPath(rootDir, origFile);
        Path generatedDir = makeGeneratedDir(origPath);
        Path small = generateFile(config.get(Config.Var.CLIENT_CMD_GENERATE_IMAGE), origPath, generatedDir, "small.jpg", 100, 75);
        Path medium = generateFile(config.get(Config.Var.CLIENT_CMD_GENERATE_IMAGE), origPath, generatedDir, "medium.jpg", 200, 150);
        Path large = generateFile(config.get(Config.Var.CLIENT_CMD_GENERATE_IMAGE), origPath, generatedDir, "large.jpg", 400, 300);
        Map<String, String> result = new HashMap<>();
        result.put(FileProcessorTaskDataKeys.SMALL_IMG, relPath(config, small, rootDir));
        result.put(FileProcessorTaskDataKeys.MEDIUM_IMG, relPath(config, medium, rootDir));
        result.put(FileProcessorTaskDataKeys.LARGE_IMG, relPath(config, large, rootDir));
        result.put(FileProcessorTaskDataKeys.THUMBNAIL_OFFSET_MS, "" + 0L);
        return result;
    }

    private Path getOrigPath(String rootDir, String origFile) throws FileProcessingFailedException {
        Path origPath = Paths.get(config.get(Config.Var.CLIENT_MEDIAROOT), rootDir, origFile);
        if ( ! Files.isRegularFile(origPath) ) {
            throw new FileProcessingFailedException(origFile + " not found.");
        }
        return origPath;
    }

    private Path makeGeneratedDir(Path origPath) throws FileProcessingFailedException {
        Path generatedDir = Paths.get(origPath.getParent().toString(), "generated");
        try {
            Files.createDirectory(generatedDir);
            return generatedDir;
        }
        catch ( FileAlreadyExistsException e ) {
            // ignore
            return generatedDir;
        }
        catch ( Throwable t ) {
            throw new FileProcessingFailedException(t);
        }
    }

    private Path generateFile(String cmdLineTemplate, Path in, Path outDir, String outFile, int maxWidth, int maxHeight) throws FileProcessingFailedException {
        try {
            Path out = Paths.get(outDir.toString(), outFile);
            // TODO: Make sure that weired file names cannot be used to inject shell scripts, like '"; rm -r *;'
            String cmdLine = cmdLineTemplate
                    .replace("$INPUT_FILE", "\"" + in.toString() + "\"")
                    .replace("$OUTPUT_FILE", "\"" + out.toString() + "\"")
                    .replace("$MAX_WIDTH", Integer.toString(maxWidth))
                    .replace("$MAX_HEIGHT", Integer.toString(maxHeight));
            CommandLine cmd = CommandLine.parse(cmdLine);
            System.out.println("Executing " + cmd); // TODO: Use logging.
            new DefaultExecutor().execute(cmd);
            return out;
        }
        catch ( Throwable t ) {
            throw new FileProcessingFailedException(t);
        }
    }

    private String relPath(Config config, Path fullPath, String rootDir) {
        // throws InvalidPathException, IllegalArgumentException
        return Paths.get(config.get(Config.Var.CLIENT_MEDIAROOT), rootDir).relativize(fullPath).toString();
    }
}
