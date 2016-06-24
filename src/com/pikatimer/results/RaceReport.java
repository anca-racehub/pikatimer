/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.results;

/**
 *
 * @author jcgarner
 */



import com.pikatimer.race.Race;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

@Entity
@DynamicUpdate
@Table(name="race_outputs")
public class RaceReport {
    
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final IntegerProperty raceProperty= new SimpleIntegerProperty();
    private final StringProperty reportTypeProperty = new SimpleStringProperty("UNSET");
    
    private List<RaceOutputTarget> raceOutputTargetList;
    private final ObservableList<RaceOutputTarget> raceOutputTargets = FXCollections.observableArrayList();
    
    private ReportTypes reportType;
    private Race race;
    
    public RaceReport(){
        
    }
//    id int primary key
    @Id
    @GenericGenerator(name="race_outputs_id" , strategy="increment")
    @GeneratedValue(generator="race_outputs_id")
    @Column(name="ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
 
    //    uuid varchar,
    @Column(name="uuid")
    public String getUUID() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return uuidProperty.getValue(); 
    }
    public void setUUID(String  uuid) {
        uuidProperty.setValue(uuid);
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty uuidProperty() {
        return uuidProperty; 
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
//    output_type varchar,
    @Enumerated(EnumType.STRING)
    @Column(name="output_type")
    public ReportTypes getReportType() {
        return reportType;
    }
    public void setReportType(ReportTypes t) {
        
        if (t != null && (reportType == null || ! reportType.equals(t)) ){
            
            reportType = t;
            reportTypeProperty.setValue(reportTypeProperty.toString());
        }
    }
    
    @OneToMany(mappedBy="raceReport",cascade={CascadeType.PERSIST, CascadeType.REMOVE},fetch = FetchType.LAZY)
    public List<RaceOutputTarget> getRaceOutputTargets() {
        return raceOutputTargetList;
    }
    public void setRaceOutputTargets(List<RaceOutputTarget> rr) {
        raceOutputTargetList = rr;
        if (rr == null) {
            System.out.println("RaceReport.setRaceOutputTarget(list) called with null list");
        } else {
            System.out.println("RaceReport.setRaceOutputTargets(list) " + "( " + IDProperty.getValue().toString() + ")" + " now has " + raceOutputTargetList.size() + " Output Destinations");
        }
        raceOutputTargets.setAll(rr);
    }
    public ObservableList<RaceOutputTarget> outputTargets() {
        return raceOutputTargets;
    }
    public void addRaceOutputTarget(RaceOutputTarget t) {
        raceOutputTargets.add(t);
        t.setRaceReport(this);
        raceOutputTargetList = raceOutputTargets.sorted();
        //raceReportsList = raceReports.sorted();
    }
    public void removeRaceOutputTarget(RaceOutputTarget w) {
        raceOutputTargets.remove(w);
        raceOutputTargetList = raceOutputTargets.sorted();
    }
    
}
