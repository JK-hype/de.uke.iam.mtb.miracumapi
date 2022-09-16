package de.uke.iam.mtb.miracumapi.service;

import static de.uke.iam.lib.restclient.RESTUtilHelper.post;
import static de.uke.iam.mtb.miracumapi.util.MiracumConverter.convertInputDetailsToJobDataMap;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.uke.iam.lib.json.GsonHelper;
import de.uke.iam.mtb.dto.miracum.MiracumInputDetailsDto;
import de.uke.iam.mtb.dto.miracum.MiracumMafDto;
import de.uke.iam.mtb.miracumapi.dao.MiracumInputDetailsRepository;
import de.uke.iam.mtb.miracumapi.dao.MiracumPathsRepository;
import de.uke.iam.mtb.miracumapi.deserializer.MiracumMafDtoDeserializer;
import de.uke.iam.mtb.miracumapi.model.MiracumInputDetailsEntity;
import de.uke.iam.mtb.miracumapi.model.MiracumPathsEntity;
import de.uke.iam.mtb.miracumapi.serializer.MiracumInputDetailsToYamlSerializer;
import de.uke.iam.mtb.miracumapi.util.MiracumPathBuilder;
import de.uke.iam.mtb.miracumapi.util.MiracumPathBuilder.OutputFile;
import de.uke.iam.mtb.miracumapi.util.MiracumQuartzJob;

