package de.uke.iam.mtb.miracumapi.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import de.uke.iam.mtb.dto.enums.Protocol;
import lombok.Data;

@Entity
@Table(schema = "miracum", name = "input_details")
@Data
public class MiracumInputDetailsEntity {

    @Id
    @Column(name = "patient_id")
    private String patientId;
    private String sex;
    @Column(name = "annotation_of_germline_findings")
    private Boolean annotationOfGermlineFindings;
    @Enumerated(EnumType.STRING)
    private Protocol protocol;
    private String entity;
    @Column(name = "number_of_file_pairs")
    private int numberOfFilePairs;
    @Column(name = "patient_first_name")
    private String patientFirstName;
    @Column(name = "patient_last_name")
    private String patientLastName;
}
