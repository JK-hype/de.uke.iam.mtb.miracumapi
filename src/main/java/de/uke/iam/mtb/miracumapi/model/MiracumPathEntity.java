package de.uke.iam.mtb.miracumapi.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(schema = "miracum", name = "paths")
@Data
public class MiracumPathEntity {

    @Id
    private String patientId;
    private String pathToOutput;
    private String pathToMaf;
    private String pathToReport;
    private String pathToInput;
    private String pathToLogs;
}
