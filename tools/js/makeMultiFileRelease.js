"use strict";

// makeMultiFileRelease

// Script to copy all scripts of one project and setup version,
// to update the combined file distribution.

// Adapt VERSION below and execute the script, check output on the console.

#include <pjsr/DataType.jsh>

#define VERSION "1.5-test"
#define MAIN_FILE_NAME "FITSFileManager.js"

// This should be identical for all my projects
// The (relative) path of the directories of the sources
#define SOURCE_RELATIVE_DIR "../../main/js"
// The (relative) path of the directory that will contain the consolidate script
#define TARGET_RELATIVE_DIR "../../../releases"

(function (version, mainFileName) {

   // Return a full normalized path to the directory containing the parameter file (or directory)
   var getDirectoryOfFileWithDriveLetter = function getDirectoryOfFileWithDriveLetter( a_file_path )
   {
      let unix_path = File.windowsPathToUnix( a_file_path );
      let pathNormalized = File.fullPath(unix_path);
      let directoryWithDrive = File.extractDrive( pathNormalized ) + File.extractDirectory(pathNormalized);
      //Console.writeln("*** getDirectoryOfFileWithDriveLetter\n    a_file_path '" + a_file_path + "\n    unix_path '" + unix_path + "'\n    pathNormalized '" + pathNormalized + "' \n    directoryWithDrive '" + directoryWithDrive +"'");
      return directoryWithDrive;
   }

   // Return a full normalized path of a directory or a file, received in unix or windows format
   var getDirectoryWithDriveLetter = function getDirectoryWithDriveLetter( a_directory_path )
   {
      let unix_path = File.windowsPathToUnix( a_directory_path );
      let pathNormalized = File.fullPath(unix_path);
      //Console.writeln("*** getDirectoryWithDriveLetter\n    a_directory_path '" + a_directory_path + "'\n    unix_path '" + unix_path  + "'\n    pathNormalized '" + pathNormalized +"'");
      return pathNormalized;
   }

   // Directories are relative to the directory of the current script
   var releaseScriptFilePath = #__FILE__ ;
   var releaseScriptDir = getDirectoryOfFileWithDriveLetter(releaseScriptFilePath);

   var sourceDir = getDirectoryWithDriveLetter(releaseScriptDir + "/" + SOURCE_RELATIVE_DIR);
   var targetRootDirectory = getDirectoryWithDriveLetter(releaseScriptDir + "/" + TARGET_RELATIVE_DIR);

   // Read a file, return it as a list of lines
   var readTextFile = function(filePath) {
      if (!File.exists(filePath)) {
         throw "File not found: " +  File.fullPath(filePath);
      }
      let file = new File;
      file.openForReading(filePath);
      let buffer = file.read( DataType_ByteArray, file.size );
      file.close();

      let lines = buffer.toString().split( '\n' );
      return lines;
   }

   Console.writeln("Make release for '" + mainFileName + "', version '" + version  + "'");
   if (!File.directoryExists(targetRootDirectory)) {
      throw "Target directory not found: " +  File.fullPath(targetRootDirectory);
   }
   var mainFileNameOnly = File.extractName(mainFileName); // Name without extension
   var targetDirectory = targetRootDirectory + "/" + mainFileNameOnly + "-" + version;
   if (File.directoryExists(targetDirectory)) {
      Console.writeln("  ** ERROR ** Target directory '" + targetDirectory +"' already exists, cannot overwrite.");
      return ;
   }
   Console.writeln("  Creating target directory '" + targetDirectory +"'");
   File.createDirectory(targetDirectory,false);
 
 
   var includeRegExp1 = new RegExp("#include[ \\t]\"+(" + mainFileNameOnly + "-.+)\"");
   var includeRegExp2 = new RegExp("#include[ \\t]\"+(PJSR-logging\.jsh)\"");
   // var includeRegExp = /#include[ \t]"+(VaryParams-.+)"/;
   Console.writeln("  looking up include as " + includeRegExp1 + " or " + includeRegExp2);

   // For some reason adding the N makes the regexp not working....
   var defineVersionRegExp = /^#define[ \t]+VERSIO/;

   // To detect the title
   var defineTitleRegExp = /^#define[ \t]+TITLE[ \t]+(.+)/;

   // Log that we have a title
   var defineDebugRegExp = /^#define[ \t]+DEBUG(.*)/;


   var hasTitle = false;
   var hasVersion = false;

   var filesToCopy = {};

   var copyFile = function copyFile(sourceDirectory, sourceFileName, targetDirectory)
   {
      // Avoid recursive copy
      filesToCopy[sourceFileName] = true;


      let sourceFilePath = sourceDirectory + "/" + sourceFileName;
      let targetFilePath = targetDirectory  + "/" + sourceFileName;

      let sourceFileText = readTextFile(sourceFilePath);
      Console.writeln(sourceFileName + " has " + sourceFileText.length + " lines") ;

      var targetFile = new File;
      targetFile.createForWriting(targetFilePath);

      let extension = File.extractExtension(sourceFileName);
      let expandJSsource = extension.startsWith(".js");
      Console.writeln("  File " + sourceFileName + " has extension " + extension + ", " + (expandJSsource ? "expanding directives" : "copying as is"));

      var anyMessage = true;

      for (var i=0; i<sourceFileText.length; i++) {
         // To show log on console
         if (anyMessage) {
            Console.flush();
            processEvents();
            anyMessage = false;
         }

         // Copy the text, adding the included files to the lisz of files to include and updating the VERSION string
         let line = sourceFileText[i];
         // Remove terminator characters
         line = line.replace(/[\r\n]*/g,"");

         if (expandJSsource) {
            
            let match = line.match(includeRegExp1);
            if (!match) match = line.match(includeRegExp2);
            if (match) {
               // Found an #include
               Console.writeln("  handling '"+ line + "'");
               anyMessage = true;
               
               let includedFileName = match[1];
               if (includedFileName in filesToCopy)
               {
                  Console.writeln("    File '" + includedFileName + "' already in list");
               } else {
                  Console.writeln("    File '" + includedFileName + "' to be added in copy list");
                  filesToCopy[includedFileName] = false;
               }
               // The #include statement must be kept
               targetFile.outTextLn(line);
   
            } else if (defineVersionRegExp.test(line)) {
               hasVersion = true;
               anyMessage = true;
               // Update version
               Console.writeln("  handling '" + line + "'");
               Console.writeln("     writing updated version " + "#define VERSION \"" + version + "\"");
               targetFile.outTextLn("// Release version " + version + " created by makeRelease on " + new Date() );
               targetFile.outTextLn("#define VERSION \"" + version + "\"");

            } else if (defineDebugRegExp.test(line)) {
               // Disable DEBUG
               anyMessage = true;
               Console.writeln("  handling '" + line + "'");
               Console.writeln("     Disabling DEBUG"); 
               targetFile.outTextLn("// " + line);

            } else if (defineTitleRegExp.test(line)) {
               hasTitle = true;
               anyMessage = true;
               // Show title
               Console.writeln("  handling '" + line + "'");
               Console.writeln("     TITLE is defined"); //  as '" + TITLE + "'");
               targetFile.outTextLn(line);

            } else {
               // No special handling for this line
               targetFile.outTextLn(line);
            }

         } else {
            // No expansion for this file, copy the line
            targetFile.outTextLn(line);
         }

      }
      targetFile.close();
      Console.writeln("  File '" + sourceFileName + "' copied.")

   }


   filesToCopy[mainFileName] = false;
   // Add all mandatory files
   filesToCopy['change-log.txt'] = false;
   filesToCopy['copyright-info.txt'] = false;
   filesToCopy['product-info.txt'] = false;

   // Add ffm-configs files
   filesToCopy['Default.ffm-configs'] = false;
   filesToCopy['iTelescope.ffm-configs'] = false;
   filesToCopy['UserCAHA.ffm-configs'] = false;

   var anyLoaded = true;
   while (anyLoaded) 
   {
      anyLoaded = false;
   
      var fileToLoad = null;
      for (var fileName in filesToCopy) {
         let toLoad = ! filesToCopy[fileName];
         if (toLoad) {
            fileToLoad = fileName;
            break;
         }
      }
      if (fileToLoad != null) {
         copyFile(sourceDir, fileToLoad, targetDirectory);
         anyLoaded = true;
      }
   }


   if (!hasTitle) {
      Console.writeln("** ERROR ** #define TITLE is missing");
   }
   if (!hasVersion) {
      Console.writeln("** ERROR ** #define VERSION is missing");
   }

   Console.writeln("Kit '" + targetDirectory + "' created.");


})(VERSION, MAIN_FILE_NAME);

