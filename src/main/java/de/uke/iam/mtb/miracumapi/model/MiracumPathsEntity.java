package de.uke.iam.mtb.miracumapi.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(schema = "miracum", name = "paths")
@Data
public class MiracumPathsEntity {

    @Id
    @Column(name = "patient_id")
    private String patientId;
    @Column(name = "path_to_output")
    private String pathToOutput;
    @Column(name = "path_to_maf")
    private String pathToMaf;
    @Column(name = "path_to_report")
    private String pathToReport;
    @Column(name = "path_to_input")
    private String pathToInput;
    @Column(name = "path_to_logs")
    private String pathToLogs;
}
