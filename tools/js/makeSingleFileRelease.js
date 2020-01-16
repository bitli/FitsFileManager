"use strict";

// makeSingleFileRelease

// Script to consolidate all scripts of one project and setup version,
// to make a single file easier for distribution.

// Adapt VERSION below and execute the script, check output on the console.

#include <pjsr/DataType.jsh>

#define VERSION "1.5-test-2"
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
   var sourceMainFilePath = getDirectoryWithDriveLetter(sourceDir + "/" + mainFileName);
   var targetDirectory = getDirectoryWithDriveLetter(releaseScriptDir + "/" + TARGET_RELATIVE_DIR);

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
   if (!File.directoryExists(targetDirectory)) {
      throw "Target directory not found: " +  File.fullPath(targetDirectory);
   }
   var mainFileNameOnly = File.extractName(mainFileName); // Name without extension
   var mainFileExtension = File.extractExtension(mainFileName);

   var targetFileName = mainFileNameOnly + "-" + version + mainFileExtension;
   var targetFilePath = targetDirectory + "/" + targetFileName;
   Console.writeln("  Target directory: " + targetDirectory);
   Console.writeln("  Target file name: " + targetFileName);


   var rootFileText = readTextFile(sourceMainFilePath);
   Console.writeln(mainFileName + " has " + rootFileText.length + " lines") ;

   var targetFile = new File;
   targetFile.createForWriting(targetFilePath);

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

   var anyMessage = true;
   for (var i=0; i<rootFileText.length; i++) {
      // To show log on console
      if (anyMessage) {
         Console.flush();
         processEvents();
         anyMessage = false;
      }

       // Copy the text, adding the included files and updating the VERSION string
      let line = rootFileText[i];
      // Remove terminator characters
      line = line.replace(/[\r\n]*/g,"");
      let match = line.match(includeRegExp1);
      if (!match) match = line.match(includeRegExp2);
      if (match) {
         // Found an #include
         Console.writeln("  handling '"+ line + "'");
         anyMessage = true;
         
         let inludedFilePath = sourceDir + "/" + match[1];
         let includedText = readTextFile(inludedFilePath);
         Console.writeln("     including " + includedText.length + " lines from '" + match[1] + "'");
         targetFile.outTextLn("//================================================================");
         targetFile.outTextLn("// " + line.substring(1));
         targetFile.outTextLn("//================================================================");
         for (let j=0; j<includedText.length; j++) {
            // Remove terminator characters
            line = includedText[j].replace(/[\r\n]*/g,"");
            if (defineDebugRegExp.test(line)) {
               // Disable DEBUG
               Console.writeln("    handling '" + line + "'");
               Console.writeln("       Disabling DEBUG"); 
               targetFile.outTextLn("// " + line);
            } else {
               targetFile.outTextLn(line);
            }
         }
         targetFile.outTextLn("//==== end of include ============================================");

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
         targetFile.outTextLn(line);
      }

   }

   if (!hasTitle) {
      Console.writeln("#define TITLE is missing");
   }
   if (!hasVersion) {
      Console.writeln("#define VERSION is missing");
   }

   targetFile.close();
   Console.writeln("File '" + targetFilePath + "' created.");


})(VERSION, MAIN_FILE_NAME);