@Service
public class MiracumService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiracumService.class);

    private final MiracumInputDetailsRepository inputDetailsRepository;
    private final MiracumPathsRepository pathRepository;
    private final MiracumPathBuilder pathBuilder;
    private final String pathToMiracum;
    private final String mapperUrl;
    private final int startTime;
    private final int endTime;
    private final int intervalTime;

    public MiracumService(MiracumInputDetailsRepository inputDetailsRepository, MiracumPathsRepository pathRepository,
            @Value("${pathToMIRACUM}") String pathToMiracum, @Value("${mapperUrl}") String mapperUrl,
            @Value("${quartz.startTime}") int startTime,
            @Value("${quartz.endTime}") int endTime, @Value("${quartz.intervalTime}") int intervalTime) {
        this.inputDetailsRepository = inputDetailsRepository;
        this.pathRepository = pathRepository;
        this.pathToMiracum = pathToMiracum;
        this.mapperUrl = mapperUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.intervalTime = intervalTime;
        this.pathBuilder = new MiracumPathBuilder(pathToMiracum);
    }

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    private void startScheduler() {
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.warn("Could not start scheduler");
            e.printStackTrace();
        }
    }

    public void runMiracum(MiracumInputDetailsDto inputDetails) {

        try {
            /*
             * ProcessBuilder takes the options and their values without a space between
             * them
             */
            ProcessBuilder processBuilder = new ProcessBuilder("./miracum_pipe.sh",
                    "-d" + inputDetails.getPatientNameWithUnderscore(), "-p" + inputDetails.getProtocol().toString(),
                    "-vbeta");
            processBuilder.directory(new File(pathToMiracum));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            scheduleJob(inputDetails);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scheduleJob(MiracumInputDetailsDto inputDetails) {
        String patientId = inputDetails.getPatientId();
        try {
            JobDetail job = newJob(MiracumQuartzJob.class).withIdentity(jobKey(patientId + "_Job"))
                    .usingJobData(convertInputDetailsToJobDataMap(inputDetails)).build();
            SimpleTrigger trigger = newTrigger().withIdentity(triggerKey(patientId + "_Trigger"))
                    .startAt(futureDate(startTime, IntervalUnit.SECOND))
                    .endAt(futureDate(endTime, IntervalUnit.HOUR))
                    .withSchedule(simpleSchedule().withIntervalInSeconds(intervalTime).repeatForever()).build();
            scheduler.scheduleJob(job, trigger);

            LOGGER.info("Started job to observe pipeline of " + patientId);
            LOGGER.info("Checks first time at " + LocalDateTime.now().plusHours(startTime) + " and then every "
                    + intervalTime + " minutes");
        } catch (SchedulerException e) {
            LOGGER.error("Did not start scheduler for " + patientId);
            e.printStackTrace();
        }
    }

    public void sendMafFile(MiracumInputDetailsDto inputDetails) {
        deleteJob(inputDetails.getPatientId());
        List<MiracumMafDto> mafDtos = readMafListFromFile(
                pathBuilder.buildFilePathToPatientOutputFile(inputDetails, OutputFile.MAF));
        // TODO error handling
        if (mafDtos == null) {
            return;
        }
        if (mafDtos.isEmpty()) {
            return;
        }
        mafDtos.stream().forEach((dto) -> {
            dto.setPatientId(inputDetails.getPatientId());
            dto.setProtocol(inputDetails.getProtocol());
        });
        String json = GsonHelper.get().getNewGson().toJson(mafDtos);
        try {
            post(new URL(mapperUrl), json);
            LOGGER.info("Sent " + json + " to " + mapperUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void deleteJob(String patientId) {
        try {
            JobKey jobKey = jobKey(patientId + "_Job");
            if (scheduler.checkExists(jobKey)) {
                boolean done = scheduler.unscheduleJob(triggerKey(patientId + "_Trigger"));
                done = done || scheduler.deleteJob(jobKey);
                if (done) {
                    LOGGER.info("Succesfully deleted job for " + patientId);
                }
            }
        } catch (SchedulerException e) {
            LOGGER.error("Did not delete job for " + patientId);
            e.printStackTrace();
        }
    }

    /*
     * Reads a file in the form of
     * Header1 Header2 Header3
     * Entry11 Entry12 Entry13
     * Entry21 Entry22 Entry23
     * 
     * to a list of maps of form
     * KEY VALUE
     * ----------------------
     * Header1 Entry11 | |
     * Header2 Entry12 |MAP |
     * Header3 Entry13 | |
     * ---------------------|LIST
     * Header1 Entry21 | |
     * Header2 Entry22 |MAP |
     * Header3 Entry23 | |
     */
    private List<MiracumMafDto> readMafListFromFile(String pathToMafFile) {
        List<MiracumMafDto> miracumMafDtoList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(pathToMafFile))) {
            ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
            List<String> headers = new ArrayList<>();
            String nextLine;
            boolean isHeader = true;

            while ((nextLine = reader.readLine()) != null) {
                List<String> entries = Arrays.asList(nextLine.split("\t"));
                if (isHeader) {
                    headers = entries;
                    isHeader = false;
                } else {
                    ArrayList<String> rowEntries = new ArrayList<>(entries);
                    // rows could contain less information than headers
                    rowEntries.addAll(getListWithEmptyStrings(headers.size() - entries.size()));
                    data.add(rowEntries);
                }
            }
            ObjectMapper objectMapper = getObjectMapper();
            for (List<String> rowEntries : data) {
                Map<String, String> mafFileMap = new LinkedHashMap<>();
                mafFileMap.clear();
                for (int i = 0; i < headers.size(); i++) {
                    mafFileMap.put(headers.get(i), rowEntries.get(i));
                }
                miracumMafDtoList.add(objectMapper.convertValue(mafFileMap, MiracumMafDto.class));
            }

        } catch (FileNotFoundException e) {
            LOGGER.info("There are no maf files in " + pathToMafFile);
            return Collections.<MiracumMafDto>emptyList();
        } catch (IOException e) {
            LOGGER.error("Could not read maf file in " + pathToMafFile);
            e.printStackTrace();
            return null;
        }
        return miracumMafDtoList;
    }

    private ObjectMapper getObjectMapper() {
        SimpleModule module = new SimpleModule().addDeserializer(MiracumMafDto.class, new MiracumMafDtoDeserializer());
        return new ObjectMapper().registerModule(module);
    }

    private ArrayList<String> getListWithEmptyStrings(int size) {
        ArrayList<String> listWithEmptyString = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            listWithEmptyString.add("");
        }
        return listWithEmptyString;
    }

    public void saveYamlConf(MiracumInputDetailsDto inputDetails)
            throws StreamWriteException, DatabindException, IOException {
        String pathToPatientInput = pathBuilder
                .buildFilePathToPatientInput(inputDetails.getPatientNameWithUnderscore());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String[] files = new File(pathToPatientInput).list();
        inputDetails.setNumberOfFilePairs(
                (int) (Arrays.asList(files).stream()
                        .filter(f -> f.contains(inputDetails.getPatientNameWithUnderscore())).count()));
        objectMapper.registerModule(
                new SimpleModule().addSerializer(MiracumInputDetailsDto.class,
                        new MiracumInputDetailsToYamlSerializer()));

        File patientYaml = new File(pathToPatientInput + "/patient.yaml");
        if (patientYaml.getParentFile().mkdirs()) {
            LOGGER.info("Created direcotry of " + inputDetails.getPatientNameWithUnderscore());
        }
        // saves the configruation yaml in
        // /"pathToMIRACUM"/assets/input/"patientFirstname"_"patientLastName"
        objectMapper.writeValue(patientYaml, inputDetails);
        LOGGER.info("patient.yaml of " + inputDetails.getPatientNameWithUnderscore() + " in " + pathToPatientInput
                + " was created.");

        savePathsInDatabase(inputDetails);
        saveYamlInDatabase(inputDetails);
    }

    private void saveYamlInDatabase(MiracumInputDetailsDto inputDetails) {
        ModelMapper mapper = new ModelMapper();
        inputDetailsRepository.save(mapper.map(inputDetails, MiracumInputDetailsEntity.class));
    }

    private void savePathsInDatabase(MiracumInputDetailsDto inputDetails) {
        MiracumPathsEntity pathEntity = new MiracumPathsEntity();
        pathEntity.setPatientId(inputDetails.getPatientId());
        pathEntity.setPathToInput(pathBuilder.buildFilePathToPatientInput(inputDetails.getPatientNameWithUnderscore()));
        pathEntity.setPathToLogs(pathBuilder.buildFilePathToLogs(inputDetails));
        pathEntity.setPathToMaf(pathBuilder.buildFilePathToPatientOutputFile(inputDetails, OutputFile.MAF));
        pathEntity.setPathToReport(pathBuilder.buildFilePathToPatientOutputFile(inputDetails, OutputFile.REPORT));
        pathEntity.setPathToOutput(pathBuilder.buildFilePathToPatientOutput(inputDetails));
        pathRepository.save(pathEntity);
    }

    public FileSystemResource getMiracumReport(String patientId) {
        Optional<MiracumPathsEntity> pathOpt = getPathById(patientId);
        String pathToReport;
        if (pathOpt.isPresent()) {
            LOGGER.info("Found " + pathOpt);
            pathToReport = pathOpt.get().getPathToReport();
        } else {
            LOGGER.warn("Did not found any paths for patient with id: " + patientId);
            return null;
        }
        return new FileSystemResource(pathToReport);
    }

    public FileSystemResource getMafFile(String patientId) {
        Optional<MiracumPathsEntity> pathOpt = getPathById(patientId);
        String pathToMaf;
        if (pathOpt.isPresent()) {
            LOGGER.info("Found " + pathOpt);
            pathToMaf = pathOpt.get().getPathToMaf();
        } else {
            LOGGER.warn("Did not found any path for patient with id: " + patientId);
            return null;
        }
        return new FileSystemResource(pathToMaf);
    }

    public String getFastQDirectory(MiracumInputDetailsDto inputDetails) {
        return pathBuilder.buildFilePathToPatientInput(inputDetails.getPatientNameWithUnderscore());
    }

    public List<String> getFastQNames(MiracumInputDetailsDto inputDetails, int numberOfFiles) {
        ArrayList<String> names = new ArrayList<>();
        String patientName = inputDetails.getPatientNameWithUnderscore();
        for (int i = 1; i <= numberOfFiles; i++) {
            names.add(patientName + "_" + i + "_R1_001.fastq.gz");
            names.add(patientName + "_" + i + "_R2_001.fastq.gz");
        }
        return names;
    }

    public void deletePatientDirectories(MiracumInputDetailsDto inputDetails) {
        String pathToInputPatient = pathBuilder
                .buildFilePathToPatientInput(inputDetails.getPatientNameWithUnderscore());
        String pathToPatientOutput = pathBuilder.buildFilePathToPatientOutput(inputDetails);

        try {
            FileUtils.deleteDirectory(new File(pathToInputPatient));
        } catch (IOException e) {
            LOGGER.error("Failed to delete input folder of " + inputDetails.getPatientNameWithUnderscore() + " ("
                    + inputDetails.getPatientId() + ")");
            e.printStackTrace();
        }
        try {
            FileUtils.deleteDirectory(new File(pathToPatientOutput));
        } catch (IOException e) {
            LOGGER.error("Failed to delete output folder of " + inputDetails.getPatientNameWithUnderscore() + " ("
                    + inputDetails.getPatientId() + ")");
            e.printStackTrace();
        }
        LOGGER.info("Succesfully deleted " + inputDetails.getPatientNameWithUnderscore() + " ("
                + inputDetails.getPatientId() + ")");
    }

    public Optional<MiracumInputDetailsDto> getInputDetailsById(String patientId) {
        ModelMapper mapper = new ModelMapper();
        Optional<MiracumInputDetailsEntity> inputDetailsEntityOpt = inputDetailsRepository.findById(patientId);
        if (inputDetailsEntityOpt.isPresent()) {
            MiracumInputDetailsEntity inputDetailsEntity = inputDetailsEntityOpt.get();
            LOGGER.info("Found " + inputDetailsEntity);
            return Optional.of(mapper.map(inputDetailsEntity, MiracumInputDetailsDto.class));
        } else {
            LOGGER.warn("Did not found any details for patient with id: " + patientId);
            return Optional.empty();
        }
    }

    private Optional<MiracumPathsEntity> getPathById(String patientId) {
        return pathRepository.findById(patientId);
    }
}
