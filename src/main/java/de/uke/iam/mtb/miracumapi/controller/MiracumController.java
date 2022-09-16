package de.uke.iam.mtb.miracumapi.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.hibernate.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import de.uke.iam.mtb.dto.miracum.MiracumInputDetailsDto;
import de.uke.iam.mtb.miracumapi.model.MiracumInputDetailsEntity;
import de.uke.iam.mtb.miracumapi.service.MiracumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
public class MiracumController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiracumController.class);

    @Autowired
    private final MiracumService miracumService;
    private final String miracumUrl = "/miracum";

    public MiracumController(MiracumService miracumService) {
        this.miracumService = miracumService;
    }

    @Operation(summary = "Run the pipline", description = "Run the pipeline for the patient with the id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Started MIRACUM pipeline for <patient name> (<id>)"),
            @ApiResponse(responseCode = "400", description = "Could not find details for patient with id: <id>") })
    @GetMapping(miracumUrl + "/run/{id}")
    public ResponseEntity<String> runMiracum(
            @Parameter(description = "Patient id. Cannot be null", required = true) @PathVariable String id) {
        Optional<MiracumInputDetailsDto> inputDetailsOpt = miracumService.getInputDetailsById(id);
        HttpStatus status;
        String message;
        if (inputDetailsOpt.isPresent()) {
            status = HttpStatus.OK;
            new Thread(() -> miracumService.runMiracum(inputDetailsOpt.get())).start();
            message = "Started MIRACUM pipeline for " + inputDetailsOpt.get().getPatientNameWithUnderscore() + " (" + id
                    + ")";
        } else {
            status = HttpStatus.BAD_REQUEST;
            message = "Could not find details for patient with id: " + id;
        }
        return new ResponseEntity<>(message, status);
    }

    @Operation(summary = "Saves", description = "Saves the yaml configuration file in the corresponding patient directory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved configuration yaml: <inputDetails>"),
            @ApiResponse(responseCode = "400", description = "Failed to save config yaml"),
            @ApiResponse(responseCode = "400", description = "Failed to map inputDetails to MiracumInputDetailsEntity") })
    @PostMapping(miracumUrl + "/yaml_conf")
    public ResponseEntity<String> saveYamlConf(
            @Parameter(description = "No field can be empty", required = true, schema = @Schema(implementation = MiracumInputDetailsDto.class)) @RequestBody MiracumInputDetailsDto inputDetails) {
        String message;
        HttpStatus status;
        try {
            miracumService.saveYamlConf(inputDetails);
            message = "Saved configuration yaml: " + inputDetails;
            status = HttpStatus.CREATED;
        } catch (IOException e) {
            LOGGER.error("Failed to save config yaml: " + inputDetails);
            e.printStackTrace();
            message = "Failed to save config yaml: " + inputDetails;
            status = HttpStatus.BAD_REQUEST;
        } catch (MappingException e) {
            LOGGER.error("Failed to map inputDetails: " + inputDetails + " to " + MiracumInputDetailsEntity.class);
            e.printStackTrace();
            message = "Failed to map inputDetails: " + inputDetails + " to " + MiracumInputDetailsEntity.class;
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(message, status);
    }

    @Operation(summary = "Get by id", description = "")
    @ApiResponse(responseCode = "200", description = "Found report for patient", content = {
            @Content(mediaType = "application/octet-stream", schema = @Schema(implementation = FileSystemResource.class)) })
    @GetMapping(value = miracumUrl + "/report/{id}", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE })
    @ResponseBody
    public FileSystemResource getReport(
            @Parameter(description = "Patient id. Cannot be null", required = true) @PathVariable String id) {
        return miracumService.getMiracumReport(id);
    }

    @Operation(summary = "Get by id", description = "")
    @ApiResponse(responseCode = "200", description = "Found maf for patient", content = {
            @Content(mediaType = "application/octet-stream", schema = @Schema(implementation = FileSystemResource.class)) })
    @GetMapping(value = miracumUrl + "/maf/{id}", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE })
    @ResponseBody
    public FileSystemResource getMaf(
            @Parameter(description = "Patient id. Cannot be null", required = true) @PathVariable String id) {
        return miracumService.getMafFile(id);
    }

    @Operation(summary = "Get by name", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returned fastQ directory of <patient name>"),
            @ApiResponse(responseCode = "400", description = "Could not create fastQ directory of <patient_name>") })
    @GetMapping(miracumUrl + "/fastq/directory/{firstName}/{lastName}")
    @ResponseBody
    public ResponseEntity<String> getFastQDirectory(
            @Parameter(description = "Patient's first name. Cannot be null or empty", required = true) @PathVariable String firstName,
            @Parameter(description = "Patient's last name. Cannot be null or empty", required = true) @PathVariable String lastName) {
        MiracumInputDetailsDto inputDetails = createInputDetails(firstName, lastName);
        String directory = miracumService.getFastQDirectory(inputDetails);
        HttpStatus status;
        if (!directory.isEmpty()) {
            LOGGER.info("Returned fastQ directory of " + inputDetails.getPatientNameWithUnderscore());
            status = HttpStatus.OK;
        } else {
            LOGGER.error("Could not create fastQ directory of " + inputDetails.getPatientNameWithUnderscore());
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(directory, status);
    }

    @Operation(summary = "Get by name", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returned fastQ name of <patient name>"),
            @ApiResponse(responseCode = "400", description = "Could not create fastQ directory of <patient_name>") })
    @GetMapping(miracumUrl + "/fastq/names/{number_of_file_pairs}/{firstName}/{lastName}")
    @ResponseBody
    public ResponseEntity<List<String>> getFastQNames(
            @Parameter(description = "Patient's first name. Cannot be null or empty", required = true) @PathVariable String firstName,
            @Parameter(description = "Patient's last name. Cannot be null or empty", required = true) @PathVariable String lastName,
            @Parameter(description = "Number of file pairs. Expects 1 for 1 pair (-> 2 files)", required = true) @PathVariable("number_of_file_pairs") int numberOfFilePairs) {
        MiracumInputDetailsDto inputDetails = createInputDetails(firstName, lastName);
        List<String> names = miracumService.getFastQNames(inputDetails, numberOfFilePairs);
        HttpStatus status;
        if (names.isEmpty() || names.stream().anyMatch(s -> s.isEmpty())) {
            LOGGER.error("Could not create fastQ names of " + inputDetails.getPatientNameWithUnderscore());
            status = HttpStatus.BAD_REQUEST;
        } else {
            LOGGER.info("Returned fastQ names of " + inputDetails.getPatientNameWithUnderscore());
            status = HttpStatus.OK;
        }
        return new ResponseEntity<>(names, status);
    }

    private MiracumInputDetailsDto createInputDetails(String firstName, String lastName) {
        MiracumInputDetailsDto miracumInputDetails = new MiracumInputDetailsDto();
        miracumInputDetails.setPatientFirstName(firstName);
        miracumInputDetails.setPatientLastName(lastName);
        return miracumInputDetails;
    }

    @Operation(summary = "Delete by id", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted patient <patient name> (<id>)"),
            @ApiResponse(responseCode = "400", description = "Could not find details for patient with id: <id>") })
    @DeleteMapping(miracumUrl + "/patient/{id}")
    public ResponseEntity<String> deletePatient(@PathVariable String id) {
        Optional<MiracumInputDetailsDto> inputDetailsOpt = miracumService.getInputDetailsById(id);
        HttpStatus status;
        String message;
        if (inputDetailsOpt.isPresent()) {
            status = HttpStatus.OK;
            miracumService.deletePatientDirectories(inputDetailsOpt.get());
            message = "Deleted patient " + inputDetailsOpt.get().getPatientNameWithUnderscore() + " (" + id + ")";
        } else {
            status = HttpStatus.BAD_REQUEST;
            message = "Could not find details for patient with id: " + id;
        }
        return new ResponseEntity<>(message, status);
    }
}