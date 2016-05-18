/* 
 * Copyright (C) 2016 John Garner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.race;

import com.pikatimer.results.ResultsDAO;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingLocation;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;



/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLRaceDetailsController {
    private RaceDAO raceDAO; 
    
    @FXML private HBox raceNameHBox;
    @FXML private TextField raceNameTextField; 
    @FXML private TextField raceDistanceTextField; 
    @FXML private ChoiceBox distanceUnitChoiceBox; 
    @FXML private TextField raceCutoffTimeTextField;
    @FXML private Label raceCutoffTimePaceLabel; 
    @FXML private TableView<Wave> waveStartsTableView;
    @FXML private TableColumn<Wave, String> waveIDTableColumn;
    @FXML private TableColumn<Wave, String> waveNameTableColumn;
    @FXML private TableColumn<Wave, String> waveStartTimeTableColumn;                
    @FXML private TableColumn<Wave, String> waveMaxStartTimeTableColumn; 
    @FXML private TableColumn<Wave, WaveAssignment> waveAssignmentMethodTableColumn;
    @FXML private TableColumn<Wave, String> waveAssignmentStartTableColumn;
    @FXML private TableColumn<Wave, String> waveAssignmentEndTableColumn;
    @FXML private TableView<Split> raceSplitsTableView;
    @FXML private TableColumn<Split, String> splitNameTableColumn;
    @FXML private TableColumn<Split,TimingLocation> splitLocationTableColumn; 
    @FXML private TableColumn<Split, String> splitDistanceTableColumn;
    @FXML private TableColumn<Split, String> splitCutoffTableColumn; 
    @FXML private Button deleteSplitButton;
    @FXML private CheckBox waveStartsCheckBox; 
    @FXML private HBox startTimeHBox; 
    @FXML private VBox waveStartsVBox; 
    @FXML private Button deleteWaveButton;
    @FXML private TextField raceStartTimeTextField; 
    @FXML private VBox splitsVBox;
    @FXML private CheckBox splitsCheckBox; 
    @FXML private Button awardSetupButton;
    @FXML private Button ageGroupSetupButton;
    @FXML private Button courseRecordsButton;

    
    Race selectedRace; 
    ObservableList<Wave> raceWaves;
    ObservableList<Split> raceSplits; 
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO

        // get a RaceDAO
        raceDAO = RaceDAO.getInstance(); 
        raceNameHBox.disableProperty().bind(Bindings.size(raceDAO.listRaces()).lessThanOrEqualTo(1));
        raceNameHBox.managedProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        raceNameHBox.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        ObservableList<Unit> unitList = FXCollections.observableArrayList(Arrays.asList(Unit.values()));
        raceWaves = FXCollections.observableArrayList(); 
        //distanceUnitChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(Unit.values()))); 
        distanceUnitChoiceBox.setItems(unitList);
        distanceUnitChoiceBox.setValue(Unit.MILES);
        
        distanceUnitChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Unit>() {
            @Override
            public void changed(ObservableValue<? extends Unit> observableValue, Unit o, Unit n) {
                System.out.println("distanceUnitChoiceBox event");
                selectedRace.setRaceDistanceUnits(n);
                updateRaceCutoffPace();
                raceDAO.updateRace(selectedRace);        
            }
        });
        
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
        raceDistanceTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                try {
                    if (!newValue.isEmpty()) {
                        new BigDecimal(raceDistanceTextField.getText());
                    }
                } catch (Exception e) {
                    raceDistanceTextField.setText(oldValue);
                }
                
        });
        // but only update when the textfield focus changes. 
        raceDistanceTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceDistanceTextField out focus");
                if ( ! raceDistanceTextField.getText().equals(selectedRace.getRaceDistance().toPlainString()) ) {
                    updateRaceDistance();
                }
            }
        });
        
        
        // Race (wave) Time stuff
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
        raceStartTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if (newValue.matches("([3-9]|[012]:)")) {
                //Integer pos = raceStartTimeTextField.getCaretPosition();
                raceStartTimeTextField.setText("0" + newValue);
                Platform.runLater(() -> {
                    raceStartTimeTextField.positionCaret(newValue.length()+2);
                });
                
            } else if (    newValue.isEmpty() || 
                    newValue.matches("([012]|[01][0-9]|2[0-3])") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5]?") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9]:[0-5]?") ){
                System.out.println("Possiblely good Race Cutoff Time (newValue: " + newValue + ")");
            } else if(newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9](\\.[0-9]*)?)?") ) { // Looks like a time, lets check
                System.out.println("Testing Race Start Time (newValue: " + newValue + ")");
            
                try {
                    if (!newValue.isEmpty()) {
                        //LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                        LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                    }
                } catch (Exception e) {
                    raceStartTimeTextField.setText(oldValue);
                    System.out.println("Exception Bad Race Start Time (newValue: " + newValue + ")");
                    e.printStackTrace();
                }
            } else {
                raceStartTimeTextField.setText(oldValue);
                System.out.println("Bad Race Start Time (newValue: " + newValue + ")");
            }
                
        });
        // but only update when the textfield focus changes. 
        raceStartTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceStartTimeTextField out focus");
                try {
                    if (!raceStartTimeTextField.getText().isEmpty()) {
                        LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                    }
                } catch (Exception e) {
                    raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
                    System.out.println("Bad Race Start Time (newValue: " + raceStartTimeTextField.getText() + ")");
                }
                if ( ! raceStartTimeTextField.getText().equals(raceWaves.get(0).getWaveStart()) ) {
                    updateRaceStartTime();
                } else {
                    System.out.println("Unchaged Race Start, not saving: \"" + raceWaves.get(0).getWaveStart() + "\" vs " + raceStartTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        startTimeHBox.visibleProperty().bind(waveStartsCheckBox.selectedProperty().not());
        startTimeHBox.managedProperty().bind(waveStartsCheckBox.selectedProperty().not());
        
        // Race (wave) Time stuff
        waveNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveNameTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveName(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveNameTableColumn.setComparator(new AlphanumericComparator());
        
        waveStartTimeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());     
        waveStartTimeTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            System.out.println("waveStartTimeTextField out focus");
                try {
                    if (!t.getNewValue().isEmpty()) {
                        LocalTime.parse(t.getNewValue(), DateTimeFormatter.ISO_LOCAL_TIME );
                        w.setWaveStart(t.getNewValue());
                        raceDAO.updateWave(w);
                        ResultsDAO.getInstance().reprocessAll(w);
                    }
                } catch (Exception e) {
                    System.out.println("Bad Race Wave Start Time (newValue: " + t.getNewValue() + ")");
                    w.setWaveStart(t.getOldValue());
                    
                }
        });
                
        waveMaxStartTimeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn()); 
        waveMaxStartTimeTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            if (t.getNewValue().matches("[0-9]+") ) {
                int minutes = Integer.valueOf(t.getNewValue());
                w.setWaveMaxStart(Duration.ofSeconds(minutes * 60L).toNanos());
                raceDAO.updateWave(w); 
            } else if (t.getNewValue().matches("[0-9][0-9]*:[0-5][0-9]") ) {
                String[] split = t.getNewValue().split(":");
                int minutes = Integer.valueOf(split[0]);
                int seconds = Integer.valueOf(split[1]);
                w.setWaveMaxStart(Duration.ofSeconds(minutes * 60L + seconds).toNanos());
                raceDAO.updateWave(w);
            } else if ( t.getNewValue().isEmpty() && ! t.getOldValue().isEmpty()) {
                w.setWaveMaxStart(0L);
                raceDAO.updateWave(w);
            } else {
                t.consume();
                w.waveMaxStartStringProperty().setValue(t.getOldValue());
            }
        });
        
        // Bib start/stop for the wave
        //ObservableList<String> waveAssignmentList = FXCollections.observableArrayList(Arrays.asList(WaveAssignment.values().toString()));
        waveAssignmentMethodTableColumn.setCellFactory(ComboBoxTableCell.<Wave, WaveAssignment>forTableColumn(WaveAssignment.values()));
        waveAssignmentMethodTableColumn.setOnEditCommit((CellEditEvent<Wave, WaveAssignment> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentMethod(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentStartTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentStartTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentStart(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentStartTableColumn.setComparator(new AlphanumericComparator());

        
        waveAssignmentEndTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentEndTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentEnd(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentEndTableColumn.setComparator(new AlphanumericComparator());
        
        
        
        
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
        raceCutoffTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if ( newValue.isEmpty() || newValue.matches("([0-9]*|[0-9]*:[0-5]?)") ) {
                System.out.println("Possiblely good Race Cutoff Time (newValue: " + newValue + ")");
            } else if(newValue.matches("[0-9]*:[0-5][0-9]") ) { // Looks like a HH:MM time, lets check
                System.out.println("Looks like a valid Race Cutoff Time (newValue: " + newValue + ")");
            } else {
                raceCutoffTimeTextField.setText(oldValue);
                System.out.println("Bad Race Cutoff Time (newValue: " + newValue + ")");
            }
                
        });
        // but only update when the textfield focus changes. 
        raceCutoffTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceCutoffTimeTextField out focus");

                if ( ! raceCutoffTimeTextField.getText().equals(selectedRace.raceCutoffProperty().getValueSafe()) ) {
                    if (raceCutoffTimeTextField.getText().matches("[0-9][0-9]*:[0-5][0-9]") ||raceCutoffTimeTextField.getText().isEmpty() ) {
                        updateRaceCutoffTime(); 
                    } else {
                        raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe());
                        System.out.println("raceCutoffTimeTextField out focus with bad time, reverting... ");
                    }
                } else {
                    System.out.println("Unchaged Cutoff time, not saving: \"" + selectedRace.raceCutoffProperty().getValueSafe() + "\" vs " + raceCutoffTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        // Split table stuff
        splitNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        splitNameTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
            Split w = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setSplitName(t.getNewValue());
            raceDAO.updateSplit(w);
        });
        
        splitLocationTableColumn.setCellFactory(ComboBoxTableCell.<Split, TimingLocation>forTableColumn(TimingDAO.getInstance().listTimingLocations()));
        splitLocationTableColumn.setOnEditCommit((CellEditEvent<Split, TimingLocation> t) -> {
            Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            s.setTimingLocation(t.getNewValue());
            raceDAO.updateSplit(s);
        });
        
        splitDistanceTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        splitDistanceTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
            BigDecimal dist;
            Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            try {
                dist = new BigDecimal(t.getNewValue());
                s.setSplitDistance(dist);
                raceDAO.updateSplit(s);
            } catch (Exception e) {
                // not a number
                s.setSplitDistance(s.getSplitDistance());
            }
        });
        splitDistanceTableColumn.setComparator(new AlphanumericComparator());
        
        Label splitCutoffTableColumnLabel = new Label("Cutoff");
        splitCutoffTableColumnLabel.setTooltip(new Tooltip("Optional Cutoff time in HH:MM"));
        splitCutoffTableColumn.setGraphic(splitCutoffTableColumnLabel);
        splitCutoffTableColumn.setText("");
        splitCutoffTableColumn.setCellFactory(TextFieldTableCell.forTableColumn()); 
        splitCutoffTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
            Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            if (t.getNewValue().matches("[0-9]+") ) {
                int hours = Integer.valueOf(t.getNewValue());
                s.setSplitCutoff(Duration.ofSeconds(hours * 3600L).toNanos());
                raceDAO.updateSplit(s); 
            } else if (t.getNewValue().matches("[0-9][0-9]*:[0-5][0-9]") ) {
                String[] split = t.getNewValue().split(":");
                int hours = Integer.valueOf(split[0]);
                int minutes = Integer.valueOf(split[1]);
                s.setSplitCutoff(Duration.ofSeconds(hours * 3600L + minutes * 60L).toNanos());
                raceDAO.updateSplit(s);
            } else if ( t.getNewValue().isEmpty() && ! t.getOldValue().isEmpty()) {
                s.setSplitCutoff(0L);
                raceDAO.updateSplit(s);
            } else {
                t.consume();
                s.splitCutoffStringProperty().setValue(t.getOldValue());
            }
        });
        
        
        
//        raceSplitsTableView.setOnDragDetected(new EventHandler<MouseEvent>() { //drag
//            @Override
//            public void handle(MouseEvent event) {
//                // drag was detected, start drag-and-drop gesture
//                String selected = raceSplitsTableView.getSelectionModel().getSelectedItem().idProperty().toString();
//                if(selected !=null){
//                                   
//                    Dragboard db = raceSplitsTableView.startDragAndDrop(TransferMode.ANY);
//                    ClipboardContent content = new ClipboardContent();
//                    content.putString(selected);
//                    db.setContent(content);
//                    event.consume(); 
//                }
//            }
//        });
//
// raceSplitsTableView.setOnDragOver(new EventHandler<DragEvent>() {
//                @Override
//                public void handle(DragEvent event) {
//                    // data is dragged over the target 
//                    Dragboard db = event.getDragboard();
//                    if (event.getDragboard().hasString()){
//                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
//                    }
//                    event.consume();
//                }
//            });
//
//raceSplitsTableView.setOnDragDropped(new EventHandler<DragEvent>() {
//                @Override
//                public void handle(DragEvent event) {
//                    Dragboard db = event.getDragboard();
//                    boolean success = false;
//                    if (event.getDragboard().hasString()) {            
//
//                        String text = db.getString();
//                        
//                        //raceSplits.add(text);
//                        //raceSplitsTableView.setItems(raceSplits);
//                        success = true;
//                    }
//                    event.setDropCompleted(success);
//                    event.consume();
//                } 
//            }); 
        
        
        
        
        

    }    
    
    public void selectRace(Race r) {
        selectedRace = r;
        
        if (selectedRace != null) {
            System.out.println("Non-Null race, populate all fields out");
            //Setup the Race Name
            raceNameTextField.setText(selectedRace.getRaceName());
            if(raceNameTextField.disableProperty().get()) {
                raceNameTextField.requestFocus();
            } else {
                raceStartTimeTextField.requestFocus(); 
            }
            
            //Setup the distance
            raceDistanceTextField.setText(selectedRace.getRaceDistance().toPlainString());
            distanceUnitChoiceBox.setValue(selectedRace.getRaceDistanceUnits()); 
            
            
            //setup the cutoff and pace

            // setup the cutoff label so that it displace the pace in M/Mi if 
            // it is set, otherwise blank it

            //Setup the splits VBox


            //Setup the wave starts VBOX
            raceWaves=selectedRace.wavesProperty(); 
            waveStartsTableView.setItems(raceWaves);
            if (raceWaves.isEmpty()) {
                // no waves. Let's create one with some default values
               Wave wave = new Wave(selectedRace);
               wave.setWaveName("Wave 1");
               wave.setWaveAssignmentMethod(WaveAssignment.BIB);
               
               raceDAO.addWave(wave);
            } 
           
            deleteWaveButton.disableProperty().bind(Bindings.or(
                waveStartsTableView.getSelectionModel().selectedItemProperty().isNull(),
                Bindings.size(waveStartsTableView.getItems()).lessThan(2))
            );
            waveStartsVBox.managedProperty().bind(waveStartsCheckBox.selectedProperty());
            waveStartsVBox.visibleProperty().bind(waveStartsCheckBox.selectedProperty());
            // if we have more than one wave then let's set the waveStartsCheckBox to true.
            if (raceWaves.size() > 1) {
                waveStartsCheckBox.setSelected(true); 
            } else {
                waveStartsCheckBox.setSelected(false);
            }
            waveStartsCheckBox.disableProperty().bind(Bindings.size(waveStartsTableView.getItems()).greaterThan(1));
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            // if there is only one race, blank out the bib range options
        
            
            
            splitsVBox.managedProperty().bind(splitsCheckBox.selectedProperty());
            splitsVBox.visibleProperty().bind(splitsCheckBox.selectedProperty());
            
            raceSplits=selectedRace.splitsProperty(); 
            raceSplitsTableView.setItems(raceSplits);
            if (raceSplits.isEmpty()) {
                System.out.println("No Splits found, creating two...");
                // no waves. Let's create one with some default values
               Split startSplit = new Split(selectedRace);
               startSplit.setSplitName("Start");
               startSplit.setSplitDistance(BigDecimal.ZERO);
               startSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
               startSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(0));
               //startSplit.setPosition(1);
               raceDAO.addSplit(startSplit);
               
               Split finishSplit = new Split(selectedRace);
               finishSplit.setSplitName("Finish");
               finishSplit.setSplitDistance(selectedRace.getRaceDistance());
               finishSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
               finishSplit.setSplitCutoff(selectedRace.getRaceCutoff());
               finishSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(1));
               //finishSplit.setPosition(2);
               raceDAO.addSplit(finishSplit);
            } 
            if (raceSplits.size() > 2) {
                splitsCheckBox.setSelected(true); 
            } else {
                splitsCheckBox.setSelected(false);
            }
            splitsCheckBox.disableProperty().bind(Bindings.size(raceSplitsTableView.getItems()).greaterThan(2));
            
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            
            
            
            
            
            // Cutoff stuff
            raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe()); 
            
            updateRaceCutoffPace();
            
        } else {
            System.out.println("Null race, de-populate all fields out");

            // blank out everything 
            // the pane will be disabled but let's not confuse things
        }
    }
    
    public void updateRaceName(ActionEvent fxevent){
        selectedRace.setRaceName(raceNameTextField.getText());
        raceDAO.updateRace(selectedRace);
    }
    
    public void updateRaceDistance(ActionEvent fxevent){
        updateRaceDistance();
    }
    
    public void updateRaceDistance() {
        //TODO: If the location is referenced by a split, 
        //prompt to reassign the split to a new location or cancel the edit. 
        //Do we have a parsable number?
        BigDecimal dist;
        try {
            dist = new BigDecimal(raceDistanceTextField.getText());
            selectedRace.setRaceDistance(dist);
            selectedRace.setRaceDistanceUnits((Unit)distanceUnitChoiceBox.getValue());
            raceDAO.updateRace(selectedRace);
        } catch (Exception e) {
            // not a number
            dist = selectedRace.getRaceDistance();
            raceDistanceTextField.setText(dist.toPlainString());
        }
        updateRaceCutoffPace();
    }
    
//    public void updateRaceCutoffTime(){
//        selectedRace.setRaceCutoff(raceCutoffTimeTextField.getText());
//        raceDAO.updateRace(selectedRace);
//    }
    
    public void updateRaceStartTime(ActionEvent fxevent){
        //updateRaceStartTime();
    }
    public void updateRaceStartTime(){ // really set the 1st wave's start time
        raceWaves.get(0).setWaveStart(raceStartTimeTextField.getText());
        //selectedRace.setRaceStart(raceStartTimeTextField.getText());
        //raceDAO.updateRace(selectedRace);
        raceDAO.updateWave(raceWaves.get(0));
        ResultsDAO.getInstance().reprocessAll(raceWaves.get(0));
    }
    
    public void updateRaceCutoffTime(ActionEvent fxevent){
        updateRaceCutoffTime();
    }
    
    public void updateRaceCutoffTime(){
        if (raceCutoffTimeTextField.getText().isEmpty()) {
            selectedRace.setRaceCutoff(0L);
        } else { 
            //LocalTime nt = LocalTime.parse(raceCutoffTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
            
            String[] split = raceCutoffTimeTextField.getText().split(":");
            int hours = Integer.valueOf(split[0]);
            int minutes = Integer.valueOf(split[1]);

            selectedRace.setRaceCutoff(Duration.ofSeconds(hours * 3600L + minutes * 60L).toNanos());
        }
        raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe());
        raceDAO.updateRace(selectedRace);
        // caclulate the MM:SS/mi 
       updateRaceCutoffPace();
    }
    
    public void updateRaceCutoffPace(){
        System.out.println("Pace Calc inputs: " + selectedRace.getRaceCutoff() + " and " + selectedRace.getRaceDistance().floatValue() );
        Duration pace = Duration.ofNanos(Math.round(selectedRace.getRaceCutoff()/selectedRace.getRaceDistance().doubleValue()));
        raceCutoffTimePaceLabel.setText(DurationFormatter.durationToString(pace, 0, Boolean.FALSE) + " min/" + selectedRace.getRaceDistanceUnits().toShortString());
    }
    
    public void addWave(ActionEvent fxevent){
        Wave wave = new Wave(selectedRace);
        wave.setWaveName("Wave " + (raceWaves.size()+1));
        wave.setWaveAssignmentMethod(WaveAssignment.BIB);
        //wave.setWaveStart(selectedRace.getRaceStart());
        wave.setWaveStart(raceWaves.get(0).getWaveStart());
        raceDAO.addWave(wave);
    }
    
    public void deleteWave(ActionEvent fxevent){
        //removeParticipants(FXCollections.observableArrayList(waveStartsTableView.getSelectionModel().getSelectedItems()));
        ObservableList deleteMe = FXCollections.observableArrayList(waveStartsTableView.getSelectionModel().getSelectedItems());
        Wave w;
        Iterator<Wave> deleteMeIterator = deleteMe.iterator();
        while (deleteMeIterator.hasNext()) {
            w = deleteMeIterator.next();
            raceDAO.removeWave(w); 
        }

    }
    
    public void addSplit(ActionEvent fxevent){
        Split newSplit = new Split(selectedRace);
        newSplit.setSplitName("New Split");
        newSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
        newSplit.setSplitDistance(BigDecimal.valueOf(0));
        newSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(1));
        if(raceSplitsTableView.getSelectionModel().getSelectedItems().size()> 0 ) {
            Integer pos = raceSplitsTableView.getSelectionModel().getSelectedItem().getPosition(); 
            if (pos > 1) {
                BigDecimal a = raceSplitsTableView.getItems().get(pos-2).getSplitDistance();
                BigDecimal b = raceSplitsTableView.getItems().get(pos-1).getSplitDistance();
//                BigDecimal c = a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) );
//                System.out.println("addSplit: " + a + " and " + b + " avg: " + c);
                newSplit.setSplitDistance(a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) ) );
                newSplit.setPosition(pos);
            }
        }
        raceDAO.addSplit(newSplit);
    }
    
    public void deleteSplit(ActionEvent fxevent){
        //removeParticipants(FXCollections.observableArrayList(waveStartsTableView.getSelectionModel().getSelectedItems()));
        ObservableList deleteMe = FXCollections.observableArrayList(raceSplitsTableView.getSelectionModel().getSelectedItems());
        Split s;
        Iterator<Split> deleteMeIterator = deleteMe.iterator();
        while (deleteMeIterator.hasNext()) {
            s = deleteMeIterator.next();
            raceDAO.removeSplit(s); 
        }

    }
    
    public void setupCourseRecords(ActionEvent fxevent){
        Alert alert = new Alert(AlertType.WARNING, "Course record detection is not yet implemented.");
        alert.showAndWait();
    }
    
    public void setupAgeGroups(ActionEvent fxevent){
        final AgeGroups ageGroups;
        if (selectedRace.getAgeGroups() == null) {
            ageGroups = new AgeGroups();
        } else {
            ageGroups = selectedRace.getAgeGroups();
        }
        
        
        // Create the custom dialog.
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Race AG Setup");
        dialog.setHeaderText("Race Age Group Setup");

        // Set the button types.
        //ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the grid for the labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5, 5, 5, 5));
        
        // AG Start (the 1 -> X part)
        TextField agStart = new TextField();
        TextFormatter<String> AGSformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agStart.setPrefWidth(33);
        agStart.setTooltip(new Tooltip("Sets the max age for the first age group. i.e. 1 -> X"));  
        agStart.setTextFormatter(AGSformatter);
        agStart.setText(ageGroups.getAGStart().toString());
        
        
        grid.add(new Label("Start Age"), 0, 0);
        grid.add(agStart, 1, 0);
        
        // Increment
        ChoiceBox<Integer> agIncChoiceBox = new ChoiceBox(FXCollections.observableArrayList(5, 10));
        agIncChoiceBox.getSelectionModel().select(ageGroups.getAGIncrement());
        grid.add(new Label("Increment"), 0, 1);
        grid.add(agIncChoiceBox, 1, 1);
        
        
        // Masters
        TextField agMasters = new TextField();
        TextFormatter<String> AGMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agMasters.setPrefWidth(33);
        agMasters.setTooltip(new Tooltip("Sets the starting age for the Masters categories."));  
        agMasters.setTextFormatter(AGMformatter);
        agMasters.setText(ageGroups.getMasters().toString());
        grid.add(new Label("Masters"), 0, 2);
        grid.add(agMasters, 1, 2);
        
        agStart.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                //System.out.println("agStart out of focus...");
                Integer st = Integer.parseUnsignedInt(agStart.getText());
                Integer inc = agIncChoiceBox.getSelectionModel().getSelectedItem();
                
                if (st < (inc - 1)) {
                    st = inc - 1;
                    agStart.setText(st.toString());
                } else if ((st+1)%inc != 0) { // oops, the start is not a good value
                    st = ((st/inc)*inc)-1;
                    agStart.setText(st.toString()); // now it should be ;-)
                }
            }
        });
        
        agIncChoiceBox.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                //System.out.println("agIncChoiceBox out of focus...");
                Integer st = Integer.parseUnsignedInt(agStart.getText());
                Integer inc = agIncChoiceBox.getSelectionModel().getSelectedItem();
               
                if (st < (inc - 1)) {
                    st = inc - 1;
                    agStart.setText(st.toString());
                } else if ((st+1)%inc != 0) { // oops, the start is not a good value
                    st = ((st/inc)*inc)-1;
                    agStart.setText(st.toString()); // now it should be ;-)
                }
            }
        });
        
        
        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        //Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ageGroups.setAGIncrement(agIncChoiceBox.getSelectionModel().getSelectedItem());
                ageGroups.setMasters(Integer.parseUnsignedInt(agMasters.getText()));
                ageGroups.setAGStart(Integer.parseUnsignedInt(agStart.getText()));
                return Boolean.TRUE;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();

        result.ifPresent(dialogOK -> {
            if (dialogOK) {
                //save it
                if (selectedRace.getAgeGroups() == null) {
                    selectedRace.setAgeGroups(ageGroups);
                }               
                raceDAO.updateRace(selectedRace);
                //raceDAO.updateAwards(awards);
            }
            //System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
        });
    }
    
    public void setupAwards(ActionEvent fxevent){
        final RaceAwards awards;
        if (selectedRace.getAwards() == null) {
            awards = new RaceAwards();
        } else {
            awards = selectedRace.getAwards();
        }
        
        
        // Create the custom dialog.
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Race Award Setup");
        dialog.setHeaderText("Race Award Depth Setup");

        // Set the button types.
        //ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the grid for the labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5, 5, 5, 5));
        
        grid.add(new Label("Male"),1,0);
        grid.add(new Label("Female"), 2, 0);
        grid.add(new Label("Pull?"),3,0);

        
        // Overall Awards
        TextField overallMale = new TextField();
        TextFormatter<String> OMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        overallMale.setPrefWidth(33);
        
        overallMale.setTextFormatter(OMformatter);
        overallMale.setText(awards.getDepth("OverallMale").toString());
        
        TextField overallFemale = new TextField();
        TextFormatter<String> OFformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        overallFemale.setPrefWidth(33);
        overallFemale.setTextFormatter(OFformatter);
        overallFemale.setText(awards.getDepth("OverallFemale").toString());
        
        CheckBox overallPull = new CheckBox();
        overallPull.setTooltip(new Tooltip("Select if you want to pull the winners from subsequent awards."));  
        overallPull.setSelected(awards.getPull("Overall"));
        

        grid.add(new Label("Overall:"), 0, 1);
        grid.add(overallMale, 1, 1);
        grid.add(overallFemale, 2, 1);
        grid.add(overallPull,3,1);
        
        // Masters
        TextField mastersMale = new TextField();
        TextFormatter<String> MMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        mastersMale.setPrefWidth(33);
        
        mastersMale.setTextFormatter(MMformatter);
        mastersMale.setText(awards.getDepth("MastersMale").toString());
        
        TextField mastersFemale = new TextField();
        TextFormatter<String> MFformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        mastersFemale.setPrefWidth(33);
        mastersFemale.setTextFormatter(MFformatter);
        mastersFemale.setText(awards.getDepth("MastersFemale").toString());
        
        CheckBox mastersPull = new CheckBox();
        mastersPull.setTooltip(new Tooltip("Select if you want to pull the winners from subsequent awards."));  
        mastersPull.setSelected(awards.getPull("Masters"));
        

        grid.add(new Label("Masters:"), 0, 2);
        grid.add(mastersMale, 1, 2);
        grid.add(mastersFemale, 2, 2);
        grid.add(mastersPull,3,2);
        
        // AG Awards
        TextField agMale = new TextField();
        TextFormatter<String> AGMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agMale.setPrefWidth(33);
        
        agMale.setTextFormatter(AGMformatter);
        agMale.setText(awards.getDepth("AGMale").toString());
        
        TextField agFemale = new TextField();
        TextFormatter<String> AGFformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agFemale.setPrefWidth(33);
        agFemale.setTextFormatter(AGFformatter);
        agFemale.setText(awards.getDepth("AGFemale").toString());
        
        CheckBox agPull = new CheckBox();
        agPull.setTooltip(new Tooltip("Select if you want to pull the winners from subsequent awards."));  
        agPull.setSelected(awards.getPull("AG"));
        

        grid.add(new Label("AG:"), 0, 3);
        grid.add(agMale, 1, 3);
        grid.add(agFemale, 2, 3);
        grid.add(agPull,3,3);
        
        

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        //Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                awards.setDepth("OverallMale",Integer.parseUnsignedInt(overallMale.getText()));
                awards.setDepth("OverallFemale",Integer.parseUnsignedInt(overallFemale.getText()));
                awards.setPull("Overall",overallPull.isSelected());
                
                awards.setDepth("MastersMale",Integer.parseUnsignedInt(mastersMale.getText()));
                awards.setDepth("MastersFemale",Integer.parseUnsignedInt(mastersFemale.getText()));
                awards.setPull("Masters",mastersPull.isSelected());
                
                awards.setDepth("AGMale",Integer.parseUnsignedInt(mastersMale.getText()));
                awards.setDepth("AGFemale",Integer.parseUnsignedInt(mastersFemale.getText()));
                awards.setPull("AG",mastersPull.isSelected());
                
                return Boolean.TRUE;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();

        result.ifPresent(dialogOK -> {
            if (dialogOK) {
                //save it
                if (selectedRace.getAwards() == null) {
                    selectedRace.setAwards(awards);
                }               
                raceDAO.updateRace(selectedRace);
                //raceDAO.updateAwards(awards);
            }
            //System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
        });
    }
}
