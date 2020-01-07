// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js


// This file contains two modules:
//    ffM_GUI_support - General utility controls (currently used in this file only)
//    ffM_GUI_config  - Dialog to manage configuration sets


#define GROUP_BOX_MIN_SIZE 22

// ========================================================================================================================
// ffM_GUI_support - GUI support methods and controls
//   This module contains controls independent of the FITSFileManager data model
//   (but specialized in term of layout for use in the FITSFileManager gui)
// ========================================================================================================================
var ffM_GUI_support = (function (){

   // ---------------------------------------------------------------------------------------------------------
   // IconButtonBar - an horizontal bar of buttons defined by icons
   // parameters:
   //     buttons:  an array of objects  {icon:aBitmapName, toolTip: aText, action: aFunction()}
   // ---------------------------------------------------------------------------------------------------------
   var IconButtonBar = function(parent, buttons) {
      this.__base__ = Control;
      this.__base__(parent);

      var i, button, toolButton;

      this.sizer =  new HorizontalSizer;
      this.sizer.margin = 6;
      this.sizer.spacing = 4;

      // To protect against GC
      this.toolButtons = [];

      for (var i=0; i<buttons.length;i ++) {
         var button = buttons[i];
#ifdef DEBUG
         Log.debug(i,button.icon, button.toolTip);
#endif
         var toolButton = new ToolButton( parent );
         this.toolButtons.push(toolButton);
         this.sizer.add(toolButton);
         toolButton.icon = new Bitmap( button.icon );
         toolButton.toolTip = button.toolTip;
         toolButton.onClick = button.action;

      }
      this.sizer.addStretch();
   }
   IconButtonBar.prototype = new Control;




   // ---------------------------------------------------------------------------------------------------------
   // ManagedList_Box: A list selection box with the possibility to add, remove and move elements,
   //                  the list elements may have multiple properties represented in multiple columns.
   // parameters:
   //    parent: UI Control
   //    modeldescription: An array of element model descriptions (one description for each column),
   //         each description is an object with the property (more could be added later):
   //            propertyName: name of the propery to use in the element model for the corresponding column
   //   initialListModel: A (usually empty) initial list model (an array of object with the properties described by modelDescription)
   //                    Usually the model is specified by the modelListChange call (including for initial populate)
   //   elementFactory: A function() that return a new element model (called when a new element is added)
   //   toolTip: The tool tip text
   //   selectionCallBack: a function(elementModel) that return the element model currently selected. Can be null if
   //                      no element is selected (for example if the last one was deleted).
   //   sorted: if true the elements are sorted on the first column
   // ---------------------------------------------------------------------------------------------------------
   function ManagedList_Box(parent, modelDescription, initialListModel, elementFactory, toolTip, selectionCallback, sorted) {
      this.__base__ = Control;
      this.__base__(parent);

      var that = this;

      var i;
      (typeof sorted === 'undefined') && (sorted = false);

      // -- Model (the model will be updated in place)
      var listModel = [];


      // -- private methods
      // Create a node based on the model described in modelDescription
      var makeNode = function(treeBox, nodeModel, index) {
         //Log.debug('ManagedList_Box: makeNode -',Log.pp(nodeModel),Log.pp(modelDescription));
         var node = new TreeBoxNode( treeBox, index);
         for (var i=0; i<modelDescription.length;i++){
            node.setText(i, nodeModel[modelDescription[i].propertyName].toString());
         }
         return node;
      }



      // -- UI

      this.sizer = new VerticalSizer;

      this.treeBox = new TreeBox(this);
      this.sizer.add(this.treeBox);

      this.treeBox.rootDecoration = false;
      this.treeBox.numberOfColumns = 2;
      this.treeBox.multipleSelection = false;
      this.treeBox.headerVisible = false;
      this.treeBox.headerSorting = false;

      this.treeBox.sort(0,sorted); // DO NOT SEEMS TO WORK

      this.treeBox.style = Frame.FrameStyleSunken;

      this.treeBox.toolTip = toolTip;


      // -- Model update methods
      // The list model is changed (the model is an array that is updated in place)
      this.modelListChanged = function(newModelList) {

         // Clear current list display
         var i;
         var nmbNodes = this.treeBox.numberOfChildren;
         for (i=nmbNodes; i>0; i--) {
            this.treeBox.remove(i-1);
         }
         // Update the variable tracking the current model
         listModel = newModelList;

         // Add new nodes
         for (i=0; i<listModel.length; i++) {
            // Just making the nodes adds them to the treeBox
            makeNode(this.treeBox, listModel[i], i);
         }
      }
      // Create initial model
      this.modelListChanged(initialListModel);

     // The current values of the current row model changed
     this.currentModelElementChanged = function() {
        if (this.treeBox.selectedNodes.length>0) {
           var selectedNode = this.treeBox.selectedNodes[0];
           var selectedIndex = this.treeBox.childIndex(selectedNode);
           if (selectedIndex>=0 && selectedIndex<listModel.length) {
              for (var i=0; i<modelDescription.length;i++){
                 selectedNode.setText(i, listModel[selectedIndex][modelDescription[i].propertyName].toString());
              }
            }
        }
      }


     // -- Internal actions
     var upAction = function() {
        var nodeToMove,  nodeIndex, element1, element2;
        if (that.treeBox.selectedNodes.length>0) {
           nodeToMove = that.treeBox.selectedNodes[0];
           nodeIndex = that.treeBox.childIndex(nodeToMove);
           if (nodeIndex>0) {
              // Update model
              element1 = listModel[nodeIndex-1];
              element2 = listModel[nodeIndex];
              listModel.splice(nodeIndex-1, 2, element2, element1);
              // update UI
              that.treeBox.remove(nodeIndex);
              that.treeBox.insert(nodeIndex-1,nodeToMove);
              that.treeBox.currentNode = nodeToMove;
           }
        }
     }
     var downAction = function() {
        var nodeToMove,  nodeIndex, element1, element2;
        if (that.treeBox.selectedNodes.length>0) {
           nodeToMove = that.treeBox.selectedNodes[0];
           nodeIndex = that.treeBox.childIndex(nodeToMove);
           if (nodeIndex<that.treeBox.numberOfChildren-1) {
              // Update model
              element1 = listModel[nodeIndex];
              element2 = listModel[nodeIndex+1];
              listModel.splice(nodeIndex, 2, element2, element1);
              // update UI
              that.treeBox.remove(nodeIndex);
              that.treeBox.insert(nodeIndex+1,nodeToMove);
              that.treeBox.currentNode = nodeToMove;
           }
        }
     }
     var addAction = function() {
        var nodeBeforeNew, newNode;
        var nodeIndex = that.treeBox.numberOfChildren;
        if (that.treeBox.selectedNodes.length>0) {
           nodeBeforeNew = that.treeBox.selectedNodes[0];
           nodeIndex = that.treeBox.childIndex(nodeBeforeNew) + 1;
        }
        // Create node
        var element = elementFactory();
        if (element !== null) {
           // insert node in model then ui
           listModel.splice(nodeIndex, 0, element);
           newNode = makeNode(that.treeBox, element, nodeIndex);
           that.treeBox.currentNode = newNode;
           that.treeBox.onNodeSelectionUpdated();
        }
     }

     var deleteAction = function() {
        var nodeToDelete,  nodeIndex;
        if (that.treeBox.selectedNodes.length>0) {
           nodeToDelete = that.treeBox.selectedNodes[0];
           nodeIndex = that.treeBox.childIndex(nodeToDelete);
           listModel.splice(nodeIndex,1);
           that.treeBox.remove(nodeIndex);
        }
     }

     this.treeBox.onNodeSelectionUpdated  = function() {
        // There could be an empty selection (last element removed)
        if (that.treeBox.selectedNodes.length>0) {
           var selectedNode = that.treeBox.selectedNodes[0];
           var selectedIndex = that.treeBox.childIndex(selectedNode);
           if (selectedIndex>=0 && selectedIndex<listModel.length) {
              selectionCallback(listModel[selectedIndex]);
            }
         }
     }


     var buttons = [
        {
           icon: ":/browser/move-up.png",
           toolTip: "Move item up",
           action: upAction
           },
        {
           icon: ":/browser/move-down.png",
           toolTip:  "Move item down",
           action: downAction
        },
        {
           icon: ":/icons/add.png",
           toolTip:  "Add new item",
           action: addAction
        },
        {
           icon: ":/icons/remove.png",
           toolTip:  "Delete item",
           action: deleteAction
        },
     ];


     var iconButtonBar = new IconButtonBar(this, buttons);
     this.sizer.add(iconButtonBar);

     //this.adjustToContents();


  }
  ManagedList_Box.prototype = new Control;


  return {
     IconButtonBar: IconButtonBar,
     ManagedList_Box: ManagedList_Box,
  }

}) ();





