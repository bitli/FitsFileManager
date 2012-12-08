// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js



var ffM_GUI_RuleSet = (function (){

var makeVertSection = function() {
   var i
   var sizer = new VerticalSizer;
   sizer.margin = 4;
   sizer.spacing = 4;
   for (i=0; i<arguments.length; i++) {
      sizer.add( arguments[i] );
   }
   return sizer;
}


var makeRuleSetSelection_ComboBox = function(parent, ruleSetNames) {
   var i;
   var comboBox = new ComboBox( parent );
   comboBox.toolTip = Text.H.GROUP_TEMPLATE_TOOLTIP;
   comboBox.enabled = true;
   comboBox.editEnabled = false;
   for (i=0; i<ruleSetNames.length;i++) {
      comboBox.addItem(ruleSetNames[i]);
   }
   comboBox.currentItem = 0;


   comboBox.onItemSelected = function() {
      Console.writeln("SELECTED " + this.currentItem);
   }

   return comboBox;
}

// buttons is an array of {icon, toolTip, action}
var makeIconButtonBar = function(parent, buttons) {
   var i, button;
   var horizontalSizer = new HorizontalSizer;
   horizontalSizer.margin = 6;
   horizontalSizer.spacing = 4;

   for (i=0; i<buttons.length;i ++) {
      var button = buttons[i];
      Log.debug(i,button.icon, button.toolTip);
      var toolButton = new ToolButton( parent );
      toolButton.icon = new Bitmap( button.icon );
      toolButton.toolTip = button.toolTip;
      toolButton.onClick = button.action;
      horizontalSizer.add(toolButton);
   }
   horizontalSizer.addStretch();
   return horizontalSizer;

}


function makeManagedListTreeBox(parent, listModel) {
   var i;

   var makeNode = function(treeNode, nodeModel, index) {
      var node = new TreeBoxNode( treeBox, index);
      node.setText( 0, nodeModel );
      return node;
   }

   var treeBox = new TreeBox( parent);

   treeBox.rootDecoration = false;
   treeBox.numberOfColumns = 1;
   treeBox.multipleSelection = false;
   treeBox.headerVisible = false;
   treeBox.headerSorting = false;
   treeBox.sort(0,true);
   //treeBox.setMinSize( 700, 200 );
   treeBox.toolTip = Text.H.FILES_TREEBOX_TOOLTIP;


   for (var i=0; i<listModel.length; i++) {
      treeBox.add(makeNode(treeBox, listModel[i], i));
   }


   var upAction = function() {
      var nodeToMove,  nodeIndex;
      if (treeBox.selectedNodes.length>0) {
         nodeToMove = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToMove);
         if (nodeIndex>0) {
            treeBox.remove(nodeIndex);
            treeBox.insert(nodeIndex-1,nodeToMove);
            treeBox.currentNode = nodeToMove;
         }
      }
   }
   var downAction = function() {
      var nodeToMove,  nodeIndex;
      if (treeBox.selectedNodes.length>0) {
         nodeToMove = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToMove);
         if (nodeIndex<treeBox.numberOfChildren-1) {
            treeBox.remove(nodeIndex);
            treeBox.insert(nodeIndex+1,nodeToMove);
            treeBox.currentNode = nodeToMove;
         }
      }
   }
   var addAction = function() {
      var nodeBeforeNew, newNode;
      var nodeIndex = treeBox.numberOfChildren;
      if (treeBox.selectedNodes.length>0) {
         nodeBeforeNew = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeBeforeNew) + 1;
      }
      Log.debug("ni",nodeIndex);
      // Create and inserted node
      newNode = makeNode(treeBox, "newnode " + new Date(), nodeIndex);
      treeBox.currentNode = newNode;
   }

   var deleteAction = function() {
      var nodeToDelete,  nodeIndex;
      if (treeBox.selectedNodes.length>0) {
         nodeToDelete = treeBox.selectedNodes[0];
         nodeIndex = treeBox.childIndex(nodeToDelete);
         treeBox.remove(nodeIndex);
      }
   }

   var buttons = [
      {
         icon: ":/images/interface/prevButton.png",
         toolTip: "Move item up",
         action: upAction
         },
      {
         icon: ":/images/interface/nextButton.png",
         toolTip:  "Move item down",
         action: downAction
      },
      {
         icon: ":/images/icons/add.png",
         toolTip:  "Add new item",
         action: addAction
      },
      {
         icon: ":/images/icons/cancel.png",
         toolTip:  "Delete item",
         action: deleteAction
      },
   ];


   var iconButtonBar = makeIconButtonBar(parent, buttons);

   var sizer = makeVertSection(treeBox, iconButtonBar);
   return sizer;

}




