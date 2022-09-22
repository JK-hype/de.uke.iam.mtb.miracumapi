CREATE DATABASE mriacum;
\c miracum;
CREATE SCHEMA IF NOT EXISTS miracum;

CREATE TABLE IF NOT EXISTS miracum.paths(
    patient_id varchar PRIMARY KEY,
    path_to_output varchar,
    path_to_maf varchar,
    path_to_report varchar,
    path_to_input varchar,
    path_to_logs varchar
);

CREATE TABLE IF NOT EXISTS miracum.input_details(
    patient_id varchar PRIMARY KEY,
    sex varchar,
    annotation_of_germline_findings boolean,
    protocol varchar,
    entity varchar,
    number_of_file_pairs varchar,
    patient_first_name varchar,
    patient_last_name varchar
);