// ========================================================================================================================
// ffM_GUI_config - Configuration dialog
//    The Dialog used to update a ConfigurationSet
// Usage:
//    A factory method is exposed to create the Dialog
//    Before being executed it must be configured with the ConfigurationSet and the name of the configuration
//       to select at start.
//    The dialog will make a local copy of the Configuration object and update it in place. It will leave
//    data for all resolvers selected for a variable, so in case a resolver is changed and then changed back to
//    the initial value, the original data is recovered.  It is exepected that this redundant data will be
//    removed before export.
//    The dialog expose properties to get the updated ConfigurationSet and currently edited set,
//    the caller should use them in case of successful return (and ignored them in case of cancel).
// ========================================================================================================================

var ffM_GUI_config = (function (){

   // -- Private methods
   function makeLoadSaveOKCancelButtons(parentDialog) {

      var c = new Control(parentDialog);
      c.sizer = new HorizontalSizer;
      c.sizer.margin = 6;
      c.sizer.spacing = 4;

      c.load_Button = new PushButton( c );
      c.load_Button.text = "Load...";
      c.load_Button.enabled = true;
      c.load_Button.onClick = function() {
         var ofd = new OpenFileDialog;
         ofd.multipleSelections = false;
         ofd.caption = "Select FITSFileManager configuration file";
         ofd.filters = [["configuration",  FFM_CONFIGURATION_FILES_FILTER]];
         ofd.initialPath = this.dialog.guiParameters.lastConfigurationDirectory;
         if ( ofd.execute() ) {
            this.dialog.guiParameters.lastConfigurationDirectory = getDirectoryOfFileWithDriveLetter(ofd.fileName);
            this.dialog.loadFileAction(ofd.fileName);
         }
      }

      c.saveAll_Button = new PushButton( c );
      c.saveAll_Button.text = "Save all...";
      c.saveAll_Button.enabled = true;
      c.saveAll_Button.onClick = function() {
         var ofd = new SaveFileDialog;
         ofd.overwritePrompt = false;
         ofd.caption = "Select FITSFileManager configuration file";
         ofd.filters = [["configuration", FFM_CONFIGURATION_FILES_FILTER]];        
         ofd.initialPath = this.dialog.guiParameters.lastConfigurationDirectory;
         if ( ofd.execute() ) {
            this.dialog.guiParameters.lastConfigurationDirectory = getDirectoryOfFileWithDriveLetter(ofd.fileName);
            this.dialog.saveAllToFileAction(ofd.fileName);
         }
      }

      c.saveCurrent_Button = new PushButton( c );
      c.saveCurrent_Button.text = "Save current...";
      c.saveCurrent_Button.enabled = true;
      c.saveCurrent_Button.onClick = function() {
         var ofd = new SaveFileDialog;
         ofd.overwritePrompt = false;
         ofd.caption = "Select FITSFileManager configuration file";
         ofd.filters = [["configuration", FFM_CONFIGURATION_FILES_FILTER]];
         // TODO - Make sure name of file is clean 
         ofd.initialPath = this.dialog.guiParameters.lastConfigurationDirectory+"/"+this.dialog.currentConfigurationName;
         if ( ofd.execute() ) {
            this.dialog.guiParameters.lastConfigurationDirectory = getDirectoryOfFileWithDriveLetter(ofd.fileName);
            this.dialog.saveCurrentToFileAction(ofd.fileName);
         }
      }


      c.cancel_Button = new PushButton( c );
      c.cancel_Button.text = "Cancel";
      c.cancel_Button.enabled = true;
      c.cancel_Button.onClick = function() {
         parentDialog.cancel();
      }
      c.ok_Button = new PushButton( c );
      c.ok_Button.text = "OK";
      c.ok_Button.enabled = true;
      c.ok_Button.onClick = function() {
         parentDialog.ok();
      }

      c.sizer.add(c.load_Button);
      c.sizer.add(c.saveCurrent_Button);
      c.sizer.add(c.saveAll_Button);
      c.sizer.addStretch();
      c.sizer.add(c.cancel_Button);
      c.sizer.add(c.ok_Button);

      return c;

  }

  // ---------------------------------------------------------------------------------------------------------

  // Top pane - Selection of configuration
  function ConfigurationSelection_ComboBox (parent, initialNames, configurationSelectedCallback) {
      this.__base__ = ComboBox;
      this.__base__(parent);

      this.configurationNames = initialNames;

      var i;

      // -- UI
      this.toolTip = Text.H.SELECT_CONFIGURATION_BUTTON_TOOLTIP;
      this.enabled = true;
      this.editEnabled = false;
      for (i=0; i<initialNames.length;i++) {
         this.addItem(initialNames[i]);
      }
      if (this.configurationNames.length>0) {
         this.currentItem = 0;
      }

      // -- callback
      this.onItemSelected = function() {
         if (this.currentItem>=0 && this.currentItem<this.configurationNames.length) {
            configurationSelectedCallback(this.configurationNames[this.currentItem]);
         }
      }

      // -- Model update method, provide now list of names and new current name
      this.configure = function(names, selectedName) {
         this.configurationNames = names;
         this.clear();
         for (i=0; i<names.length;i++) {
            this.addItem(names[i]);
            if (names[i] === selectedName) {
               this.currentItem = i;
            }
         }
      }
  }
  ConfigurationSelection_ComboBox.prototype = new ComboBox;



  // Helper to validate and normalize input text,
  // There is no real conversion, as we keep all information in text format
  // the ensureIsString()  helps debug in case a non string object is received
  var testInvalidVariableNameRegExp = /[&\(\);<>=!%*]/;
  var removeVariableReferencesRE = /&[0-9]+;/g
  var propertyTypes = {
     FREE_TEXT: {
         name: "FREE_TEXT",
         propertyToText: function(value) {return ensureIsString(value)},
         textToProperty: function(text) {return text},
     },
     REG_EXP: {
        // Check that this is a valid regular expression, but the regexp is kept in its
        // string format (as parsed with regExpFromString)
        name: "REG_EXP",
        propertyToText: function(value) {return ensureIsString(value)},
        textToProperty: function(text) {return regExpToString(regExpFromUserString(text))},
     },
     REG_EXP_REPLACEMENT: {
        // Check that this is a valid replacement for a regular expression,
        // this ensures that the only &<number>; are used (no &variable; or dangling &)
        name: "REG_EXP_REPLACEMENT",
        propertyToText: function(value) {return ensureIsString(value)},
        textToProperty: function(text) {
           var withoutRef = text.replace(removeVariableReferencesRE,'');
           if (withoutRef.indexOf("&")>=0) {
               throw "Invalid replacement string"}
            else {return text}
        },
     },
     // Check that the characters are valid for a variable name
     VAR_NAME: {
        name: "VAR_NAME",
        propertyToText: function(value) {return ensureIsString(value)},
        textToProperty: function(text) {
            var t = text.trim();
            if (testInvalidVariableNameRegExp.test(t)) {
               throw "Invalid character in variable name"}
            else {return t}
         },
     },

  }



  // Utility pane - A Label - Labelled text field of a property of an object
  //  parent: UI parent control
  //  style: Style related properties (minLabelWidth, minDataWidth)
  //  name: Text of the label of this text field
  //  toolTip: the tooltip to present to the user
  //  property: Name of property of the target that will be used as source of text and destination of text
  //  propertyType: An object with two functions:
  //     propertyToText: that format the property as text
  //     textToProperty: that parse the text and return the property value, throw an exception in case of error
  //  valueChangedCallback(): function that will be called if the text is successfuly updated - if null there is no callback
  // The target (the object containing the property to edit) is specified dynamically (including at initialization)
  function TextEntryRow(parent, style, name, toolTip, property, propertyType, valueChangedCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 2;
     this.sizer.spacing = 2;

     this.property = property;
     this.target = null;

     this.the_Label = new Label( this );
     this.sizer.add(this.the_Label);
     this.the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
     this.the_Label.minWidth = style.minLabelWidth;
     this.the_Label.text = name + ": ";
     this.the_Label.toolTip = toolTip;

     this.text_Edit = new Edit(this);
     this.text_Edit.minWidth = style.minDataWidth;
     this.sizer.add(this.text_Edit)
     this.text_Edit.toolTip = toolTip;

     this.text_Edit.onTextUpdated = function() {
        var value;
        if (that.target !== null) {
           //Log.debug("TextEntryRow: onTextUpdated:",property,this.text);
           try {
              value =  propertyType.textToProperty(this.text);
              // Next lines will only execute if value was correctly parsed
              that.text_Edit.textColor = 0x000000;
              that.target[property] = value;
              if (valueChangedCallback != null) {
                 valueChangedCallback();
              }
           } catch (error) {
              that.text_Edit.textColor = 0xFF0000;
           }
        }
     }
     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
           that.text_Edit.text = '';
           that.text_Edit.enabled = false;
        } else {
           if (! target.hasOwnProperty(property)) {
              throw "TextEntryRow.updateTarget: Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
           }
           that.text_Edit.text = propertyType.propertyToText(target[property]);
           that.text_Edit.enabled = true;
        }
     }

     this.updateTarget(null);
  }
  TextEntryRow.prototype = new Control;

   // TODO Refactor with Text Entry Row
  function BooleanEntryRow(parent, style, name, toolTip, property, valueChangedCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 2;
     this.sizer.spacing = 2;

     this.property = property;
     this.target = null;

     this.the_Label = new Label( this );
     this.sizer.add(this.the_Label);
     this.the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
     this.the_Label.minWidth = style.minLabelWidth;
     this.the_Label.text = name + ": ";
     this.the_Label.toolTip = toolTip;

     this.bool_CheckBox = new CheckBox(this);
     this.bool_CheckBox.minWidth = style.minDataWidth;
     this.sizer.add(this.bool_CheckBox)
     this.bool_CheckBox.toolTip = toolTip;

     this.bool_CheckBox.onCheck = function() {
        var value;
        if (that.target !== null) {
             value =  this.checked;
            that.bool_CheckBox.textColor = 0x000000;
            that.target[property] = value;
            if (valueChangedCallback != null) {
               valueChangedCallback();
            }
        }
     }
     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
           that.bool_CheckBox.checked = false;
           that.bool_CheckBox.enabled = false;
        } else {
           if (! target.hasOwnProperty(property)) {
              throw "BooleanEntryRow.updateTarget: Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
           }
           that.bool_CheckBox.checked = target[property];
           that.bool_CheckBox.enabled = true;
        }
     }

     this.updateTarget(null);
  }
  BooleanEntryRow.prototype = new Control;


   // TODO Refactor with Text Entry Row
  function CheckListEntryRow(parent, style, name, toolTip, property, checkNames, checkValues, valueChangedCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     var i;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 2;
     this.sizer.spacing = 2;

     this.property = property;
     this.target = null;
     this.checkLabels = [];

     this.the_Label = new Label( this );
     this.sizer.add(this.the_Label);
     this.the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
     this.the_Label.minWidth = style.minLabelWidth;
     this.the_Label.text = name + ": ";
     this.the_Label.toolTip = toolTip;

     this.checkBoxContainer = new Control(this);
     this.sizer.add(this.checkBoxContainer);
     this.checkBoxContainer.sizer = new HorizontalSizer;
     this.checkBoxes = [];

     for (var i=0; i<checkNames.length; i++) {

         var check_Label = new Label( this );
         this.checkBoxContainer.sizer.add(check_Label);
         this.checkLabels.push(check_Label);
         check_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
         check_Label.text = checkNames[i];
         this.checkBoxContainer.sizer.addSpacing(4);

         var bool_CheckBox = new RadioButton(this.checkBoxContainer);
         this.checkBoxContainer.sizer.add(bool_CheckBox);
         this.checkBoxes.push(bool_CheckBox);
         bool_CheckBox.toolTip = toolTip;
         bool_CheckBox.code = checkValues[i];
         this.checkBoxContainer.sizer.addSpacing(4);

         bool_CheckBox.onCheck = function() {
            var value;
            if (that.target !== null) {
               value =  this.code;
               bool_CheckBox.textColor = 0x000000;
               that.target[property] = value;
               if (valueChangedCallback != null) {
                  valueChangedCallback();
               }
            }
         }
     }
     this.checkBoxContainer.sizer.addStretch();

     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
            for (var i=0; i<that.checkBoxes.length;i ++) {
               that.checkBoxes[i].checked = false;
               that.checkBoxes[i].enabled = false;
            }
        } else {
            if (! target.hasOwnProperty(property)) {
               throw "CheckListEntryRow.updateTarget: Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
            }
            var code = target[property];
            for (var i=0; i<that.checkBoxes.length;i ++) {
               that.checkBoxes[i].checked = (code === checkValues[i]);
               that.checkBoxes[i].enabled = true;
            }
        }
     }

     this.updateTarget(null);
  }
  CheckListEntryRow.prototype = new Control;


  // -- Middle right sub-panes (components to edit variables)
  function ResolverSelection_ComboBox(parent, mappingNames, mappingSelectionCallback) {
     this.__base__ = ComboBox;
     this.__base__(parent);

     var i;

     // -- UI
     this.toolTip = Text.H.VARIABLE_RESOLVER_TOOLTIP;
     this.enabled = true;
     this.editEnabled = false;
     for (i=0; i<mappingNames.length;i++) {
        this.addItem(mappingNames[i]);
     }
     this.currentItem = 0;
     this.enabled = false;

     // -- Call backs
     this.onItemSelected = function() {
        if (this.currentItem>=0) {
           mappingSelectionCallback(mappingNames[this.currentItem]);
        }
     }

     // -- Update model
     this.selectResolver = function(name) {
        if (name == null) {
              this.enabled = false;
        } else {
           for (i=0; i<mappingNames.length;i++) {
              if (name === mappingNames[i]) {
                 this.currentItem = i;
                 this.enabled = true;
                 break;
              }
           }
        }
     }
  }
  ResolverSelection_ComboBox.prototype = new ComboBox

  function ResolverSelectionRow(parent, rowStyle, name, mappingNames, mappingSelectionCallback) {
      this.__base__ = Control;
      this.__base__(parent);

      var that = this;

      // -- UI
      this.sizer = new HorizontalSizer;
      this.sizer.margin = 2;
      this.sizer.spacing = 2;

      this.the_Label = new Label( this );
      this.sizer.add(this.the_Label);
      this.the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      this.the_Label.minWidth = rowStyle.minLabelWidth;
      this.the_Label.text = name + ": ";

      this.resolver_ComboBox = new ResolverSelection_ComboBox (parent, mappingNames, mappingSelectionCallback);
      this.resolver_ComboBox.minWidth = rowStyle.minDataWidth;
      this.sizer.add(this.resolver_ComboBox);

      // -- Update model
      this.selectResolver = function(name) {
         that.resolver_ComboBox.selectResolver(name);
      }
  }
  ResolverSelectionRow.prototype = new Control;


  // ..............................................................................................
  // -- Controls for resolver
  // ..............................................................................................

  function MapFirstRegExpControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "Enter the name of a FITS key that will provide the value",
               "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(keyRow);


     var transformationDefinitionFactory = function() {
        return {regexp: '/.*/', replacement: '?'}
     }

     var transformationSelectionCallback = function(transformationModel) {
        that.selectTransformationToEdit(transformationModel);
     }

     this.regExpListSelection_GroupBox = new GroupBox(this);
     this.sizer.add(this.regExpListSelection_GroupBox);
     this.regExpListSelection_GroupBox.title ="List of RegExp->value";
     this.regExpListSelection_GroupBox.sizer = new VerticalSizer;

     this.selectionLayoutControl = new Control;
     this.regExpListSelection_GroupBox.sizer.add(this.selectionLayoutControl);
     this.selectionLayoutControl.sizer = new HorizontalSizer;
     this.selectionLayoutControl.sizer.addStretch();

     this.regExpListSelection_Box = new ffM_GUI_support.ManagedList_Box(
           this.selectionLayoutControl,
           [{propertyName: 'regexp'},{propertyName: 'replacement'}],
           [], // Its model will be initialized dynamically
           transformationDefinitionFactory,
           "Regular expression to text mapping.\nSelect a row to edit the details.\n"+
           "The first matching expression will define the value of the variable.\n"+
           "If none match, the variable is undefined. Use '/.*/' as the last expression to have a default value\n"+
           "&0; etc may be used to refer to the result of the regular expression",
           transformationSelectionCallback,
           false // Keep in order
     );
     this.selectionLayoutControl.sizer.add(this.regExpListSelection_Box);


     this.currentRegExpRow = new TextEntryRow(this.regExpListSelection_GroupBox, rowStyle, "Regexp",
        "A regular expression that will be tested against the key value", "regexp",
        propertyTypes.REG_EXP,
        function() {that.regExpListSelection_Box.currentModelElementChanged()});
     this.regExpListSelection_GroupBox.sizer.add(this.currentRegExpRow);

     this.currentReplacementRow = new TextEntryRow(this.regExpListSelection_GroupBox,rowStyle, "Replacement",
        "The replacement text to use if the regular expression matched.\n" +
        "&0; may be used to refer to the original text, &1;, &2; refers to parenthesized groups in the regular expression.", "replacement",
        propertyTypes.REG_EXP_REPLACEMENT,
        function() {that.regExpListSelection_Box.currentModelElementChanged()});
     this.regExpListSelection_GroupBox.sizer.add(this.currentReplacementRow);


     this.selectTransformationToEdit = function(transformationModel) {
        this.currentRegExpRow.updateTarget(transformationModel);
        this.currentReplacementRow.updateTarget(transformationModel);
     }

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // initialize should probably be somewhere else
        this.initialize(variableDefinition);
        keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.regExpListSelection_Box.modelListChanged(variableDefinition.parameters[resolverName].reChecks);
        // Workaround:
        // The modelListChanged above seem to generate callbacks events that update the regexp
        // and replacement row, but we want them to be disabled until the user explicitely select one.
        this.currentRegExpRow.updateTarget(null);
        this.currentReplacementRow.updateTarget(null);
     }

     this.leave = function() {
        this.currentRegExpRow.updateTarget(null);
        this.currentReplacementRow.updateTarget(null);
     }
  }
  MapFirstRegExpControl.prototype = new Control;

  // ..............................................................................................

  function ConstantValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;
     this.constantValueRow = new TextEntryRow(this, rowStyle, "Value",
     "The fixed value for this variable",
     "value", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.constantValueRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.constantValueRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  ConstantValueResolverControl.prototype = new Control;

  // ..............................................................................................

  function TextValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     this.keyRow = new TextEntryRow(this, rowStyle, "FITS key",
      "The name of a FITS key that will provide the value of this variable\n" +
      "(the FITS key value will be cleaned of special characters).",
      "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.keyRow);

     this.formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the value, for example '-%ls' to preceed the string with a dash\n"+
     "IMPORTANT - You must use '%ls', not '%s' to indicate the location of the string in the format",
     "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.formatRow);

     this.caseRuleRow = new CheckListEntryRow(this, rowStyle, "Case conversion",
     "Case conversion",
     "case",['up','down','none'], ['UP','DOWN','NONE'], null);
     this.sizer.add(this.caseRuleRow);


     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.formatRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.caseRuleRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  TextValueResolverControl.prototype = new Control;

  // ..............................................................................................

  function IntegerValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     this.keyRow = new TextEntryRow(this, rowStyle, "FITS key",
      "The name of a FITS key that will provide the value",
      "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.keyRow);

     this.useAbsoluteRow = new BooleanEntryRow(this, rowStyle, "Use absolute value",
        "If true, the absolute value of the corresponding value is taken (the sign is discarded)",
        "abs", null);
     this.sizer.add(this.useAbsoluteRow);

     this.formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the value, like '%4.4d', possibly with additional text like 'TEMP-%3d'",
     "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.formatRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.useAbsoluteRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.formatRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  IntegerValueResolverControl.prototype = new Control;


  // ..............................................................................................

  function IntegerPairValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key 1
     this.key1Row = new TextEntryRow(this, rowStyle, "FITS key 1",
      "The name of a FITS key that will provide the first value",
      "key1", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.key1Row);

     this.key2Row = new TextEntryRow(this, rowStyle, "FITS key 2",
      "The name of a FITS key that will provide the second value",
      "key2", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.key2Row);

     this.formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the 2 values, for example '%dx%d'",
     "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.formatRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.key1Row.updateTarget(variableDefinition.parameters[resolverName]);
        this.key2Row.updateTarget(variableDefinition.parameters[resolverName]);
        this.formatRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  IntegerPairValueResolverControl.prototype = new Control;

   // ..............................................................................................

  function DateTimeValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     this.keyRow = new TextEntryRow(this, rowStyle, "FITS key",
      "The name of a FITS key that will provide the date value\n"+
      "or date/time value in FITS format as 'YYYY-MM-DDThh:mm:ss[.sss. . . ]",
      "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.keyRow);

     this.formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid date or date/time format, as %Y-%m%dT%h:%m%s.%L for 2014-03-21T12:34:21.321\n" +
     "%y for 2 digit year, %j for julian day, %% for %\n" +
     "Usually used as %Y%m%d to add the date to the file name",
     "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.formatRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        this.formatRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  DateTimeValueResolverControl.prototype = new Control;

// ..............................................................................................

  function NightResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     this.key1Row = new TextEntryRow(this, rowStyle, "LONG_OBS key",
     "The name of a FITS key that provide the longitude of the observatory",
     "keyLongObs", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.key1Row);

     this.key2Row = new TextEntryRow(this, rowStyle, "JD key",
     "The name of a FITS key that contains the julian date of the observation",
     "keyJD", propertyTypes.FREE_TEXT, null);
     this.sizer.add(this.key2Row);


     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        this.key1Row.updateTarget(variableDefinition.parameters[resolverName]);
        this.key2Row.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  NightResolverControl.prototype = new Control;




  // ..............................................................................................


  // -- Middle pane - Variable definitions (two panes: add/remove variables and edit selected variables)
  function VariableUIControl(parent, variableDefinitionFactory ) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     // -- Model data - set by selectVariable()
     this.currentVariableDefinition = null;

     // -- UI data
     // this.resolverSelection_GroupBox = null; // defined later
     this.currentResolver = null;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 6;
     this.sizer.spacing = 4;

     var labelWidth = this.font.width( "MMMMMMMMMMMM: " );
     var dataWidth = this.font.width( "MMMMMMMMMMMMMMMMMMMMMMM" );
     var rowStyle = {
        minLabelWidth: labelWidth,
        minDataWidth: dataWidth,
     }


     // -- GUI action callbacks
     // Variable selected in current configuration, forward to the model handling later in this object
     var variableSelectionCallback = function(variableDefinition) {
        //Log.debug("VariableUIControl: variableSelectionCallback - Variable selected: " +variableDefinition.name);
        that.selectVariable(variableDefinition);
     }




     // -- Left side - select variable being edited, add/remove variables

     this.variableListSelection_GroupBox = new GroupBox(this);
     this.sizer.add(this.variableListSelection_GroupBox);
     this.variableListSelection_GroupBox.title = "Select variable";
     this.variableListSelection_GroupBox.sizer = new VerticalSizer; // Any sizer

     this.variableListSelection_Box = new ffM_GUI_support.ManagedList_Box(
           this.variableListSelection_GroupBox,
          [{propertyName: 'name'},{propertyName: 'description'}],
          [], // Its model will be initialized dynamically
           variableDefinitionFactory,
           Text.H.VARIABLE_SELECTION_TOOLTIP,
           variableSelectionCallback,
           true // Sort by variable name
     );
     this.variableListSelection_GroupBox.sizer.add(this.variableListSelection_Box);



     //--  Right side - Enter parameters corresponding to selected variables
     var resolverSelectionCallback = function(resolverName) {
#ifdef DEBUG
        debug("VariableUIControl: resolverSelectionCallback - selected:",resolverName);
#endif
        that.updateResolver(resolverName);
     }

     this.variableDetails_GroupBox = new GroupBox(this);
     this.variableDetails_GroupBox.title = "Parameters of variable";
     this.sizer.add(this.variableDetails_GroupBox);

     this.variableDetails_GroupBox.sizer = new VerticalSizer;



     this.resolverSelectionRow =  new ResolverSelectionRow(this, rowStyle, "Resolver", ffM_Resolver.resolverNames, resolverSelectionCallback);
     this.variableDetails_GroupBox.sizer.add(this.resolverSelectionRow);

     this.variableNameRow = new TextEntryRow(this, rowStyle, "Variable name",
        "Enter the name of the variable, it will be used as '&amp;name;' in a template",
        "name",
        propertyTypes.VAR_NAME,
        function() {that.variableListSelection_Box.currentModelElementChanged()});
     this.variableDetails_GroupBox.sizer.add(this.variableNameRow);

     this.descriptionRow = new TextEntryRow(this, rowStyle, "Description",
        "Enter a short description of the variable",
        "description",
        propertyTypes.FREE_TEXT,
        function() {that.variableListSelection_Box.currentModelElementChanged()});
     this.variableDetails_GroupBox.sizer.add(this.descriptionRow);

     this.variableShownRow = new BooleanEntryRow(this, rowStyle, "Show column by default",
        "If true, the corresponding column is shown in the Input file list by default",
        "show",
        function() {that.variableListSelection_Box.currentModelElementChanged()});
     this.variableDetails_GroupBox.sizer.add(this.variableShownRow);

     this.addNewResolverControl = function(resolverControl){
        // This also keep a reference to the resolverControl, so that it is not GCs
        ffM_Resolver.resolverByName(resolverControl.resolverName).control = resolverControl;
        this.variableDetails_GroupBox.sizer.add(resolverControl);
        resolverControl.hide();
     }

     // Make all resolver controls, only the selected one will be shown
     this.addNewResolverControl(new ConstantValueResolverControl(this.variableDetails_GroupBox, 'Constant', rowStyle));

     this.addNewResolverControl(new TextValueResolverControl(this.variableDetails_GroupBox, 'Text', rowStyle));

     this.addNewResolverControl(new IntegerValueResolverControl(this.variableDetails_GroupBox, 'Integer', rowStyle));

     this.addNewResolverControl(new IntegerPairValueResolverControl(this.variableDetails_GroupBox, 'IntegerPair', rowStyle));
     
     this.addNewResolverControl(new DateTimeValueResolverControl(this.variableDetails_GroupBox, 'DateTime', rowStyle));

     this.addNewResolverControl(new MapFirstRegExpControl(this.variableDetails_GroupBox,'RegExpList', rowStyle));

     this.addNewResolverControl(new NightResolverControl(this.variableDetails_GroupBox,'Night', rowStyle));

     // FileName and FileExtension have no parameter
     ffM_Resolver.resolverByName('FileName').control = null
     ffM_Resolver.resolverByName('FileExtension').control = null

     this.variableDetails_GroupBox.sizer.addStretch();

     // TRICK - void growing when more proeprties are added to the right pane, the dialg can grow,
     // so it is possible to avoid this minimum size, but the effect is likely uggly.
     this.variableDetails_GroupBox.setMinHeight(GROUP_BOX_MIN_SIZE * this.font.lineSpacing + 2*this.sizer.margin);



     // -- Update the model
     // The resolver name was updated (by select box, by changing variable or initially)
     // null if no resolver (like an empty variable list)
     this.updateResolver = function(resolverName) {
        //Log.debug(" updateResolver",resolverName,that.currentResolver);
        // There could be no controller if there is no parameter
        if (this.currentResolver != null && this.currentResolver.control !== null) {
           this.currentResolver.control.leave();
           this.currentResolver.control.hide();
        }
        this.currentResolver =null;
        if (resolverName != null) {
           var resolver = ffM_Resolver.resolverByName(resolverName);
           if (resolver === null) {
              throw "Invalid resolver '" + resolverName + "' for variable '"+ variableDefinition.name+"'";
           }
           if (this.currentVariableDefinition !== null) {
              // record the new resolver
              this.currentVariableDefinition.resolver = resolverName;
              // Populate and show it
              this.currentResolver = resolver;
              if (resolver.control !== null) {
                 resolver.control.populate(this.currentVariableDefinition );
                 resolver.control.show();
              }
           }
        }
     }

     // The variable to edit was selected
     this.selectVariable = function(variableDefinition) {
#ifdef DEBUG
        debug("VariableUIControl: selectVariable - Variable selected ",variableDefinition.name );
        //debug("********** ",JSON.stringify(variableDefinition) );
#endif
         var resolverName;

        this.currentVariableDefinition = variableDefinition;

        // populate the common fields
        this.variableNameRow.updateTarget(variableDefinition);
        this.descriptionRow.updateTarget(variableDefinition);
        this.variableShownRow.updateTarget(variableDefinition);

        // Find new resolver, populate and show it
        resolverName = this.currentVariableDefinition.resolver;
        this.updateResolver(resolverName);

        // Update the UI to have the resolver type of the current variable
        this.resolverSelectionRow.selectResolver(resolverName);
      }

     // The list of variables was changed externally (initial or change)
     this.updateVariableList = function(newVariableList) {
        // Update UI
        this.variableListSelection_Box.modelListChanged(newVariableList);
     }

  }

  VariableUIControl.prototype = new Control;


  function ConfigurationGroupBox(parent, configurationSelectedCallback, configurationDuplicateCallback, configurationDeleteCallback ) {
      this.__base__ = GroupBox;
      this.__base__(parent);
      var that = this;

      // Initialized by configure()
      this.editedConfigurationSet = null;

      var configurationNameMinWidth = this.font.width( "MMMMMMMMMMMMMMMMM: " );
      this.title = "Configuration selection";

      this.sizer = new VerticalSizer;
      this.sizer.margin = 5;
      this.sizer.spacing = 5;

      this.nameRow = new Control(this);
      this.sizer.add(this.nameRow);

      this.nameRow.sizer = new HorizontalSizer;

      this.nameRow.sizer.margin = 1;
      this.nameRow.sizer.spacing = 2;
      this.configurationSelection_ComboBox = new ConfigurationSelection_ComboBox(this.nameRow, [], configurationSelectedCallback);
      this.nameRow.sizer.add(this.configurationSelection_ComboBox);
      this.configurationSelection_ComboBox.minWidth = configurationNameMinWidth;


      this.nameRow.sizer.addStretch();

      this.name_Label = new Label( this.nameRow );
      this.nameRow.sizer.add(this.name_Label);
      this.name_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      this.name_Label.text = " Name: ";

      this.configurationName_Edit = new Edit(this.nameRow);
      this.nameRow.sizer.add(this.configurationName_Edit);
      this.configurationName_Edit.minWidth = configurationNameMinWidth;
      this.configurationName_Edit.toolTip = "Name of configuration\nIllegal special characters makes the field red.";
      this.configurationName_Edit.onTextUpdated = function() {
         // We should always have a selected configuration at this point, but play it safe
         if (that.selectedConfiguration !== null) {
            var t = this.text.trim();
            var configurationNames = ffM_Configuration.getAllConfigurationNames(that.editedConfigurationSet);
            // Avoid duplicate or illegal names (the check is a hack, but this is java script after all)
            // Characters are limited to avoid problem in case the name is used in some expression later
            if (t.length >0 && !(/[()\[\]{}&$;!?'".,]/.test(t)) && configurationNames.indexOf(t)===-1) {
               that.updateName(t);
            } else if (that.currentConfigurationName !== t) {
               // Red only if the trimmed value is not the current one
               this.textColor=0x00FF0000;
            } else {
               // Back to the current one, assumne legal
               this.textColor=0x00000000;
            }
         }
      }

      this.addConfigurationButton = new ToolButton( this.nameRow );
      this.nameRow.sizer.add(this.addConfigurationButton);
      this.addConfigurationButton.icon = ":/file-explorer/copy.png" ;
      this.addConfigurationButton.toolTip = "Add a configuration (duplicate current one)";
      this.addConfigurationButton.onClick = function() {
         configurationDuplicateCallback(that.currentConfigurationName);
      }

      this.removeConfigurationButton = new ToolButton( this.nameRow );
      this.nameRow.sizer.add(this.removeConfigurationButton);
      this.removeConfigurationButton.icon = ":/file-explorer/delete.png" ;
      this.removeConfigurationButton.toolTip = "Delete the current configuration";
      this.removeConfigurationButton.onClick = function() {
         var msg = new MessageBox( "Do you want to delete the configuration '" +that.currentConfigurationName + "' ?",
                          "Are you sure?", StdIcon_Question, StdButton_Yes, StdButton_No );
         if (msg.execute() == StdButton_Yes) {
          configurationDeleteCallback(that.currentConfigurationName);
         }
      }

      this.descriptionRow = new Control(this);
      this.sizer.add(this.descriptionRow);
      this.descriptionRow.sizer = new HorizontalSizer;

      this.description_Label = new Label( this.descriptionRow );
      this.descriptionRow.sizer.add(this.description_Label);
      this.description_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      this.description_Label.text = " Description: ";

      this.descriptionRow.sizer.margin = 1;
      this.descriptionRow.sizer.spacing = 2;
      this.configurationComment_Edit = new Edit(this.descriptionRow);
      this.descriptionRow.sizer.add(this.configurationComment_Edit);
      this.configurationComment_Edit.toolTip = "Short description of the configuration";

      this.descriptionRow.sizer.addSpacing(50); // Pretty approximate

      // To track edited comment
      this.selectedConfiguration = null;


      this.configure = function(editedConfigurationSet, currentConfigurationName) {
         this.editedConfigurationSet = editedConfigurationSet;
         this.currentConfigurationName = currentConfigurationName;
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.configurationSelection_ComboBox.configure(configurationNames, currentConfigurationName);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);
         if (this.selectedConfiguration === null) {
            throw "Internal Error - configuration name '" + currentConfigurationName + "' not in list of configurations";
         }
         this.configurationComment_Edit.text = this.selectedConfiguration.description;
         this.configurationName_Edit.text = currentConfigurationName;
         this.configurationName_Edit.textColor =0x00000000;
      }
      this.updateName = function(newConfigurationName) {
         // Assume the name is allowed and not duplicated (checked by caller),
         // set the new current name in our working copy (our selected configuration)
         this.selectedConfiguration.name = newConfigurationName;
         this.currentConfigurationName = newConfigurationName;
         // Update the whole configuration selection box with the new names
         var configurationNames = ffM_Configuration.getAllConfigurationNames(this.editedConfigurationSet);
         if (configurationNames.indexOf(newConfigurationName)===-1) {
            throw "Internal Error - new configuration name '" + newConfigurationName + "' not in list of configurations";
         }
         this.configurationSelection_ComboBox.configure(configurationNames, newConfigurationName);
         this.configurationName_Edit.textColor=0x00000000;
      }
      this.configurationComment_Edit.onTextUpdated = function() {
         if (that.selectedConfiguration !== null) {
            that.selectedConfiguration.description = this.text;
         }
      }
   }
   ConfigurationGroupBox.prototype = new GroupBox;

  // ---------------------------------------------------------------------------------------------------------
   function BuiltinVariableGroup(parent) {
      this.__base__ = GroupBox;
      this.__base__(parent);
      var that = this;

      this.title = "Format of the built-in variable parameters";
      this.sizer = new HorizontalSizer;

      var labelWidth = this.font.width( "MMMMMMMMMMMM: " );
      var dataWidth = this.font.width( "MMMMMMMMMMMMMMMMMMMMMMM" );
      var rowStyle = {
        minLabelWidth: labelWidth,
        minDataWidth: dataWidth,
      }

      //this.sizer.addStretch();

      // Left columns
      this.colLayout1_Control = new Control;
      this.sizer.add(this.colLayout1_Control);
      this.colLayout1_Control.sizer = new VerticalSizer;

      var rankFormat = new TextEntryRow(this.colLayout1_Control, rowStyle, "rank",
      "Enter a valid C format string for the &rank; value, like '%3.3d'\nYou can also add text around the value, like 'N%3.3d'",
      "format", propertyTypes.FREE_TEXT, null);
      this.colLayout1_Control.sizer.add(rankFormat);

      // Right column
      this.colLayout2_Control = new Control;
      this.sizer.add(this.colLayout2_Control);
      this.colLayout2_Control.sizer = new VerticalSizer;

      var countFormat = new TextEntryRow(this.colLayout2_Control, rowStyle, "count",
      "Enter a valid C format string for the &count; value, like '%3.3d'\nYou can also add text around the value like 'group-%d'",
      "format", propertyTypes.FREE_TEXT, null);
      this.colLayout2_Control.sizer.add(countFormat);


      this.configure = function(editedConfigurationSet, currentConfigurationName) {
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);

         rankFormat.updateTarget(this.selectedConfiguration.builtins.rank);
         countFormat.updateTarget(this.selectedConfiguration.builtins.count);
      }

   }
   BuiltinVariableGroup.prototype = new GroupBox();


  // ---------------------------------------------------------------------------------------------------------
  // This Dialog controls the update of a configurationSet, starting at a current configuration.
  // The configurationSet may be modified or not, at the end the caller must get
  // the configurationSet and new current configuration name properties from the dialog to get the current
  // state.
  // There is currently no way to add, delete or rename configurations
  function ConfigurationDialog( parentDialog, guiParameters) {
     this.__base__ = Dialog;
     this.__base__();
     var that = this;
     this.guiParameters = guiParameters;

     // Model -
     // Keeps track of configurationSet and current configuration selected
     // in a copy, so in case of cancel nothing is changed
     // Done in 'configure'
     this.editedConfigurationSet = null;
     this.currentConfigurationName = null;

     this.newVariableCounter = 0;

     this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;


     this.sizer = new VerticalSizer;
     this.sizer.margin = 6;
     this.sizer.spacing = 4;


     // -- GUI actions callbacks
     // configuration changed (also used in initialization)
     var configurationSelectedCallback = function(configurationName) {
#ifdef DEBUG
        debug("ConfigurationDialog: configurationSelectedCallback - ConfigurationSet selected:",configurationName);
#endif
        that.selectedConfiguration = ffM_Configuration.getConfigurationByName(that.editedConfigurationSet, configurationName);
        if (that.selectedConfiguration == null) {
           throw "PROGRAM ERROR - Invalid configuration set name '" + configurationName +"'";
        }

        // Update model
        that.currentConfigurationName = configurationName;
        // Update UI
        that.variableUI.updateVariableList(that.selectedConfiguration.variableList);
        that.builtinVariable_Group.configure(that.editedConfigurationSet, that.currentConfigurationName);
        // Update the description text
        that.configurationLayoutControl.configure(that.editedConfigurationSet, that.currentConfigurationName);
     }

     var deleteConfigurationCallback = function(currentConfigurationName) {
#ifdef DEBUG
        debug("ConfigurationDialog: deleteConfigurationCallback");
#endif
        var newConfigurationName = ffM_Configuration.removeConfigurationByName(that.editedConfigurationSet, currentConfigurationName);
        if (newConfigurationName == null) {
           Console.writeln("Configuration '" + currentConfigurationName  +"' not deleted, it is the last one");
        } else {
           configurationSelectedCallback(newConfigurationName);
        }
     }

     // We duplicate the current configuration
     var duplicateConfigurationCallback = function(configurationName) {
#ifdef DEBUG
        debug("ConfigurationDialog: duplicateConfigurationCallback");
#endif
        var selectedConfiguration = ffM_Configuration.getConfigurationByName(that.editedConfigurationSet, configurationName);
        var newConfiguration = deepCopyData(selectedConfiguration);
        newConfiguration.name = createUniqueName(configurationName,ffM_Configuration.getAllConfigurationNames(that.editedConfigurationSet));
        that.editedConfigurationSet.push(newConfiguration);
        configurationSelectedCallback(newConfiguration.name);
        Console.writeln("Configuration '" + newConfiguration.name  +"' created");
     }

     // -- Model call backs
     // New variable requested in current configuration, define one with default values
     var variableDefinitionFactory = function() {
#ifdef DEBUG
          debug("ConfigurationDialog: variableDefinitionFactory - create new variable");
#endif
          that.newVariableCounter++;
          return ffM_Configuration.defineVariable("new_" + that.newVariableCounter,'','Text');
     }

     // -- Build the top level pane

     // Top pane - select configuration to operate upon
     this.configurationLayoutControl = new ConfigurationGroupBox(this, configurationSelectedCallback, duplicateConfigurationCallback, deleteConfigurationCallback);
     this.sizer.add(this.configurationLayoutControl);


     // Middle pane - define variables, their resolvers and the resolver's parameters
     this.variableUI = new VariableUIControl(this, variableDefinitionFactory);
     this.sizer.add(this.variableUI);

     this.builtinVariable_Group = new BuiltinVariableGroup(this);
     this.sizer.add(this.builtinVariable_Group);

     // Bottom pane - buttons
     this.okCancelButtons = makeLoadSaveOKCancelButtons(this);
     this.sizer.add(this.okCancelButtons);

     this.setVariableSize();
     //this.adjustToContents();

     // -- Configure before executing
     this.configure = function configure(originalConfigurationSet, configurationNameToEdit) {
        this.editedConfigurationSet = deepCopyData(originalConfigurationSet);
        this.selectedConfiguration = ffM_Configuration.getConfigurationByName(this.editedConfigurationSet, configurationNameToEdit);
        if (this.selectedConfiguration === null) {
           throw "Internal Error - current configuration not found:'" + configurationNameToEdit + "'";
        }
        this.currentConfigurationName = configurationNameToEdit;
        var configurationNames = ffM_Configuration.getAllConfigurationNames(this.editedConfigurationSet);
        // Initialize content
        this.configurationLayoutControl.configure(this.editedConfigurationSet, this.currentConfigurationName);
        this.builtinVariable_Group.configure(this.editedConfigurationSet, this.currentConfigurationName);
        configurationSelectedCallback(this.currentConfigurationName);
     }


     this.loadFileAction = function loadFileAction(fileName) {
        var result = ffM_Configuration.loadConfigurationFile(fileName);
          
        // Error, no configuration
        if (result.configurations === null) {
           var msg = new MessageBox(
            result.messages.join("\n"),
                  "Load configuration error", 
                  StdIcon_Error, StdButton_Ok );
            msg.execute();
        } else {
          // Configuration present, if there are also warnings, add them to the message box

          var text = result.configurations.length.toString() + " configuration(s)\n";
          for (var i = 0; i<result.configurations.length; i++){
            text += "  " + result.configurations[i].name + "\n";
          }
          text += "loaded from file\n" + fileName;

          var title = "Configurations loaded";
          if (result.configurations.length ===1) {
            title = "Configuration " +  result.configurations[0].name + " loaded";
          }

          if (result.messages.length === 0) {
             var msg = new MessageBox( 
                    text,
                    title, 
                    StdIcon_Information, StdButton_Ok );
          } else {
             var msg = new MessageBox(
                    text + "\n" + result.messages.join("\n"),
                    "Warning occured on configuration load", 
                    StdIcon_Warning, StdButton_Ok );
          }
          // TODO We could ask for confirmation in case of warning 
          msg.execute();

          var replaceCurrent = false;
          for (var i=0; i<result.configurations.length; i++) {
            var loadedConfiguration = result.configurations[i];
            var loadedConfigurationName = loadedConfiguration.name;
            
            // Replace or insert
            // TODO may be support method
            var replaced = false;
            for (var j=0; j<that.editedConfigurationSet.length; j++) {
              if (this.currentConfigurationName === loadedConfigurationName) {
                replaceCurrent = true;
              }
              if (that.editedConfigurationSet[j].name === loadedConfigurationName) {
                that.editedConfigurationSet.splice(j,1);
                replaced = true;
                break;
              }
            }
        
            // TODO - Maintainn a canonical order
            that.editedConfigurationSet.push(loadedConfiguration);

            Console.writeln("Configuration '" + loadedConfigurationName  +"' " + (replaced ? "replaced" : "added"));

            // Update UI if the current configuration was loaded OR if a single configuration was loaded
            if (replaceCurrent || result.configurations.length === 1) {
              configurationSelectedCallback(loadedConfigurationName);
            }

          }
          
        }
      }

     this.saveCurrentToFileAction = function saveCurrentToFileAction(fileName) {
        var result = ffM_Configuration.saveConfigurationFile(fileName, [this.selectedConfiguration]);
        if (result == null) {
           var msg = new MessageBox( "Configuration '" + this.currentConfigurationName + "' saved to file\n" + fileName,
                  "Configuration '" + this.currentConfigurationName + "' saved", StdIcon_Information, StdButton_Ok );
        } else {
           var msg = new MessageBox(result,
                  "Write configuration error", StdIcon_Error, StdButton_Ok );
        }
        msg.execute();
     }

     this.saveAllToFileAction = function saveAllToFileAction(fileName) {
        var result = ffM_Configuration.saveConfigurationFile(fileName, this.editedConfigurationSet);
        if (result == null) {
          var text = "Configurations\n";
          for (var i=0; i<this.editedConfigurationSet.length; i++) {
            text += "  " + this.editedConfigurationSet[i].name + "\n";
          }
          text += "saved to file\n" + fileName;
          var msg = new MessageBox( text,
                  "Configurations saved", StdIcon_Information, StdButton_Ok );
        } else {
           var msg = new MessageBox(result,
                  "Write configuration error", StdIcon_Error, StdButton_Ok );
        }
        msg.execute();
     }



  }
  ConfigurationDialog.prototype = new Dialog;



  return {
    // makeDialog must be followed by a call to configure()
     makeDialog: function(parent, guiParameters) {
        return new ConfigurationDialog(parent, guiParameters);
     },
     ConfigurationSelection_ComboBox: ConfigurationSelection_ComboBox,

  }

}) ();