// ========================================================================================================================
// Configuration dialog
// ========================================================================================================================
// ---------------------------------------------------------------------------------------------------------
function ConfigurationDialog( parentDialog, ruleSet) {
   this.__base__ = Dialog;
   this.__base__();

   this.ruleSet = ruleSet;

   var ruleSetNames = ["aaa","bbb","ccc"];



   this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;

   var ruleSetSelection_ComboBox = makeRuleSetSelection_ComboBox(this, ruleSetNames);


   var variableList = makeManagedListTreeBox(this, ruleSetNames);

   this.sizer = makeVertSection(ruleSetSelection_ComboBox, variableList);

   this.setVariableSize();
   this.adjustToContents();



#ifdef NOTDEF
   // Mapping of variable 'logical keyword use' to FITS keyword names
   var keywordNames_GroupBox = new GroupBox(this);

   keywordNames_GroupBox.title = Text.T.KEYWORDNAMES_GROUPBOX_TITLE;
   keywordNames_GroupBox.sizer = new VerticalSizer;

   var keywordNames_TreeBox = new TreeBox(this);
   keywordNames_TreeBox.rootDecoration = false;
   keywordNames_TreeBox.numberOfColumns = 2;
   keywordNames_TreeBox.headerVisible = false;
   keywordNames_TreeBox.toolTip = Text.H.KEYWORDNAMES_GROUPBOX_TOOLTIP


   var refreshRemappedFITSkeywordsNames = function (keywordNames_TreeBox) {
      keywordNames_TreeBox.clear();
      var remappedFITSkeywordsNames = Object.keys(engine.remappedFITSkeywords);
      for (var ic=0; ic<remappedFITSkeywordsNames.length; ic++) {
         var node = new TreeBoxNode(keywordNames_TreeBox);
         node.setText( 0, remappedFITSkeywordsNames[ic] );
         node.setText( 1, engine.remappedFITSkeywords[remappedFITSkeywordsNames[ic]] );
         node.checkable = false;
      }
   }

   refreshRemappedFITSkeywordsNames(keywordNames_TreeBox);
   keywordNames_GroupBox.sizer.add(keywordNames_TreeBox);


   // Conversion of type names
   var typeConversion_GroupBox = new GroupBox(this);
   typeConversion_GroupBox.sizer = new VerticalSizer;

   typeConversion_GroupBox.title = Text.T.TYPECONVERSION_GROUPBOX_TITLE;
   typeConversion_GroupBox.toolTip = Text.H.TYPECONVERSION_GROUPBOX_TOOLTIP;

   var typeConversion_TreeBox = new TreeBox(this);
   typeConversion_TreeBox.rootDecoration = false;
   typeConversion_TreeBox.numberOfColumns = 2;
   typeConversion_TreeBox.headerVisible = false;

   var refreshTypeConversions = function (typeConversion_TreeBox) {
      typeConversion_TreeBox.clear();
      var typeConversions = guiParameters.getCurrentConfiguration().typeConversions;
      for (var ic=0; ic<typeConversions.length; ic++) {
         var node = new TreeBoxNode(typeConversion_TreeBox);
         node.setText( 0, typeConversions[ic][0].toString() );
         node.setText( 1, typeConversions[ic][1] );
         node.checkable = false;
      }
   }
   refreshTypeConversions(typeConversion_TreeBox);
   typeConversion_GroupBox.sizer.add(typeConversion_TreeBox);


   // Conversion of filter names
   var filterConversion_GroupBox = new GroupBox(this);
   filterConversion_GroupBox.title = Text.T.FILTERCONVERSION_GROUPBOX_TITLE;
   filterConversion_GroupBox.toolTip = Text.H.FILTERCONVERSION_GROUPBOX_TOOLTIP;
   filterConversion_GroupBox.sizer = new VerticalSizer;


   var filterConversion_TreeBox = new TreeBox(this);
   filterConversion_TreeBox.rootDecoration = false;
   filterConversion_TreeBox.numberOfColumns = 2;
   filterConversion_TreeBox.headerVisible = false;
   var refreshFilterConversions = function (filterConversion_TreeBox) {
      filterConversion_TreeBox.clear();
      var filterConversions = guiParameters.getCurrentConfiguration().filterConversions;
      for (var ic=0; ic<filterConversions.length; ic++) {
         var node = new TreeBoxNode(filterConversion_TreeBox);
         node.setText( 0, filterConversions[ic][0].toString() );
         node.setText( 1, filterConversions[ic][1] );
         node.checkable = false;
      }
   }
   refreshFilterConversions(filterConversion_TreeBox);
   filterConversion_GroupBox.sizer.add(filterConversion_TreeBox);

   // Selection of mapping rules
   var mappingRules_ComboBox = new ComboBox( this );
   mappingRules_ComboBox.toolTip = "Select rules";
   mappingRules_ComboBox.enabled = true;
   var mappingList = ffM_Configuration.configurationList;
   for (var it = 0; it<mappingList.length; it++) {
      mappingRules_ComboBox.addItem( mappingList[it] +  " - " + ffM_Configuration.getConfigurationByName(mappingList[it]).description);
   }
   mappingRules_ComboBox.currentItem = guiParameters.currentConfigurationIndex;

   mappingRules_ComboBox.onItemSelected = function() {
      debug("mappingRules_ComboBox: onItemSelected " + this.currentItem );
       if (this.currentItem >= mappingList.length) {
          return; // Protect against CR in input field
       }
       guiParameters.currentConfigurationIndex = this.currentItem;
       engine.setConfiguration(guiParameters.getCurrentConfiguration());

       refreshRemappedFITSkeywordsNames(keywordNames_TreeBox);
       refreshTypeConversions(typeConversion_TreeBox);
       refreshFilterConversions(filterConversion_TreeBox);
//     somehow show the current configuration
//       this.dialog.barConversions.setCollapsedTitle(Text.T.REMAPPING_SECTION_PART_TEXT + " - " + ffM_Configuration.configurationList[guiParameters.currentConfigurationIndex]);

      // If the rules are changed, all variables must be recalculated
      // TODO RECALCULATE VARIABLES
      // TODO We can probably clear in one go
//      for ( var i = this.dialog.filesTreeBox.numberOfChildren; --i >= 0; ) {
//            this.dialog.filesTreeBox.remove( i );
//      }
//      this.dialog.engine.reset();

      // rebuild all
      for (var i=0; i<this.dialog.engine.inputFiles.length; i++) {
         var fileName = this.dialog.engine.inputFiles[i];
         var imageKeywords  = ffM_FITS_Keywords.makeImageKeywordsfromFile(fileName);
         this.dialog.engine.inputFITSKeywords[i] = imageKeywords;
         // Create the synthethic variables using the desired rules
         var variables = ffM_variables.makeSynthethicVariables(fileName, imageKeywords,
              this.dialog.engine.remappedFITSkeywords,
              this.dialog.engine.filterConverter, this.dialog.engine.typeConverter);
         this.dialog.engine.inputVariables[i] = variables;
      }

      // TODO - Merge with action on add files
      //this.dialog.rebuildFilesTreeBox();
      parentDialog.updateButtonState();
      parentDialog.updateTotal();
      parentDialog.refreshTargetFiles();
    }



   // Group the list boxed of the current mapping and conversions
   var currentState_GroupBox = new Control( this );

   currentState_GroupBox.sizer = new HorizontalSizer;
   currentState_GroupBox.sizer.margin = 6;
   currentState_GroupBox.sizer.spacing = 4;

   currentState_GroupBox.sizer.add( keywordNames_GroupBox);
   currentState_GroupBox.sizer.add( typeConversion_GroupBox);
   currentState_GroupBox.sizer.add( filterConversion_GroupBox);
   // TODO Find other way to fix minimal size
   //currentState_GroupBox.setMinHeight(150);


   // Group and create section bar

   this.conversion_GroupBox = new GroupBox( this );

   this.conversion_GroupBox.sizer = new VerticalSizer;
   this.conversion_GroupBox.sizer.margin = 6;
   this.conversion_GroupBox.sizer.spacing = 4;

   this.conversion_GroupBox.sizer.add( mappingRules_ComboBox);
   this.conversion_GroupBox.sizer.add( currentState_GroupBox, 100);





   // Buttons
   this.done_Button = new PushButton( this );
   this.done_Button.text = "Done"
   this.done_Button.enabled = true;
   this.done_Button.onClick = function() {
      this.dialog.ok();
   }


   this.buttonsSizer = new HorizontalSizer;
   this.buttonsSizer.spacing = 2;
   this.buttonsSizer.addStretch();
   this.buttonsSizer.add( this.done_Button);


   // Assemble configuration Dialog
   this.sizer = new VerticalSizer;
   this.sizer.margin = 4;
   this.sizer.spacing = 4;
   this.sizer.add( this.conversion_GroupBox );
   this.sizer.add(this.buttonsSizer);
   this.setVariableSize();
   this.adjustToContents();

#endif

}
ConfigurationDialog.prototype = new Dialog;

return {
   makeDialog: function(parent, ruleSet) {
      return new ConfigurationDialog(parent, ruleSet);
   }

}

}) ();
